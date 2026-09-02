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
                switch (payload.actionName()) {
                    case "FOLLOW" -> handleToggleFollow(serverPlayer, npc);
                    case "GREET" -> handleGreet(serverPlayer, npc);
                    case "ROMANCE" -> handleRomance(serverPlayer, npc);
                    case "MEAN" -> handleMean(serverPlayer, npc);
                    case "TRANSACTIONS" -> handleTransactions(serverPlayer, npc);
                    default -> serverPlayer.sendSystemMessage(Component.literal("Acción desconocida: " + payload.actionName()));
                }
            }
        }
    }

    private static void handleToggleFollow(ServerPlayer player, NpcEntity npc) {
        if (npc.getFriendship() < 0) {
            player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " te ignora por completo."));
            return;
        }

        NpcEntity.NpcBehavior nextBehavior = npc.getBehavior().next();
        npc.setBehavior(nextBehavior);

        switch (nextBehavior) {
            case WANDER -> player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " ahora camina libremente."));
            case FOLLOW -> player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " ahora te está siguiendo."));
            case STAY -> player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " se quedará esperando aquí."));
        }
    }


    private static void handleGreet(ServerPlayer player, NpcEntity npc) {
        npc.addFriendship(5); // Suma 5 puntos
        player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " te saluda amablemente. (Amistad: " + npc.getFriendship() + ")"));
    }

    private static void handleRomance(ServerPlayer player, NpcEntity npc) {
        if (npc.getFriendship() < 10) {
            player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " aún no te conoce lo suficiente como para aceptar coqueteos. ¡Sube tu nivel de amistad!"));
            return;
        }

        npc.addRomance(5);
        player.sendSystemMessage(Component.literal("¡Has coqueteado con " + npc.getNpcTitle() + "! Afecto actual: " + npc.getRomance()));

        // Opcional: Podríamos hacer que spawnee partículas de corazones alrededor del NPC
        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    npc.getX(), npc.getY() + 1.0D, npc.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private static void handleMean(ServerPlayer player, NpcEntity npc) {
        // Restamos puntos de amistad y romance (puedes ajustar los valores a tu gusto)
        npc.addFriendship(-5);
        npc.addRomance(-10);

        player.sendSystemMessage(Component.literal("Has ofendido a " + npc.getNpcTitle() + ". ¡Su expresión se ensombrece!"));

        // Opcional: Partículas de enojo de aldeano para que la reacción sea visible
        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                    npc.getX(), npc.getY() + 1.0D, npc.getZ(), 3, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private static void handleTransactions(ServerPlayer player, NpcEntity npc) {
        player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " evalúa negociar contigo. (Amistad: " + npc.getFriendship() + ")"));
    }
}