package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.EnumSet;

public class SleepInBedGoal extends Goal {
    private final NpcEntity npc;
    private final double speedModifier;
    private int noPathTicks = 0;

    public SleepInBedGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    /**
     * Metodo auxiliar para garantizar que siempre busquemos la almohada (HEAD),
     * sin importar si el bloque guardado fue el de los pies.
     */
    private BlockPos getBedHeadPos() {
        BlockPos pos = npc.getBedPos();
        if (pos == null) return null;

        BlockState state = npc.level().getBlockState(pos);
        if (state.getBlock() instanceof BedBlock) {
            if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                Direction facing = state.getValue(BedBlock.FACING);
                return pos.relative(facing); // Nos movemos hacia donde apunta la cama (la cabecera)
            }
        }
        return pos;
    }

    @Override
    public boolean canUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        BlockPos bedHead = getBedHeadPos();
        if (bedHead == null) return false;

        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            return timeOfDay >= 13000 && timeOfDay < 23000;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        BlockPos bedHead = getBedHeadPos();
        if (bedHead == null) return false;

        if (npc.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            boolean isNight = timeOfDay >= 13000 && timeOfDay < 23000;
            if (!isNight) {
                if (npc.isSleeping()) {
                    npc.stopSleeping();
                }
                return false;
            }
        } else {
            return false;
        }

        // Usamos la posición de la cabecera para detenernos
        if (npc.blockPosition().distSqr(bedHead) <= 2.5D) {
            npc.getNavigation().stop();
            if (!npc.isSleeping()) {
                // Ajustamos la posición exactamente al centro de la almohada con la altura vainilla (0.6875D)
                npc.setPos(bedHead.getX() + 0.5D, bedHead.getY() + 0.6875D, bedHead.getZ() + 0.5D);
                npc.startSleeping(bedHead);
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
    }
}