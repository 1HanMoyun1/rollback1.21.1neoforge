package com.taobao.koi.rollbackmod.network;

import com.taobao.koi.rollbackmod.RollbackMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DayInfoPacket(
        boolean showHud,
        boolean countdownMode,
        int day,
        int remainingDays,
        boolean playAnimation,
        int fromNumber,
        int toNumber
) implements CustomPacketPayload {
    public static final Type<DayInfoPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RollbackMod.MOD_ID, "day_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DayInfoPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeBoolean(packet.showHud());
                buffer.writeBoolean(packet.countdownMode());
                buffer.writeVarInt(packet.day());
                buffer.writeVarInt(packet.remainingDays());
                buffer.writeBoolean(packet.playAnimation());
                if (packet.playAnimation()) {
                    buffer.writeVarInt(packet.fromNumber());
                    buffer.writeVarInt(packet.toNumber());
                }
            },
            buffer -> {
                boolean showHud = buffer.readBoolean();
                boolean countdownMode = buffer.readBoolean();
                int day = buffer.readVarInt();
                int remainingDays = buffer.readVarInt();
                boolean playAnimation = buffer.readBoolean();
                int fromNumber = -1;
                int toNumber = -1;
                if (playAnimation) {
                    fromNumber = buffer.readVarInt();
                    toNumber = buffer.readVarInt();
                }
                return new DayInfoPacket(showHud, countdownMode, day, remainingDays, playAnimation, fromNumber, toNumber);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DayInfoPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.taobao.koi.rollbackmod.client.ClientDayHud.update(packet));
    }
}
