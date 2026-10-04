package com.taobao.koi.rollbackmod;

import com.taobao.koi.rollbackmod.config.ClothConfigScreen;
import com.taobao.koi.rollbackmod.config.RollbackConfig;
import com.taobao.koi.rollbackmod.core.CoreEffectRegistry;
import com.taobao.koi.rollbackmod.event.CommonEvents;
import com.taobao.koi.rollbackmod.item.ModCreativeTabs;
import com.taobao.koi.rollbackmod.item.ModItems;
import com.taobao.koi.rollbackmod.network.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RollbackMod.MOD_ID)
public class RollbackMod {
    public static final String MOD_ID = "rollbackmod";

    public RollbackMod(IEventBus modEventBus, ModContainer modContainer) {
        RollbackConfig.load(FMLPaths.CONFIGDIR.get().resolve("rollbackmod.json"));

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(ModNetworking::register);

        CoreEffectRegistry.bootstrap();
        NeoForge.EVENT_BUS.register(CommonEvents.class);
        if (FMLLoader.getDist().isClient()) {
            com.taobao.koi.rollbackmod.client.ClientDayHud.register(modEventBus);

            // Cloth Config 设置界面（可选依赖，仅客户端；必须放在 dist 判断内，
            // 否则 IConfigScreenFactory 的 Screen 签名会在专用服务器上被解析并触发
            // RuntimeDistCleaner 报错：Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER）。
            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (IConfigScreenFactory) (minecraft, parent) -> ClothConfigScreen.create(parent)
            );
        }
    }
}
