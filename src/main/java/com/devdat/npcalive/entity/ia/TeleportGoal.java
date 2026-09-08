package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import com.devdat.npcalive.entity.profession.ProfessionLogic;
import com.devdat.npcalive.entity.profession.ProfessionRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.EnumSet;

public class TeleportGoal extends Goal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NpcEntity npc;
    private int teleportCooldown = 0;

    private Vec3 lastPosition = Vec3.ZERO;
    private int stationaryTicks = 0;

    public TeleportGoal(NpcEntity npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    private boolean isWorkTime(long timeOfDay, ProfessionLogic logic) {
        boolean firstShift = timeOfDay >= logic.getWorkStartTime() && timeOfDay < logic.getWorkEndTime();
        boolean secondShift = logic.getSecondWorkStartTime() != -1 && timeOfDay >= logic.getSecondWorkStartTime() && timeOfDay < logic.getSecondWorkEndTime();
        return firstShift || secondShift;
    }

    @Override
    public boolean canUse() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.STAY || npc.isSleeping()) {
            return false;
        }

        if (teleportCooldown > 0) {
            teleportCooldown--;
            return false;
        }

        // 1. CONDICIÓN EN FOLLOW
        if (npc.getBehavior() == NpcEntity.NpcBehavior.FOLLOW) {
            Player player = npc.level().getNearestPlayer(npc, 32.0D);
            if (player != null) {
                double distSq = npc.distanceToSqr(player);
                double yDiff = Math.abs(player.getY() - npc.getY());

                boolean tooFar = distSq > (18.0D * 18.0D);
                boolean heightBlocked = yDiff > 3.0D && (!npc.getNavigation().isInProgress() || npc.getNavigation().isDone());

                if (tooFar || heightBlocked) {
                    return true;
                }
            }
        }

        // 2. EXCEPCIÓN: Si está trabajando en su estación, estar quieto es normal
        if (npc.getValidWorkPos() != null && npc.level() instanceof ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            ProfessionLogic logic = ProfessionRegistry.getLogic(npc.getProfession());
            if (logic != null && isWorkTime(timeOfDay, logic)) {
                BlockPos targetWork = logic.getTargetPosition(npc, npc.getValidWorkPos());
                if (npc.blockPosition().distSqr(targetWork) <= 4.0D) {
                    stationaryTicks = 0;
                    return false; // No teletransportar si está trabajando legítimamente
                }
            }
        }

        // 3. CONDICIÓN DE ATASCO / PLATAFORMA AISLADA (WANDER, ir a la cama, etc.)
        boolean hasBedTarget = false;
        if (npc.level() instanceof ServerLevel serverLevel) {
            long timeOfDay = serverLevel.getDefaultClockTime() % 24000;
            boolean isNight = timeOfDay >= 13000 && timeOfDay < 23000;
            hasBedTarget = npc.getBedPos() != null && isNight;
        }

        boolean isTryingToMove = npc.getNavigation().isInProgress() || npc.getBehavior() == NpcEntity.NpcBehavior.WANDER;

        if (hasBedTarget || isTryingToMove) {
            double movedDistSq = npc.position().distanceToSqr(lastPosition);
            if (movedDistSq < 0.002D) {
                stationaryTicks++;
                if (stationaryTicks > 100) {
                    stationaryTicks = 0;
                    return true;
                }
            } else {
                stationaryTicks = 0;
            }
        }

        lastPosition = npc.position();
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        if (npc.getBehavior() == NpcEntity.NpcBehavior.FOLLOW) {
            Player player = npc.level().getNearestPlayer(npc, 32.0D);
            if (player != null) {
                performSafePlayerTeleport(player);
                teleportCooldown = 80;
                return;
            }
        }

        teleportAlongPathOrTowardsTarget();
        teleportCooldown = 80;
    }

    private void performSafePlayerTeleport(Player player) {
        var level = npc.level();
        Vec3 playerPos = player.position();
        BlockPos targetPos = new BlockPos((int)playerPos.x, (int)playerPos.y, (int)playerPos.z);

        BlockPos groundPos = targetPos.below();
        BlockState groundState = level.getBlockState(groundPos);
        boolean isSolidGround = !groundState.isAir() && groundState.blocksMotion();

        if (player.onClimbable() || !isSolidGround) {
            BlockPos.MutableBlockPos mutablePos = targetPos.mutable();
            boolean foundSafeFloor = false;

            for (int i = 0; i < 10; i++) {
                mutablePos.move(0, -1, 0);
                BlockState state = level.getBlockState(mutablePos);
                if (!state.isAir() && state.blocksMotion()) {
                    targetPos = mutablePos.above();
                    foundSafeFloor = true;
                    break;
                }
            }

            if (!foundSafeFloor) {
                teleportCooldown = 40;
                return;
            }
        }

        if (!level.getBlockState(targetPos).isAir() || !level.getBlockState(targetPos.above()).isAir()) {
            teleportCooldown = 40;
            return;
        }

        npc.teleportTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
        level.playSound(null, npc.getX(), npc.getY(), npc.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.0F);
        npc.getNavigation().stop();
    }

    private void teleportAlongPathOrTowardsTarget() {
        Level level = npc.level();
        Path path = npc.getNavigation().getPath();
        BlockPos safeTargetPos = null;

        if (path != null && !path.isDone()) {
            int currentIndex = path.getNextNodeIndex();
            for (int i = currentIndex + 2; i < Math.min(currentIndex + 8, path.getNodeCount()); i++) {
                Node node = path.getNode(i);
                BlockPos nodePos = new BlockPos(node.x, node.y, node.z);
                if (isSafePosition(level, nodePos)) {
                    safeTargetPos = nodePos;
                    break;
                }
            }
        }

        if (safeTargetPos == null && npc.getBedPos() != null) {
            BlockPos bedPos = npc.getBedPos();
            Vec3 npcPos = npc.position();
            Vec3 targetVec = new Vec3(bedPos.getX() + 0.5D, bedPos.getY(), bedPos.getZ() + 0.5D);

            Vec3 dir = targetVec.subtract(npcPos).normalize();
            for (int dist = 5; dist >= 2; dist--) {
                Vec3 interVec = npcPos.add(dir.scale(dist));
                BlockPos checkPos = new BlockPos((int)interVec.x, (int)interVec.y, (int)interVec.z);
                BlockPos groundCheck = findGroundNear(level, checkPos);
                if (groundCheck != null) {
                    safeTargetPos = groundCheck;
                    break;
                }
            }
        }

        if (safeTargetPos == null) {
            safeTargetPos = findGroundNear(level, npc.blockPosition());
        }

        if (safeTargetPos != null) {
            npc.teleportTo(safeTargetPos.getX() + 0.5D, safeTargetPos.getY(), safeTargetPos.getZ() + 0.5D);
            level.playSound(null, npc.getX(), npc.getY(), npc.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.8F, 1.0F);
        } else {
            npc.getNavigation().stop();
        }
    }

    private boolean isSafePosition(Level level, BlockPos pos) {
        BlockState groundState = level.getBlockState(pos.below());
        boolean isGroundSolid = !groundState.isAir() && groundState.blocksMotion();
        boolean isSpaceClear = level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
        return isGroundSolid && isSpaceClear;
    }

    private BlockPos findGroundNear(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();

        for (int yOffset = 2; yOffset >= -12; yOffset--) {
            mutable.set(pos.getX(), pos.getY() + yOffset, pos.getZ());
            if (isSafePosition(level, mutable)) {
                return mutable.immutable();
            }
        }
        return null;
    }
}