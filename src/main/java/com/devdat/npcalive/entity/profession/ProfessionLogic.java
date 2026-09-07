package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
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

    default boolean transferBackpackToChest(SimpleContainer inventory, Container chest) {
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
        return transferredSomething;
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