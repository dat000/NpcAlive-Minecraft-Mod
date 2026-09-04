package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class ReturnHomeGoal extends Goal {
    private final NpcEntity npc;
    private final double speedModifier;
    private int noPathTicks = 0;

    public ReturnHomeGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        BlockPos home = npc.getHomePos();
        if (home == null) return false;

        boolean tooFar = npc.blockPosition().distSqr(home) > 100.0D;

        boolean isNight = false;
        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            isNight = timeOfDay >= 13000 && timeOfDay < 23000;
        }

        // Reducimos el umbral a 2.0D para que no se detenga antes de entrar por la puerta
        return (tooFar || isNight) && npc.blockPosition().distSqr(home) > 2.0D;
    }

    @Override
    public void start() {
        BlockPos home = npc.getHomePos();
        if (home != null) {
            noPathTicks = 0;
            npc.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, speedModifier);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        BlockPos home = npc.getHomePos();
        if (home == null) return false;

        // Solo se detiene cuando está verdaderamente pegado al punto de hogar (radio menor a 1.5 bloques)
        if (npc.blockPosition().distSqr(home) <= 1.5D) {
            return false;
        }

        if (npc.getNavigation().isDone()) {
            noPathTicks++;
            return noPathTicks < 60;
        } else {
            noPathTicks = 0;
        }

        return true;
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
        noPathTicks = 0;
    }
}