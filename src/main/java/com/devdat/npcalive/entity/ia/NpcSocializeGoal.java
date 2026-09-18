package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;

public class NpcSocializeGoal extends Goal {
    private final NpcEntity npc;
    private NpcEntity targetNpc;
    private int interactTimer;
    private int cooldown;

    public NpcSocializeGoal(NpcEntity npc) {
        this.npc = npc;
        // Obliga al NPC a usar sus piernas y su cabeza para esta acción
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    // Metodo para enviar mensajes de depuración directamente al chat del juego a jugadores cercanos
    private void debugChat(String message) {
        if (this.npc.level() instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
            Component component = Component.literal("§e[DEBUG-SOCIAL] §f" + message);
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(
                    ServerPlayer.class,
                    this.npc.getBoundingBox().inflate(16.0D)
            )) {
                player.sendSystemMessage(component);
            }
        }
    }

    @Override
    public boolean canUse() {
        // 1. Manejo del cooldown (para que no estén saludándose cada segundo)
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }

        // 2. Probabilidad aleatoria de querer socializar (ej: 1 entre 40 ticks)
        if (this.npc.getRandom().nextInt(40) != 0) {
            return false;
        }

        // 3. Buscar otros NPCs en un radio de 10 bloques
        List<NpcEntity> nearbyNpcs = this.npc.level().getEntitiesOfClass(
                NpcEntity.class,
                this.npc.getBoundingBox().inflate(10.0D),
                e -> e != this.npc && e.isAlive()
        );

        if (nearbyNpcs.isEmpty()) {
            return false;
        }

        // 4. Elegir un NPC al azar para interactuar
        this.targetNpc = nearbyNpcs.get(this.npc.getRandom().nextInt(nearbyNpcs.size()));
        debugChat("¡Objetivo seleccionado! Socializando con NPC ID: " + this.targetNpc.getId());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        boolean canContinue = this.interactTimer > 0 && this.targetNpc != null && this.targetNpc.isAlive() && this.npc.distanceToSqr(this.targetNpc) < 256.0D;
        if (!canContinue && this.targetNpc != null) {
            debugChat("canContinueToUse cortado. Timer: " + this.interactTimer + ", DistanciaSq: " + this.npc.distanceToSqr(this.targetNpc));
        }
        return canContinue;
    }

    @Override
    public void start() {
        this.interactTimer = 80; // 80 ticks = 4 segundos de interacción
        debugChat("START: Iniciando Goal de socialización.");
    }

    @Override
    public void stop() {
        debugChat("STOP: Deteniendo socialización. Cooldown asignado.");
        this.targetNpc = null;
        this.npc.getNavigation().stop();
        this.cooldown = 200 + this.npc.getRandom().nextInt(200); // Cooldown entre 10 y 20 segundos
    }

    @Override
    public void tick() {
        if (this.targetNpc == null) return;

        // Mirar siempre al objetivo
        this.npc.getLookControl().setLookAt(this.targetNpc, 30.0F, 30.0F);

        double distance = this.npc.distanceToSqr(this.targetNpc);

        // Si están a más de 2 bloques (4.0D al cuadrado), caminar hacia él
        if (distance > 4.0D) {
            this.npc.getNavigation().moveTo(this.targetNpc, 0.6D); // 0.6D = Caminar tranquilo
        } else {
            // Si ya están cerca, se detienen a "charlar"
            if (this.interactTimer == 80) {
                debugChat("¡Llegó al objetivo! Deteniéndose a charlar.");
            }
            this.npc.getNavigation().stop();
            this.interactTimer--;

            // Cuando la charla termina (último tick)
            if (this.interactTimer == 1) {
                debugChat("¡Charla finalizada! Generando partículas.");
                // Generar partículas felices si estamos en el servidor
                if (!this.npc.level().isClientSide() && this.npc.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            this.npc.getX(), this.npc.getY() + this.npc.getEyeHeight(), this.npc.getZ(),
                            3, 0.3, 0.3, 0.3, 0.0);
                }
            }
        }
    }
}