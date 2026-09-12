package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface ProfessionLogic {

    int BACKPACK_START = 6;

    long getWorkStartTime();
    long getWorkEndTime();
    default long getSecondWorkStartTime() { return -1; }
    default long getSecondWorkEndTime() { return -1; }

    boolean canWork(NpcEntity npc);
    ItemStack getWorkTool();
    BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos);
    void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer);
    void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos);

    // --- MÉTODOS COMPARTIDOS DE MOCHILA Y COFRES ---

    default int getBackpackEnd(SimpleContainer inventory) {
        return inventory != null ? inventory.getContainerSize() - 1 : 13;
    }

    default boolean hasAnyItemInBackpack(SimpleContainer inventory) {
        if (inventory == null) return false;
        int end = getBackpackEnd(inventory);
        for (int i = BACKPACK_START; i <= end; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    default boolean hasEnoughItemsToStore(SimpleContainer inventory) {
        if (inventory == null) return false;
        int count = 0;
        int end = getBackpackEnd(inventory);
        for (int i = BACKPACK_START; i <= end; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count >= 3;
    }

    default BlockPos findChestAdjacentToWorkstation(ServerLevel level, BlockPos workPos) {
        for (BlockPos neighbor : BlockPos.betweenClosed(workPos.offset(-1, 0, -1), workPos.offset(1, 1, 1))) {
            if (neighbor.distManhattan(workPos) <= 1) {
                BlockState state = level.getBlockState(neighbor);
                if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
                    return neighbor.immutable();
                }
            }
        }
        return null;
    }

    default void openChest(ServerLevel level, BlockPos chestPos) {
        BlockState state = level.getBlockState(chestPos);
        level.blockEvent(chestPos, state.getBlock(), 1, 1);
        level.playSound(null, chestPos, net.minecraft.sounds.SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
        level.sendBlockUpdated(chestPos, state, state, 3);

        // Si es un cofre doble, actualizar también la otra mitad para que la animación no falle
        if (state.hasProperty(net.minecraft.world.level.block.ChestBlock.TYPE)) {
            net.minecraft.world.level.block.state.properties.ChestType type = state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE);
            if (type != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                BlockPos otherPos = chestPos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
                BlockState otherState = level.getBlockState(otherPos);
                level.blockEvent(otherPos, otherState.getBlock(), 1, 1);
                level.sendBlockUpdated(otherPos, otherState, otherState, 3);
            }
        }
    }

    default void closeChest(ServerLevel level, BlockPos chestPos) {
        BlockState state = level.getBlockState(chestPos);
        level.blockEvent(chestPos, state.getBlock(), 1, 0);
        level.playSound(null, chestPos, net.minecraft.sounds.SoundEvents.CHEST_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.0F);
        level.sendBlockUpdated(chestPos, state, state, 3);

        // Sincronizar el cierre de la otra mitad si es un cofre doble
        if (state.hasProperty(net.minecraft.world.level.block.ChestBlock.TYPE)) {
            net.minecraft.world.level.block.state.properties.ChestType type = state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE);
            if (type != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                BlockPos otherPos = chestPos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
                BlockState otherState = level.getBlockState(otherPos);
                level.blockEvent(otherPos, otherState.getBlock(), 1, 0);
                level.sendBlockUpdated(otherPos, otherState, otherState, 3);
            }
        }
    }

    // Transfiere los ítems y cierra el cofre al terminar
    default boolean transferAndCloseChest(ServerLevel level, BlockPos chestPos, SimpleContainer inventory, Container chest) {
        boolean transferredSomething = false;
        int end = getBackpackEnd(inventory);
        for (int i = BACKPACK_START; i <= end; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                int originalCount = stack.getCount();
                ItemStack remainder = insertItemIntoChest(chest, stack);
                inventory.setItem(i, remainder);
                if (remainder.getCount() < originalCount) {
                    transferredSomething = true;
                }
            }
        }
        if (transferredSomething) {
            inventory.setChanged();
        }

        // Cierra el cofre con su animación y actualización visual real
        closeChest(level, chestPos);

        return transferredSomething;
    }

    // Helper automático para tickWork (mantiene el cofre abierto mientras el NPC trabaja)
    default boolean handleChestTick(ServerLevel level, BlockPos targetPos, SimpleContainer inventory, int workTimer, NpcEntity npc) {
        BlockPos chestPos = getAdjacentBlock(level, targetPos, Blocks.CHEST, Blocks.TRAPPED_CHEST);
        if (chestPos != null && hasAnyItemInBackpack(inventory)) {
            // Usamos 1 para que abra al comenzar el ciclo y no se bugee cada 40 ticks al reiniciarse
            if (workTimer == 1) {
                openChest(level, chestPos);
            }
            npc.getLookControl().setLookAt(chestPos.getX() + 0.5D, chestPos.getY() + 0.5D, chestPos.getZ() + 0.5D, 30.0F, 30.0F);
            if (workTimer % 20 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
            }
            return true;
        }
        return false;
    }

    // Helper automático para performWork (realiza la transferencia y cierra el cofre)
    default boolean handleChestPerform(ServerLevel level, BlockPos targetPos, SimpleContainer inventory) {
        BlockPos chestPos = getAdjacentBlock(level, targetPos, Blocks.CHEST, Blocks.TRAPPED_CHEST);
        if (chestPos != null) {
            BlockEntity blockEntity = level.getBlockEntity(chestPos);
            if (blockEntity instanceof Container chestContainer) {
                return transferAndCloseChest(level, chestPos, inventory, chestContainer);
            }
        }
        return false;
    }

    default ItemStack insertItemIntoChest(Container chest, ItemStack stack) {
        for (int i = 0; i < chest.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack existing = chest.getItem(i);
            if (existing.isEmpty()) {
                chest.setItem(i, stack.copy());
                stack.setCount(0);
                chest.setChanged();
                break;
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toTransfer = Math.min(space, stack.getCount());
                    existing.grow(toTransfer);
                    stack.shrink(toTransfer);
                    chest.setChanged();
                }
            }
        }
        return stack;
    }

    default BlockPos getAdjacentBlock(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block... targetBlocks) {
        for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
            if (neighbor.distManhattan(pos) <= 1) {
                BlockState neighborState = level.getBlockState(neighbor);
                for (net.minecraft.world.level.block.Block target : targetBlocks) {
                    if (neighborState.is(target)) {
                        return neighbor;
                    }
                }
            }
        }
        return null;
    }
}