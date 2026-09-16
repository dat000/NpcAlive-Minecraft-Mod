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

public class ToolsmithProfession implements ProfessionLogic {

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

        // 2. Posicionamiento seguro alrededor de la mesa de herrería
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

        // Fija la mirada en la mesa de herrería
        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 0.5D, workPos.getZ() + 0.5D, 30.0F, 30.0F);

        // Efectos visuales de trabajo en la mesa de herrería
        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            // Sonido del aldeano herramentero trabajando
            serverLevel.playSound(null, workPos, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.BLOCKS, 0.5F, serverLevel.getRandom().nextFloat() * 0.2F + 0.9F);

            // Partículas sutiles de chispa o crítico sobre la mesa
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    workPos.getX() + 0.5D, workPos.getY() + 1.0D, workPos.getZ() + 0.5D,
                    3, 0.2, 0.1, 0.2, 0.0);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        // Maneja la interacción del cofre automáticamente
        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        // Generación de recompensas de herrería de herramientas
        npc.swing(InteractionHand.MAIN_HAND, true);
        giveToolsmithReward(npc, serverLevel, workPos);
    }

    private void giveToolsmithReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.45F) { // Carbón o lingotes de hierro comunes
            reward = new ItemStack(Items.IRON_INGOT, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.70F) { // Herramientas de hierro (Pico o Hacha)
            Item[] tools = {Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE};
            reward = new ItemStack(tools[serverLevel.getRandom().nextInt(tools.length)], 1);
        } else if (roll < 0.90F) { // Diamante (muy valioso, como en el juego)
            reward = new ItemStack(Items.DIAMOND, 1);
        } else if (roll < 0.98F) { // Herramienta de diamante de recompensa alta
            reward = new ItemStack(Items.DIAMOND_PICKAXE, 1);
        } else { // Esmeralda
            reward = new ItemStack(Items.EMERALD, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
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
        return new ItemStack(Items.IRON_PICKAXE);
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