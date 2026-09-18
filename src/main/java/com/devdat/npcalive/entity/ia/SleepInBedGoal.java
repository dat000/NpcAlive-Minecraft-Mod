package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class SleepInBedGoal extends Goal {
    private final NpcEntity npc;
    private final double speedModifier;
    private int noPathTicks = 0;
    private boolean hasTriedToSleep = false;

    public SleepInBedGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private BlockPos getBedHeadPos() {
        BlockPos pos = npc.getBedPos();
        if (pos == null) {
            return null;
        }

        BlockState state = npc.level().getBlockState(pos);

        if (!(state.getBlock() instanceof BedBlock)) {
            npc.setBedPos(null);
            if (npc.isSleeping()) {
                npc.stopSleeping();
            }
            return null;
        }

        if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return pos;
        }

        // Búsqueda inteligente
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = npc.level().getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof BedBlock &&
                    neighborState.getValue(BedBlock.PART) == BedPart.HEAD) {
                return neighborPos;
            }
        }

        return pos;
    }

    private boolean isAnotherNpcInBed(BlockPos bedHead) {
        AABB bedBox = new AABB(bedHead).inflate(0.2D);
        List<NpcEntity> nearbyNpcs = npc.level().getEntitiesOfClass(NpcEntity.class, bedBox);

        for (NpcEntity otherNpc : nearbyNpcs) {
            if (otherNpc != npc) {
                if (otherNpc.isSleeping() || otherNpc.blockPosition().equals(bedHead)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) {
            return false;
        }

        BlockPos currentHead = getBedHeadPos();
        if (currentHead == null || npc.isBedTaken(currentHead) || isAnotherNpcInBed(currentHead)) {
            npc.setBedPos(null);
            npc.searchAndAssignBed();
        }

        BlockPos bedHead = getBedHeadPos();
        if (bedHead == null) {
            return false;
        }

        if (isAnotherNpcInBed(bedHead)) {
            npc.setBedPos(null);
            return false;
        }

        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            boolean isNight = timeOfDay >= 13000 && timeOfDay < 23000;
            return isNight;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) {
            return false;
        }

        BlockPos bedHead = getBedHeadPos();
        if (bedHead == null) {
            if (npc.isSleeping()) npc.stopSleeping();
            return false;
        }

        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            boolean isNight = timeOfDay >= 13000 && timeOfDay < 23000;
            if (!isNight) {
                if (npc.isSleeping()) npc.stopSleeping();
                return false;
            }
        } else {
            return false;
        }

        if (npc.isSleeping()) {
            return true;
        }

        double distanceSqr = npc.position().distanceToSqr(bedHead.getX() + 0.5D, bedHead.getY(), bedHead.getZ() + 0.5D);

        if (distanceSqr <= 2.0D) {
            npc.getNavigation().stop();

            if (!hasTriedToSleep) {
                if (isAnotherNpcInBed(bedHead)) {
                    npc.setBedPos(null);
                    npc.searchAndAssignBed();
                    return false;
                }

                npc.startSleeping(bedHead);
                hasTriedToSleep = true;
            }
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
        BlockPos bedHead = getBedHeadPos();
        if (bedHead != null) {
            noPathTicks = 0;
            hasTriedToSleep = false;
            npc.getNavigation().moveTo(bedHead.getX() + 0.5D, bedHead.getY(), bedHead.getZ() + 0.5D, speedModifier);
        }
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
        if (npc.isSleeping()) {
            npc.stopSleeping();
        }
        noPathTicks = 0;
        hasTriedToSleep = false;
    }
}