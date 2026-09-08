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

public class FishermanProfession implements ProfessionLogic {

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        // Prioridad absoluta: Guardar ítems en el cofre si el turno está por terminar o la mochila está llena
        if ((endingFirst || endingSecond || hasEnoughItemsToStore(inventory)) && hasAnyItemInBackpack(inventory)) {
            BlockPos chestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (chestPos != null) {
                return chestPos.north().immutable();
            }
        }

        // Búsqueda inteligente de agua cerca del barril (en un radio de 4 bloques)
        BlockPos waterPos = findNearbyWater(serverLevel, workPos, 4);
        if (waterPos != null) {
            // Devuelve un bloque adyacente al agua para que el NPC se pare en la orilla
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos sidePos = waterPos.relative(dir);
                if (serverLevel.getBlockState(sidePos).isAir() && serverLevel.getBlockState(sidePos.below()).blocksMotion()) {
                    return sidePos.immutable();
                }
            }
            return waterPos.above().immutable();
        }

        // Si no encuentra agua cerca, por defecto usa el frente del barril
        net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(workPos);
        net.minecraft.core.Direction facing = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                : net.minecraft.core.Direction.NORTH;

        return workPos.relative(facing).immutable();
    }

    private BlockPos findNearbyWater(ServerLevel level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(mutablePos).is(Blocks.WATER)) {
                        return mutablePos.immutable();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        // Busca agua alrededor de su posición de trabajo para lanzar los efectos ahí
        BlockPos waterPos = findNearbyWater(serverLevel, workPos, 5);
        BlockPos effectPos = waterPos != null ? waterPos : workPos;

        // El NPC hace gestos con la caña hacia el agua
        if (workTimer % 20 == 0) {
            npc.swing(InteractionHand.MAIN_HAND, true);
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    effectPos.getX() + 0.5D, effectPos.getY() + 1.0D, effectPos.getZ() + 0.5D,
                    4, 0.3, 0.1, 0.3, 0.1);
            serverLevel.playSound(null, effectPos, SoundEvents.FISHING_BOBBER_THROW, SoundSource.BLOCKS, 0.5F, 1.0F);
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

        // 2. Obtener recompensa de pesca
        BlockPos waterPos = findNearbyWater(serverLevel, workPos, 5);
        BlockPos effectPos = waterPos != null ? waterPos : workPos;

        npc.swing(InteractionHand.MAIN_HAND, true);

        // Cooldown de 200 ticks (10 segundos) para la recompensa
        if (serverLevel.getGameTime() % 200 == 0) {
            giveFishermanReward(npc, serverLevel, effectPos);
        }
    }

    private void giveFishermanReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.60F) {
            reward = new ItemStack(Items.COD, 1);
        } else if (roll < 0.85F) {
            reward = new ItemStack(Items.SALMON, 1);
        } else if (roll < 0.95F) {
            reward = new ItemStack(Items.TROPICAL_FISH, 1);
        } else {
            reward = new ItemStack(Items.PUFFERFISH, 1);
        }

        npc.addItemToBackpack(reward);
        serverLevel.playSound(null, pos, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return true;
    }

    @Override
    public ItemStack getWorkTool() {
        return new ItemStack(Items.FISHING_ROD);
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