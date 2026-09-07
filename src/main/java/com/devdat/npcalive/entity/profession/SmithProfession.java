package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;

import java.util.ArrayList;
import java.util.List;

public class SmithProfession implements ProfessionLogic {

    private static final int WORK_RADIUS = 6;

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        // Prioridad absoluta: Guardar ítems en el cofre al terminar turno o si está lleno
        if ((endingFirst || endingSecond || hasEnoughItemsToStore(inventory)) && hasAnyItemInBackpack(inventory)) {
            BlockPos chestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (chestPos != null) {
                return chestPos.north().immutable();
            }
        }

        // Recolectar estaciones disponibles alrededor de la mesa de trabajo
        List<BlockPos> availableStations = new ArrayList<>();
        availableStations.add(workPos); // Mesa de trabajo principal

        BlockPos blastFurnacePos = getAdjacentBlock(serverLevel, workPos, Blocks.BLAST_FURNACE);
        if (blastFurnacePos != null) {
            availableStations.add(blastFurnacePos.north().immutable());
        }

        BlockPos anvilPos = getAdjacentBlock(serverLevel, workPos, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        if (anvilPos != null) {
            availableStations.add(anvilPos.north().immutable());
        }

        // Rotación estable: cambia de estación cada 200 ticks (10 segundos) de forma fluida
        long timeSlot = serverLevel.getGameTime() / 200;
        int index = (int) Math.abs((npc.getUUID().getLeastSignificantBits() + timeSlot) % availableStations.size());

        return availableStations.get(index);
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        BlockPos blastFurnace = getAdjacentBlock(serverLevel, targetPos, Blocks.BLAST_FURNACE);
        if (blastFurnace != null) {
            if (workTimer % 20 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, blastFurnace.getX() + 0.5D, blastFurnace.getY() + 1.0D, blastFurnace.getZ() + 0.5D, 2, 0.2, 0.2, 0.2, 0.02);
                serverLevel.playSound(null, blastFurnace, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return;
        }

        BlockPos anvil = getAdjacentBlock(serverLevel, targetPos, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        if (anvil != null) {
            if (workTimer % 40 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.sendParticles(ParticleTypes.LAVA, anvil.getX() + 0.5D, anvil.getY() + 1.0D, anvil.getZ() + 0.5D, 1, 0.1, 0.1, 0.1, 0.01);
                serverLevel.playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 0.8F);
            }
            return;
        }

        // Trabajo principal en la mesa de trabajo (workPos)
        if (targetPos.equals(workPos)) {
            if (workTimer % 40 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.playSound(null, workPos, SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.5F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        workPos.getX() + 0.5, workPos.getY() + 1.1, workPos.getZ() + 0.5,
                        3, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        SimpleContainer inventory = npc.getInventory();

        // 1. Verificación de cofre para vaciar inventario
        BlockPos chestPos = getAdjacentBlock(serverLevel, targetPos, Blocks.CHEST, Blocks.TRAPPED_CHEST);
        if (chestPos != null) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(chestPos);
            if (blockEntity instanceof Container chestContainer) {
                if (transferBackpackToChest(inventory, chestContainer)) {
                    serverLevel.blockEvent(chestPos, serverLevel.getBlockState(chestPos).getBlock(), 1, 0);
                    serverLevel.playSound(null, chestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
                    return;
                }
            }
        }

        // 2. Trabajo en Alto Horno
        BlockPos blastFurnace = getAdjacentBlock(serverLevel, targetPos, Blocks.BLAST_FURNACE);
        if (blastFurnace != null) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            serverLevel.playSound(null, blastFurnace, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            return;
        }

        // 3. Trabajo en Yunque
        BlockPos anvil = getAdjacentBlock(serverLevel, targetPos, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        if (anvil != null) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            giveSmithReward(npc, serverLevel, anvil);
            return;
        }

        // 4. Trabajo principal en su estación (workPos)
        if (targetPos.equals(workPos)) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            giveSmithReward(npc, serverLevel, workPos);
        }
    }

    private void giveSmithReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.90F) { // 90% de probabilidad: Pepitas o Carbón (Materiales básicos)
            reward = serverLevel.getRandom().nextBoolean()
                    ? new ItemStack(Items.IRON_NUGGET, 1 + serverLevel.getRandom().nextInt(2))
                    : new ItemStack(Items.COAL, 1);
        } else if (roll < 0.98F) { // 8% de probabilidad: Lingote de hierro (Intermedio)
            reward = new ItemStack(Items.IRON_INGOT, 1);
        } else if (roll < 0.998F) { // 1.8% de probabilidad: Pico de hierro (Raro)
            reward = new ItemStack(Items.IRON_PICKAXE, 1);
        } else { // 0.2% de probabilidad: Armadura de caballo de hierro (Muy raro / Excepcional)
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