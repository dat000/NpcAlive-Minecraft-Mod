package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ArmorerProfession implements ProfessionLogic {

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
                return findSafeStandingNear(serverLevel, chestPos);
            }
        }

        // 2. Posicionamiento seguro alrededor del alto horno
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos sidePos = workPos.relative(dir);
            if (!serverLevel.getBlockState(sidePos).blocksMotion() && serverLevel.getBlockState(sidePos.below()).blocksMotion()) {
                return sidePos.immutable();
            }
        }

        return workPos.north().immutable();
    }

    private BlockPos findSafeStandingNear(ServerLevel level, BlockPos targetBlockPos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos sidePos = targetBlockPos.relative(dir);
            if (!level.getBlockState(sidePos).blocksMotion() && level.getBlockState(sidePos.below()).blocksMotion()) {
                return sidePos.immutable();
            }
        }
        return targetBlockPos.north().immutable();
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        if (handleChestTick(serverLevel, targetPos, inventory, workTimer, npc)) return;

        // Fija la mirada en el alto horno
        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 0.5D, workPos.getZ() + 0.5D, 30.0F, 30.0F);

        // Efectos visuales de fundición y humo
        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            // Sonido del aldeano armero trabajando
            serverLevel.playSound(null, workPos, SoundEvents.VILLAGER_WORK_ARMORER, SoundSource.BLOCKS, 0.5F, serverLevel.getRandom().nextFloat() * 0.2F + 0.9F);

            // Partículas de humo grande sobre el alto horno
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    workPos.getX() + 0.5D, workPos.getY() + 1.1D, workPos.getZ() + 0.5D,
                    3, 0.2, 0.2, 0.2, 0.02);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        // Maneja la interacción del cofre automáticamente
        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        // Generación de recompensas de armería
        npc.swing(InteractionHand.MAIN_HAND, true);
        giveArmorerReward(npc, serverLevel, workPos);
    }

    private void giveArmorerReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.45F) { // Pepitas de hierro
            reward = new ItemStack(Items.IRON_NUGGET, 2 + serverLevel.getRandom().nextInt(3));
        } else if (roll < 0.70F) { // Carbón
            reward = new ItemStack(Items.COAL, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.88F) { // Piezas de armadura (cota de malla o hierro)
            Item[] armorPieces = {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.IRON_HELMET, Items.IRON_BOOTS};
            reward = new ItemStack(armorPieces[serverLevel.getRandom().nextInt(armorPieces.length)], 1);
        } else if (roll < 0.98F) { // Lingote de hierro
            reward = new ItemStack(Items.IRON_INGOT, 1);
        } else { // Esmeralda
            reward = new ItemStack(Items.EMERALD, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 1.1F);
    }

    @Override
    public BlockPos findChestAdjacentToWorkstation(ServerLevel level, BlockPos workPos) {
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
        return new ItemStack(Items.IRON_INGOT);
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