package com.taobao.koi.rollbackmod.event;

import com.taobao.koi.rollbackmod.config.RollbackConfig;
import com.taobao.koi.rollbackmod.rollback.BlockRollbackManager;
import com.taobao.koi.rollbackmod.rollback.ChunkTracker;
import com.taobao.koi.rollbackmod.rollback.DayCounterManager;
import com.taobao.koi.rollbackmod.rollback.HiveManager;
import com.taobao.koi.rollbackmod.rollback.RollbackManager;
import com.taobao.koi.rollbackmod.rollback.RollbackRestoreQueue;
import com.taobao.koi.rollbackmod.rollback.SelfDestructManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CommonEvents {
    public static final String SKIP_NEXT_DEATH_ROLLBACK_TAG = "rollbackmod_skip_next_death_rollback";

    private CommonEvents() {
    }

    /** 世界创建/首次加载时只存档一次，之后不再自动存档，存档全靠药芯。 */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RollbackManager.ensureInitialCheckpoint(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 世界创建时的存档点里没有玩家：玩家加入时补拍快照进存档点，
            // 保证死亡回溯能把玩家送回加入时的状态（而不是原地不动）
            RollbackManager.addPlayerToCheckpoint(player.getServer(), player);
            SelfDestructManager.enforce(player);
            DayCounterManager.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        // 1.21.1 伤害事件不可取消：以“伤害被清零”作为已处理的标志
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        // 多人共享伤害：任一玩家受伤时伤害重定向给全体玩家
        if (HiveManager.shareDamage(event)) {
            return;
        }
        handleFatalRollback(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || RollbackManager.isRollingBack()) {
            return;
        }
        if (player.getPersistentData().getBoolean(SKIP_NEXT_DEATH_ROLLBACK_TAG)) {
            player.getPersistentData().remove(SKIP_NEXT_DEATH_ROLLBACK_TAG);
            return;
        }
        if (!RollbackConfig.enableDeathRollback) {
            return;
        }
        if (!RollbackManager.hasCheckpoint(player.getServer())) {
            return;
        }

        event.setCanceled(true);
        player.setHealth(Math.max(1.0F, player.getHealth()));
        RollbackManager.rollback(player.getServer(), "player_death");
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SelfDestructManager.enforce(player);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            // 两格方块（床/门/高花高草）被破坏时只有一格触发事件，另一半会被连带移除；一起记下以便完整回溯
            BlockRollbackManager.rememberBlockAndConnected(level, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            BlockRollbackManager.rememberBlockBeforeChange(level, event.getPos(), event.getBlockSnapshot().getCurrentState());
        }
    }

    @SubscribeEvent
    public static void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        event.getReplacedBlockSnapshots().forEach(snapshot ->
                BlockRollbackManager.rememberBlockBeforeChange(level, snapshot.getPos(), snapshot.getCurrentState()));
    }

    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            BlockRollbackManager.rememberBlockBeforeChange(level, event.getPos(), event.getOriginalState());
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            BlockRollbackManager.rememberBlockBeforeChange(level, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        if (!event.isCanceled() && event.getEntity().level() instanceof ServerLevel level) {
            BlockRollbackManager.rememberBlockBeforeChange(level, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (BlockPos pos : event.getAffectedBlocks()) {
                BlockRollbackManager.rememberBlockBeforeChange(level, pos);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChunkTracker.onChunkLoad(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChunkTracker.onChunkUnload(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ChunkTracker.clearAll();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        DayCounterManager.tick(event.getServer());
        HiveManager.tick(event.getServer());
        RollbackRestoreQueue.tick();
        RollbackManager.clearRollbackJustHappened();
        event.getServer().getPlayerList().getPlayers().forEach(SelfDestructManager::enforce);
    }

    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SelfDestructManager.isLocked(player)) {
            if (event.getNewGameMode() == GameType.SPECTATOR) {
                return;
            }
            event.setCanceled(true);
            event.setNewGameMode(GameType.SPECTATOR);
            SelfDestructManager.enforce(player);
        }
    }

    private static boolean handleFatalRollback(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || RollbackManager.isRollingBack()) {
            return false;
        }
        if (player.getPersistentData().getBoolean(SKIP_NEXT_DEATH_ROLLBACK_TAG)) {
            return false;
        }
        if (!RollbackConfig.enableDeathRollback) {
            return false;
        }
        if (!RollbackManager.hasCheckpoint(player.getServer())) {
            return false;
        }
        if (event.getNewDamage() < player.getHealth() + player.getAbsorptionAmount()) {
            return false;
        }

        event.setNewDamage(0.0F);
        player.setHealth(Math.max(1.0F, player.getHealth()));
        RollbackManager.rollback(player.getServer(), "fatal_damage");
        return true;
    }
}
