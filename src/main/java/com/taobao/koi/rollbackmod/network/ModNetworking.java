package com.taobao.koi.rollbackmod.network;

import com.taobao.koi.rollbackmod.RollbackMod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(RollbackMod.MOD_ID);
        registrar.playToClient(DayInfoPacket.TYPE, DayInfoPacket.STREAM_CODEC, DayInfoPacket::handle);
    }
}
