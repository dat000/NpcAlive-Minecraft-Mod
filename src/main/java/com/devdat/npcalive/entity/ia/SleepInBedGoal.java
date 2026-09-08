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

    public SleepInBedGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private BlockPos getBedHeadPos() {
        BlockPos pos = npc.getBedPos();
        if (pos == null) return null;

        BlockState state = npc.level().getBlockState(pos);

        // SEGURIDAD: Si la cama fue destruida o reemplazada por aire/otro bloque
        if (!(state.getBlock() instanceof BedBlock)) {
            npc.setBedPos(null); // Olvidamos la cama rota
            if (npc.isSleeping()) {
                npc.stopSleeping(); // Nos levantamos inmediatamente
            }
            return null;
        }

        if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
            Direction facing = state.getValue(BedBlock.FACING);
            return pos.relative(facing);
        }
        return pos;
    }

    /**
     * Verifica si hay CUALQUIER otro NPC durmiendo o parado encima de la cabecera de esta cama.
     */
    private boolean isAnotherNpcInBed(BlockPos bedHead) {
        AABB bedBox = new AABB(bedHead).inflate(0.2D);
        List<NpcEntity> nearbyNpcs = npc.level().getEntitiesOfClass(NpcEntity.class, bedBox);

        for (NpcEntity otherNpc : nearbyNpcs) {
            if (otherNpc != npc) {
                // Si otro NPC ya está durmiendo aquí O está a punto de dormirse sobre el bloque
                if (otherNpc.isSleeping() || otherNpc.blockPosition().equals(bedHead)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY) return false;

        BlockPos currentHead = getBedHeadPos();

        // Si no tiene cama, si fue tomada por el mapa de asignación O si hay otro NPC durmiendo físicamente en ella
        if (currentHead == null || npc.isBedTaken(currentHead) || isAnotherNpcInBed(currentHead)) {
            npc.setBedPos(null);
            npc.searchAndAssignBed();
        }

        BlockPos bedHead = getBedHeadPos();
        if (bedHead == null) return false;

        // Doble verificación: Si la cama que le dio la búsqueda sigue teniendo a alguien durmiendo, cancela
        if (isAnotherNpcInBed(bedHead)) {
            npc.setBedPos(null);
            return false;
        }

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
        if (bedHead == null) {
            if (npc.isSleeping()) {
                npc.stopSleeping();
            }
            return false;
        }

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

        // LLEGÓ A LA CAMA
        if (npc.blockPosition().distSqr(bedHead) <= 2.5D) {
            npc.getNavigation().stop();

            if (!npc.isSleeping()) {
                // VERIFICACIÓN CRÍTICA DE ÚLTIMO SEGUNDO:
                // Si alguien más llegó un instante antes y se acostó, aborta e intenta buscar otra cama.
                if (isAnotherNpcInBed(bedHead)) {
                    npc.setBedPos(null);
                    npc.searchAndAssignBed();
                    return false;
                }

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