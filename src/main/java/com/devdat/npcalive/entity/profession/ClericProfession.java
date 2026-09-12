package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;

public class ClericProfession implements ProfessionLogic {

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        // 1. Prioridad: Guardar en el cofre si termina el turno o la mochila está llena
        if ((endingFirst || endingSecond || hasEnoughItemsToStore(inventory)) && hasAnyItemInBackpack(inventory)) {
            BlockPos chestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (chestPos != null) {
                return chestPos.north().immutable();
            }
        }

        // 2. Posicionamiento seguro alrededor del soporte de alquimia
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos sidePos = workPos.relative(dir);
            if (!serverLevel.getBlockState(sidePos).blocksMotion() && serverLevel.getBlockState(sidePos.below()).blocksMotion()) {
                return sidePos.immutable();
            }
        }

        return workPos.north().immutable();
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();
        if (handleChestTick(serverLevel, targetPos, inventory, workTimer, npc)) return;

        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            serverLevel.playSound(null, workPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    workPos.getX() + 0.5, workPos.getY() + 1.0, workPos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        // Maneja automáticamente la apertura, transferencia y cierre del cofre
        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        // 2. Generación de recompensas de alquimia
        npc.swing(InteractionHand.MAIN_HAND, true);
        giveClericReward(npc, serverLevel, workPos);
    }

    private void giveClericReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.85F) { // Polvo de Redstone o Piedra Luminosa (Básico)
            reward = serverLevel.getRandom().nextBoolean()
                    ? new ItemStack(Items.REDSTONE, 1 + serverLevel.getRandom().nextInt(2))
                    : new ItemStack(Items.GLOWSTONE_DUST, 1);
        } else if (roll < 0.95F) { // Lapislázuli o Verruga Abisal (Intermedio)
            reward = serverLevel.getRandom().nextBoolean()
                    ? new ItemStack(Items.LAPIS_LAZULI, 1)
                    : new ItemStack(Items.NETHER_WART, 1);
        } else if (roll < 0.99F) { // Polvo de Blaze (Raro)
            reward = new ItemStack(Items.BLAZE_POWDER, 1);
        } else { // Botella de experiencia (Muy raro)
            reward = new ItemStack(Items.EXPERIENCE_BOTTLE, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.2F);
    }

    @Override
    public BlockPos findChestAdjacentToWorkstation(ServerLevel level, BlockPos workPos) {
        // Busca cofres tanto a la altura del soporte como un bloque más abajo (en el suelo junto a la mesa)
        for (BlockPos center : new BlockPos[]{workPos, workPos.below()}) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos checkPos = center.relative(dir);
                if (level.getBlockState(checkPos).is(Blocks.CHEST) || level.getBlockState(checkPos).is(Blocks.TRAPPED_CHEST)) {
                    return checkPos.immutable();
                }
            }
        }
        return null;
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return true;
    }

    @Override
    public ItemStack getWorkTool() {
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public long getWorkStartTime() {
        return 2000L;
    }

    @Override
    public long getWorkEndTime() {
        return 6000L;
    }

    @Override
    public long getSecondWorkStartTime() {
        return 8000L;
    }

    @Override
    public long getSecondWorkEndTime() {
        return 12000L;
    }
}