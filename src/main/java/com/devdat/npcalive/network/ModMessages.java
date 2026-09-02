package com.devdat.npcalive.network;

import com.devdat.npcalive.NpcAlive;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = NpcAlive.MOD_ID)
public class ModMessages {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        // Paquete existente (Servidor a Cliente)
        registrar.playToClient(
                OpenNpcGuiPacket.TYPE,
                OpenNpcGuiPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPayloadHandler.handleOpenNpcGui(payload);
                })
        );

        // Nuevo paquete (Cliente a Servidor)
        registrar.playToServer(
                PerformNpcActionPacket.TYPE,
                PerformNpcActionPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ServerPayloadHandler.handlePerformNpcAction(payload, context);
                })
        );
    }
}