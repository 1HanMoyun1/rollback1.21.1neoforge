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
        if (isClientDist()) {
            com.taobao.koi.rollbackmod.client.ClientDayHud.register(modEventBus);

            // Cloth Config 设置界面（可选依赖，仅客户端；必须放在 dist 判断内，
            // 否则 IConfigScreenFactory 的 Screen 签名会在专用服务器上被解析并触发
            // RuntimeDistCleaner 报错：Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER）。
            // 未安装 cloth_config 时返回父界面，避免加载 ClothConfigScreen 触发 NoClassDefFoundError。
            modContainer.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (IConfigScreenFactory) (minecraft, parent) ->
                            net.neoforged.fml.ModList.get().isLoaded("cloth_config")
                                    ? ClothConfigScreen.create(parent)
                                    : parent
            );
        }
    }

    /**
     * 是否客户端：用反射读 loader 的 dist，兼容两代 API：
     * 1.21–1.21.8 为 {@code FMLEnvironment.dist}（静态字段），
     * 1.21.9+ 为 {@code FMLEnvironment.getDist()}（静态方法）。
     * 只反射 loader 类，不加载任何客户端类，不会触发 RuntimeDistCleaner 报错。
     */
    private static boolean isClientDist() {
        try {
            Class<?> env = Class.forName("net.neoforged.fml.loading.FMLEnvironment");
            Object dist;
            try {
                dist = env.getField("dist").get(null);
            } catch (NoSuchFieldException e) {
                dist = env.getMethod("getDist").invoke(null);
            }
            return dist != null && "CLIENT".equals(dist.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }
}
