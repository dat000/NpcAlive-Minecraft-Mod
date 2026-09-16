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

public class LeatherworkerProfession implements ProfessionLogic {

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

        // 2. Posicionamiento seguro alrededor del caldero
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

        // Fija la mirada en el caldero
        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 0.5D, workPos.getZ() + 0.5D, 30.0F, 30.0F);

        // Efectos visuales de trabajar el cuero (burbujas/salpicaduras sutiles)
        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            // Sonido de aldeano peletero trabajando
            serverLevel.playSound(null, workPos, SoundEvents.VILLAGER_WORK_LEATHERWORKER, SoundSource.BLOCKS, 0.5F, serverLevel.getRandom().nextFloat() * 0.2F + 0.9F);

            // Partículas de agua o burbujas simulando el lavado/teñido en el caldero
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    workPos.getX() + 0.5D, workPos.getY() + 1.0D, workPos.getZ() + 0.5D,
                    4, 0.2, 0.1, 0.2, 0.0);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        // Maneja la interacción del cofre automáticamente
        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        // Generación de recompensas de peletería
        npc.swing(InteractionHand.MAIN_HAND, true);
        giveLeatherworkerReward(npc, serverLevel, workPos);
    }

    private void giveLeatherworkerReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.50F) { // Cuero común (muy común)
            reward = new ItemStack(Items.LEATHER, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.70F) { // Piel de conejo
            reward = new ItemStack(Items.RABBIT_HIDE, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.88F) { // Piezas de armadura de cuero aleatorias
            Item[] leatherArmor = {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS};
            reward = new ItemStack(leatherArmor[serverLevel.getRandom().nextInt(leatherArmor.length)], 1);
        } else if (roll < 0.98F) { // Silla de montar (Saddle)
            reward = new ItemStack(Items.SADDLE, 1);
        } else { // Esmeralda (muy raro)
            reward = new ItemStack(Items.EMERALD, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.0F);
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
        return new ItemStack(Items.LEATHER); // Sostiene un pedazo de cuero como herramienta
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