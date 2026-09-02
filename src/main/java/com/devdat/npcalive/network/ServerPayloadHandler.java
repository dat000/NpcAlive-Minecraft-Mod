package com.devdat.npcalive.network;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handlePerformNpcAction(PerformNpcActionPacket payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            Entity entity = serverPlayer.level().getEntity(payload.entityId());

            if (entity instanceof NpcEntity npc) {
                // Procesar la acción según el tipo recibido
                switch (payload.actionName()) {
                    case "GREET" -> handleGreet(serverPlayer, npc);
                    case "ROMANCE" -> handleRomance(serverPlayer, npc);
                    case "MEAN" -> handleMean(serverPlayer, npc);
                    case "TRANSACTIONS" -> handleTransactions(serverPlayer, npc);
                    default -> serverPlayer.sendSystemMessage(Component.literal("Acción desconocida: " + payload.actionName()));
                }
            }
        }
    }

    private static void handleGreet(ServerPlayer player, NpcEntity npc) {
        player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " te saluda amablemente. ¡Hola!"));
        // Aquí puedes sumar puntos de amistad en el futuro
    }

    private static void handleRomance(ServerPlayer player, NpcEntity npc) {
        player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " se sonroja levemente ante tu atención."));
    }

    private static void handleMean(ServerPlayer player, NpcEntity npc) {
        player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " se ve ofendido por tu actitud."));
    }

    private static void handleTransactions(ServerPlayer player, NpcEntity npc) {
        player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " abre su inventario o tienda."));
    }
}