package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class MasonProfession implements ProfessionLogic {

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        // 1. Prioridad: Guardar en el cofre
        if ((endingFirst || endingSecond || hasEnoughItemsToStore(inventory)) && hasAnyItemInBackpack(inventory)) {
            BlockPos chestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (chestPos != null) {
                return chestPos.north().immutable();
            }
        }

        // 2. Posicionamiento seguro garantizando que haya espacio libre y suelo sólido abajo
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos sidePos = workPos.relative(dir);
            if (!serverLevel.getBlockState(sidePos).blocksMotion() && serverLevel.getBlockState(sidePos.below()).blocksMotion()) {
                return sidePos.immutable();
            }
        }

        // Fallback seguro
        return workPos.north().immutable();
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        if (handleChestTick(serverLevel, targetPos, inventory, workTimer, npc)) return;

        // Fija la mirada en el cortapiedras
        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 0.5D, workPos.getZ() + 0.5D, 30.0F, 30.0F);

        // Efectos visuales de cortar piedra
        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            serverLevel.playSound(null, workPos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.5F, 1.0F);

            // Corrección aquí: Usando BlockParticleOption para las partículas de piedra
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    workPos.getX() + 0.5D, workPos.getY() + 1.0D, workPos.getZ() + 0.5D,
                    5, 0.2, 0.2, 0.2, 0.05);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        // Maneja automáticamente la apertura, transferencia y cierre del cofre
        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        // 2. Generación de recompensas de cantería
        npc.swing(InteractionHand.MAIN_HAND, true);
        giveMasonReward(npc, serverLevel, workPos);
    }

    private void giveMasonReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.60F) { // Adoquín común
            reward = new ItemStack(Items.COBBLESTONE, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.85F) { // Ladrillos
            reward = new ItemStack(Items.BRICK, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.95F) { // Bola de arcilla
            reward = new ItemStack(Items.CLAY_BALL, 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.99F) { // Piedra lisa
            reward = new ItemStack(Items.SMOOTH_STONE, 1);
        } else { // Esmeralda (muy raro)
            reward = new ItemStack(Items.EMERALD, 1);
        }

        npc.addItemToBackpack(reward);

        // Sonido de guardado en mochila
        serverLevel.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.8F, 1.2F);
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return true;
    }

    @Override
    public ItemStack getWorkTool() {
        return new ItemStack(Items.STONE_PICKAXE); // Usa un pico de piedra para trabajar
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