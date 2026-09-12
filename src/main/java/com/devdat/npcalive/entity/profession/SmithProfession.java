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

import java.util.ArrayList;
import java.util.List;

public class SmithProfession implements ProfessionLogic {

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        // Prioridad absoluta: Buscar cofre al lado de CUALQUIER estación del herrero (Mesa, Alto Horno o Yunque)
        if ((endingFirst || endingSecond || hasEnoughItemsToStore(inventory)) && hasAnyItemInBackpack(inventory)) {
            BlockPos chestPos = findChestAdjacentToAnyStation(serverLevel, workPos);
            if (chestPos != null) {
                return chestPos.north().immutable();
            }
        }

        // Recolectar estaciones disponibles alrededor y calcular posiciones de parada seguras
        List<BlockPos> availableStations = new ArrayList<>();

        // 1. Estación principal (Mesa de trabajo)
        availableStations.add(findSafeStandingNear(serverLevel, workPos));

        // 2. Alto Horno (si existe)
        BlockPos blastFurnacePos = getAdjacentBlock(serverLevel, workPos, Blocks.BLAST_FURNACE);
        if (blastFurnacePos != null) {
            availableStations.add(findSafeStandingNear(serverLevel, blastFurnacePos));
        }

        // 3. Yunque (si existe)
        BlockPos anvilPos = getAdjacentBlock(serverLevel, workPos, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        if (anvilPos != null) {
            availableStations.add(findSafeStandingNear(serverLevel, anvilPos));
        }

        // Rotación estable: cambia de estación cada 200 ticks (10 segundos) de forma fluida
        long timeSlot = serverLevel.getGameTime() / 200;
        int index = (int) Math.abs((npc.getUUID().getLeastSignificantBits() + timeSlot) % availableStations.size());

        return availableStations.get(index);
    }

    // Método auxiliar para buscar cofre alrededor de la mesa, alto horno o yunque
    private BlockPos findChestAdjacentToAnyStation(ServerLevel level, BlockPos workPos) {
        // Revisar mesa principal
        BlockPos chest = findChestAdjacentToWorkstation(level, workPos);
        if (chest != null) return chest;

        // Revisar alto horno
        BlockPos blastFurnace = getAdjacentBlock(level, workPos, Blocks.BLAST_FURNACE);
        if (blastFurnace != null) {
            chest = findChestAdjacentToWorkstation(level, blastFurnace);
            if (chest != null) return chest;
        }

        // Revisar yunque
        BlockPos anvil = getAdjacentBlock(level, workPos, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        if (anvil != null) {
            chest = findChestAdjacentToWorkstation(level, anvil);
            if (chest != null) return chest;
        }

        return null;
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

        BlockPos blastFurnace = getAdjacentBlock(serverLevel, targetPos, Blocks.BLAST_FURNACE);
        if (blastFurnace != null) {
            npc.getLookControl().setLookAt(blastFurnace.getX() + 0.5D, blastFurnace.getY() + 0.5D, blastFurnace.getZ() + 0.5D, 30.0F, 30.0F);
            if (workTimer % 20 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, blastFurnace.getX() + 0.5D, blastFurnace.getY() + 1.0D, blastFurnace.getZ() + 0.5D, 2, 0.2, 0.2, 0.2, 0.02);
                serverLevel.playSound(null, blastFurnace, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return;
        }

        BlockPos anvil = getAdjacentBlock(serverLevel, targetPos, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        if (anvil != null) {
            npc.getLookControl().setLookAt(anvil.getX() + 0.5D, anvil.getY() + 0.5D, anvil.getZ() + 0.5D, 30.0F, 30.0F);
            if (workTimer % 40 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.sendParticles(ParticleTypes.LAVA, anvil.getX() + 0.5D, anvil.getY() + 1.0D, anvil.getZ() + 0.5D, 1, 0.1, 0.1, 0.1, 0.01);
                serverLevel.playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 0.8F);
            }
            return;
        }

        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 0.5D, workPos.getZ() + 0.5D, 30.0F, 30.0F);
        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            serverLevel.playSound(null, workPos, SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.5F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    workPos.getX() + 0.5, workPos.getY() + 1.1, workPos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        BlockPos blastFurnace = getAdjacentBlock(serverLevel, targetPos, Blocks.BLAST_FURNACE);
        if (blastFurnace != null) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            serverLevel.playSound(null, blastFurnace, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            return;
        }

        BlockPos anvil = getAdjacentBlock(serverLevel, targetPos, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        if (anvil != null) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            giveSmithReward(npc, serverLevel, anvil);
            return;
        }

        npc.swing(InteractionHand.MAIN_HAND, true);
        giveSmithReward(npc, serverLevel, workPos);
    }

    private void giveSmithReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.90F) {
            reward = serverLevel.getRandom().nextBoolean()
                    ? new ItemStack(Items.IRON_NUGGET, 1 + serverLevel.getRandom().nextInt(2))
                    : new ItemStack(Items.COAL, 1);
        } else if (roll < 0.98F) {
            reward = new ItemStack(Items.IRON_INGOT, 1);
        } else if (roll < 0.998F) {
            reward = new ItemStack(Items.IRON_PICKAXE, 1);
        } else {
            reward = new ItemStack(Items.IRON_HORSE_ARMOR, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 0.9F);
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