package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import com.devdat.npcalive.entity.profession.ProfessionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class WorkAtStationGoal extends Goal {
    private final NpcEntity npc;
    private final double speedModifier;
    private int workTimer = 0;
    private int noPathTicks = 0;

    public WorkAtStationGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private boolean isWorkTime(long timeOfDay, com.devdat.npcalive.entity.profession.ProfessionLogic logic) {
        boolean firstShift = timeOfDay >= logic.getWorkStartTime() && timeOfDay < logic.getWorkEndTime();
        boolean secondShift = logic.getSecondWorkStartTime() != -1 && timeOfDay >= logic.getSecondWorkStartTime() && timeOfDay < logic.getSecondWorkEndTime();
        return firstShift || secondShift;
    }

    @Override
    public boolean canUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        if (npc.getValidWorkPos() == null) {
            npc.searchAndAssignWorkstation();
        }

        if (npc.getValidWorkPos() == null) {
            return false;
        }

        if (npc.level() instanceof ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            var professionLogic = ProfessionRegistry.getLogic(npc.getProfession());
            return isWorkTime(timeOfDay, professionLogic);
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        BlockPos workPos = npc.getValidWorkPos();
        if (workPos == null) return false;

        if (npc.level() instanceof ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            var professionLogic = ProfessionRegistry.getLogic(npc.getProfession());
            if (!isWorkTime(timeOfDay, professionLogic)) {
                return false;
            }
        } else {
            return false;
        }

        BlockPos target = ProfessionRegistry.getLogic(npc.getProfession()).getTargetPosition(npc, workPos);
        if (npc.blockPosition().distSqr(target) <= 4.0D) {
            return true;
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
    public void start() {
        noPathTicks = 0;
        moveToTarget();
    }

    private void moveToTarget() {
        BlockPos workPos = npc.getValidWorkPos();
        if (workPos == null) return;

        BlockPos target = ProfessionRegistry.getLogic(npc.getProfession()).getTargetPosition(npc, workPos);
        npc.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speedModifier);
    }

    @Override
    public void tick() {
        super.tick();

        BlockPos workPos = npc.getValidWorkPos();
        if (workPos == null) return;

        BlockPos target = ProfessionRegistry.getLogic(npc.getProfession()).getTargetPosition(npc, workPos);
        double distSqr = npc.blockPosition().distSqr(target);

        if (distSqr > 4.0D) {
            if (npc.getNavigation().isDone() && noPathTicks == 0) {
                moveToTarget();
            }
            return;
        }

        noPathTicks = 0;
        npc.getNavigation().stop();

        npc.equipWorkTool(ProfessionRegistry.getLogic(npc.getProfession()).getWorkTool());

        npc.getLookControl().setLookAt(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D,
                30.0F, 30.0F
        );

        workTimer++;
        ProfessionRegistry.getLogic(npc.getProfession()).tickWork(npc, target, workPos, workTimer);

        if (workTimer >= 40) {
            workTimer = 0;
            ProfessionRegistry.getLogic(npc.getProfession()).performWork(npc, target, workPos);
        }
    }

    @Override
    public void stop() {
        npc.restoreOriginalHand();

        if (npc.level() instanceof ServerLevel serverLevel && npc.getValidWorkPos() != null) {
            BlockPos workPos = npc.getValidWorkPos();
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            var professionLogic = ProfessionRegistry.getLogic(npc.getProfession());

            // Si el turno terminó, ordenamos al NPC volver directo a su casa/mesa de trabajo
            if (!isWorkTime(timeOfDay, professionLogic)) {
                npc.getNavigation().moveTo(workPos.getX() + 0.5D, workPos.getY(), workPos.getZ() + 0.5D, 1.2D);
            }

            BlockPos target = professionLogic.getTargetPosition(npc, workPos);
            serverLevel.destroyBlockProgress(npc.getId(), target, -1);
        }

        workTimer = 0;
        noPathTicks = 0;
    }
}