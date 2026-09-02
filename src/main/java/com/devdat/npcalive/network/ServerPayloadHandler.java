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
                    case "PROPOSE" -> handlePropose(serverPlayer, npc);
                    case "INVENTORY" -> serverPlayer.sendSystemMessage(Component.literal("Abriendo inventario de " + npc.getNpcTitle() + "... (En desarrollo)"));
                    case "ROMANCE" -> handleRomance(serverPlayer, npc);
                    case "MEAN" -> handleMean(serverPlayer, npc);
                    case "GIFT" -> handleGift(serverPlayer, npc);
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

    private static void handleGift(ServerPlayer player, NpcEntity npc) {
        net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            player.sendSystemMessage(Component.literal("¡No tienes nada en la mano para regalar!"));
            return;
        }

        if (heldItem.is(net.minecraft.world.item.Items.POPPY) || heldItem.is(net.minecraft.world.item.Items.DANDELION)) {
            heldItem.shrink(1);
            npc.addFriendship(15);
            player.sendSystemMessage(Component.literal("¡A " + npc.getNpcTitle() + " le encantó la flor! +15 Amistad"));
            playFeedbackParticles(npc, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER);
        } else if (heldItem.is(net.minecraft.world.item.Items.DIAMOND)) {
            heldItem.shrink(1);
            npc.addRomance(25);
            npc.addFriendship(10);
            player.sendSystemMessage(Component.literal("¡" + npc.getNpcTitle() + " está deslumbrado por el diamante! +25 Romance"));
            playFeedbackParticles(npc, net.minecraft.core.particles.ParticleTypes.HEART);
        } else {
            player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " mira el objeto con indiferencia y no le interesa."));
        }
    }

    private static void handlePropose(ServerPlayer serverPlayer, NpcEntity npc) {
        if (npc.isMarried()) {
            serverPlayer.sendSystemMessage(Component.literal("¡Ya estás casado con " + npc.getNpcTitle() + "!"));
            return;
        }

        if (npc.getRomance() < 200) {
            serverPlayer.sendSystemMessage(Component.literal(npc.getNpcTitle() + " piensa que aún es muy pronto para dar este paso. (Requiere 200 de Romance)"));
            return;
        }

        net.minecraft.world.item.ItemStack heldItem = serverPlayer.getMainHandItem();
        if (heldItem.is(net.minecraft.world.item.Items.DIAMOND)) {
            heldItem.shrink(1);
            npc.setMarried(true);
            serverPlayer.sendSystemMessage(Component.literal("¡" + npc.getNpcTitle() + " ha aceptado tu propuesta! Ahora están casados."));

            if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, npc.getX(), npc.getY() + 1.0D, npc.getZ(), 10, 0.5, 0.5, 0.5, 0.2);
            }
        } else {
            serverPlayer.sendSystemMessage(Component.literal("Necesitas sostener un Diamante en la mano para proponer matrimonio."));
        }
    }

    private static void playFeedbackParticles(NpcEntity npc, net.minecraft.core.particles.ParticleOptions particle) {
        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, npc.getX(), npc.getY() + 1.0D, npc.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private static void handleTransactions(ServerPlayer player, NpcEntity npc) {
        player.sendSystemMessage(Component.literal(npc.getNpcTitle() + " evalúa negociar contigo. (Amistad: " + npc.getFriendship() + ")"));
    }
}