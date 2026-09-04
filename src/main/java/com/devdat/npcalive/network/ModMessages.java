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

        // 1. Paquete existente (Servidor a Cliente)
        registrar.playToClient(
                OpenNpcGuiPacket.TYPE,
                OpenNpcGuiPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientPayloadHandler.handleOpenNpcGui(payload);
                })
        );

        // 2. Paquete existente (Cliente a Servidor - Acciones del menú y botones)
        registrar.playToServer(
                PerformNpcActionPacket.TYPE,
                PerformNpcActionPacket.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ServerPayloadHandler.handlePerformNpcAction(payload, context);
                })
        );

        // 3. Paquete de asignación de puesto
        registrar.playToServer(
                AssignWorkstationPacket.TYPE,
                AssignWorkstationPacket.STREAM_CODEC,
                AssignWorkstationPacket::handle
        );
    }
}