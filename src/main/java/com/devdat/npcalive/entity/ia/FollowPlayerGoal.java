package com.devdat.npcalive.entity.ia;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {
    private final NpcEntity npc;
    private Player targetPlayer;
    private final double speedModifier;
    private final float stopDistance;
    private final float areaSize;

    public FollowPlayerGoal(NpcEntity npc, double speedModifier, float stopDistance, float areaSize) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.areaSize = areaSize;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (npc.isInteracting() || npc.getBehavior() != NpcEntity.NpcBehavior.FOLLOW) return false;
        Player player = npc.level().getNearestPlayer(npc, areaSize);
        if (player == null) return false;
        this.targetPlayer = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return npc.getBehavior() == NpcEntity.NpcBehavior.FOLLOW && targetPlayer != null && targetPlayer.isAlive() && npc.distanceToSqr(targetPlayer) > (stopDistance * stopDistance);
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
            this.npc.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
            if (this.npc.distanceToSqr(targetPlayer) > (stopDistance * stopDistance)) {
                this.npc.getNavigation().moveTo(targetPlayer, speedModifier);
            } else {
                this.npc.getNavigation().stop();
            }
        }
    }
}