package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class DefaultProfession implements ProfessionLogic {

    @Override
    public long getWorkStartTime() {
        return 0;
    }

    @Override
    public long getWorkEndTime() {
        return 0;
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return false; // Al ser la profesión por defecto/desempleado, no realiza tareas de trabajo específicas.
    }

    @Override
    public ItemStack getWorkTool() {
        return ItemStack.EMPTY;
    }

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        return workPos != null ? workPos : npc.blockPosition();
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        // Sin lógica en el tick por defecto
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        npc.swing(InteractionHand.MAIN_HAND, true);
        if (npc.level() instanceof ServerLevel serverLevel) {
            double px = targetPos.getX() + 0.5D;
            double py = targetPos.getY() + 0.8D;
            double pz = targetPos.getZ() + 0.5D;
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 3, 0.3, 0.3, 0.3, 0.05);
            serverLevel.playSound(null, targetPos, SoundEvents.WOOD_PLACE, SoundSource.NEUTRAL, 0.4F, 1.0F);
        }
    }
}