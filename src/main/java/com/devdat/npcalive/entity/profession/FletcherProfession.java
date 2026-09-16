package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class FletcherProfession implements ProfessionLogic {

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

        // 2. Posicionamiento seguro alrededor de la mesa
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

        // Fija la mirada en la mesa de flechas
        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 0.5D, workPos.getZ() + 0.5D, 30.0F, 30.0F);

        // Efectos visuales de estar tallando flechas
        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            // Sonido de aldeano flechero trabajando
            serverLevel.playSound(null, workPos, SoundEvents.VILLAGER_WORK_FLETCHER, SoundSource.BLOCKS, 0.5F, serverLevel.getRandom().nextFloat() * 0.2F + 0.9F);

            // Partículas de "golpe/corte" sutiles (como si estuviera tallando)
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    workPos.getX() + 0.5D, workPos.getY() + 1.1D, workPos.getZ() + 0.5D,
                    3, 0.2, 0.2, 0.2, 0.05);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        // Maneja la interacción del cofre automáticamente
        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        // Generación de recompensas de flechero
        npc.swing(InteractionHand.MAIN_HAND, true);
        giveFletcherReward(npc, serverLevel, workPos);
    }

    private void giveFletcherReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.50F) { // Flechas normales (muy común)
            reward = new ItemStack(Items.ARROW, 2 + serverLevel.getRandom().nextInt(3));
        } else if (roll < 0.75F) { // Palos o Plumas
            reward = serverLevel.getRandom().nextBoolean()
                    ? new ItemStack(Items.STICK, 1 + serverLevel.getRandom().nextInt(2))
                    : new ItemStack(Items.FEATHER, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.90F) { // Pedernal
            reward = new ItemStack(Items.FLINT, 1);
        } else if (roll < 0.98F) { // Arco o Ballesta
            reward = serverLevel.getRandom().nextBoolean()
                    ? new ItemStack(Items.BOW, 1)
                    : new ItemStack(Items.CROSSBOW, 1);
        } else { // Esmeralda (muy raro)
            reward = new ItemStack(Items.EMERALD, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.ARROW_HIT, SoundSource.BLOCKS, 0.5F, 1.2F);
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return true;
    }

    @Override
    public ItemStack getWorkTool() {
        return new ItemStack(Items.FLINT); // Usa un pedernal en la mano como herramienta
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