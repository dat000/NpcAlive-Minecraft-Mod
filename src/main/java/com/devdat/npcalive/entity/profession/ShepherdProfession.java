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

public class ShepherdProfession implements ProfessionLogic {

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

        // 2. Posicionamiento seguro alrededor del telar
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

        // Fija la mirada en el telar
        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 0.5D, workPos.getZ() + 0.5D, 30.0F, 30.0F);

        // Efectos visuales de tejer en el telar
        if (workTimer % 40 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            // Sonido del aldeano pastor trabajando
            serverLevel.playSound(null, workPos, SoundEvents.VILLAGER_WORK_SHEPHERD, SoundSource.BLOCKS, 0.5F, serverLevel.getRandom().nextFloat() * 0.2F + 0.9F);

            // Partículas felices o suaves sobre el telar
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    workPos.getX() + 0.5D, workPos.getY() + 1.0D, workPos.getZ() + 0.5D,
                    3, 0.2, 0.1, 0.2, 0.05);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        // Maneja la interacción del cofre automáticamente
        if (handleChestPerform(serverLevel, targetPos, inventory)) return;

        // Generación de recompensas de pastoreo/textiles
        npc.swing(InteractionHand.MAIN_HAND, true);
        giveShepherdReward(npc, serverLevel, workPos);
    }

    private void giveShepherdReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.45F) { // Lana blanca usando la estructura de tu entorno
            reward = new ItemStack(Blocks.WOOL.white(), 1 + serverLevel.getRandom().nextInt(2));
        } else if (roll < 0.70F) { // Hilo (String)
            reward = new ItemStack(Items.STRING, 1 + serverLevel.getRandom().nextInt(3));
        } else if (roll < 0.88F) { // Alfombra o Estandarte
            reward = serverLevel.getRandom().nextBoolean()
                    ? new ItemStack(Blocks.CARPET.white(), 1 + serverLevel.getRandom().nextInt(2))
                    : new ItemStack(Blocks.BANNER.white(), 1);
        } else if (roll < 0.98F) { // Tijeras (Shears)
            reward = new ItemStack(Items.SHEARS, 1);
        } else { // Esmeralda (muy raro)
            reward = new ItemStack(Items.EMERALD, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 0.8F, 1.2F);
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
        return new ItemStack(Items.SHEARS); // Sostiene unas tijeras como herramienta
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