package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import com.devdat.npcalive.entity.profession.ProfessionLogic;
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
    private int searchCooldown = 0;

    public WorkAtStationGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private boolean isWorkTime(long timeOfDay, ProfessionLogic logic) {
        boolean firstShift = timeOfDay >= logic.getWorkStartTime() && timeOfDay < logic.getWorkEndTime();
        boolean secondShift = logic.getSecondWorkStartTime() != -1 && timeOfDay >= logic.getSecondWorkStartTime() && timeOfDay < logic.getSecondWorkEndTime();
        return firstShift || secondShift;
    }

    // Metodo centralizado para asegurar que NUNCA caminen encima del bloque de trabajo
    private BlockPos getEffectiveTarget(BlockPos workPos) {
        ProfessionLogic logic = ProfessionRegistry.getLogic(npc.getProfession());
        BlockPos rawTarget = logic.getTargetPosition(npc, workPos);
        // Si la lógica devuelve exactamente la posición del bloque de trabajo, la desplazamos al norte (un bloque al lado)
        if (rawTarget.equals(workPos)) {
            return workPos.north();
        }
        return rawTarget;
    }

    @Override
    public boolean canUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        if (this.searchCooldown > 0) {
            this.searchCooldown--;
        }

        if (npc.getValidWorkPos() == null && this.searchCooldown <= 0) {
            npc.searchAndAssignWorkstation();

            if (npc.getValidWorkPos() == null) {
                this.searchCooldown = 100;
                return false;
            }
        }

        if (npc.getValidWorkPos() == null) {
            return false;
        }

        if (npc.level() instanceof ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            ProfessionLogic logic = ProfessionRegistry.getLogic(npc.getProfession());
            return isWorkTime(timeOfDay, logic);
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
            ProfessionLogic logic = ProfessionRegistry.getLogic(npc.getProfession());
            if (!isWorkTime(timeOfDay, logic)) {
                return false;
            }
        } else {
            return false;
        }

        BlockPos target = getEffectiveTarget(workPos);

        if (npc.blockPosition().distSqr(target) <= 2.0D) {
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

        // Usa la posición lateral segura
        BlockPos target = getEffectiveTarget(workPos);
        npc.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speedModifier);
    }

    @Override
    public void tick() {
        super.tick();

        BlockPos workPos = npc.getValidWorkPos();
        if (workPos == null) return;

        BlockPos target = getEffectiveTarget(workPos);
        double distSqr = npc.blockPosition().distSqr(target);

        if (distSqr > 2.0D) {
            if (npc.getNavigation().isDone() && noPathTicks == 0) {
                moveToTarget();
            }
            return;
        }

        noPathTicks = 0;
        npc.getNavigation().stop();

        ProfessionLogic logic = ProfessionRegistry.getLogic(npc.getProfession());
        npc.equipWorkTool(logic.getWorkTool());

        // El NPC se para al lado (en target) pero mira fijamente hacia la mesa de trabajo real (workPos)
        npc.getLookControl().setLookAt(
                workPos.getX() + 0.5D,
                workPos.getY() + 0.5D,
                workPos.getZ() + 0.5D,
                30.0F, 30.0F
        );

        workTimer++;
        logic.tickWork(npc, target, workPos, workTimer);

        if (workTimer >= 40) {
            workTimer = 0;
            logic.performWork(npc, target, workPos);
        }
    }

    @Override
    public void stop() {
        npc.restoreOriginalHand();

        if (npc.level() instanceof ServerLevel serverLevel && npc.getValidWorkPos() != null) {
            BlockPos workPos = npc.getValidWorkPos();
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            ProfessionLogic logic = ProfessionRegistry.getLogic(npc.getProfession());

            if (!isWorkTime(timeOfDay, logic)) {
                npc.getNavigation().moveTo(workPos.getX() + 0.5D, workPos.getY(), workPos.getZ() + 0.5D, 1.2D);
            }

            BlockPos target = getEffectiveTarget(workPos);
            serverLevel.destroyBlockProgress(npc.getId(), target, -1);
        }
        workTimer = 0;
        noPathTicks = 0;
    }
}