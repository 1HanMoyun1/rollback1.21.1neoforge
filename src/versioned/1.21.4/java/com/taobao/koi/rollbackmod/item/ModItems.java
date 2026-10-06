package com.taobao.koi.rollbackmod.item;

import com.taobao.koi.rollbackmod.RollbackMod;
import com.taobao.koi.rollbackmod.core.CoreType;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RollbackMod.MOD_ID);

    /** 吸入器：装入 1 枚药芯，长按右键（喝水动作）触发。**无限耐久**（无最大耐久，永不损坏）。 */
    public static final DeferredHolder<Item, Item> INHALER = ITEMS.register("inhaler",
            key -> new InhalerItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, key)).stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<Item, Item> COCOON_CORE = registerCore(CoreType.COCOON);
    public static final DeferredHolder<Item, Item> MOLTING_CORE = registerCore(CoreType.MOLTING);
    public static final DeferredHolder<Item, Item> TOWER_CORE = registerCore(CoreType.TOWER);

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static Optional<CoreType> getCoreType(ItemStack stack) {
        if (stack.getItem() instanceof CoreItem coreItem) {
            return Optional.of(coreItem.getCoreType());
        }
        return Optional.empty();
    }

    public static boolean isCore(ItemStack stack) {
        return getCoreType(stack).isPresent();
    }

    private static DeferredHolder<Item, Item> registerCore(CoreType type) {
        return ITEMS.register(type.registryName(),
                key -> new CoreItem(type, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, key)).stacksTo(1).rarity(Rarity.RARE)));
    }
}
