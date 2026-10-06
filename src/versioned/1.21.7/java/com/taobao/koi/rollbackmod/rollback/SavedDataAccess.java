package com.taobao.koi.rollbackmod.rollback;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class SavedDataAccess {
    private SavedDataAccess() {
    }

    private static SavedDataType<RollbackSavedData> type() {
        return new SavedDataType<>(
                RollbackSavedData.DATA_NAME,
                context -> new RollbackSavedData(),
                context -> CompoundTag.CODEC.xmap(
                        tag -> RollbackSavedData.readTag(tag, context.levelOrThrow().registryAccess()),
                        RollbackSavedData::writeTag),
                DataFixTypes.LEVEL);
    }

    public static RollbackSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(type());
    }
}