package com.devdat.npcalive.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NpcMenu extends AbstractContainerMenu {
    private final Container npcContainer;

    public NpcMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(15));
    }

    public NpcMenu(int containerId, Inventory playerInventory, Container npcContainer) {
        super(ModMenuTypes.NPC_MENU.get(), containerId);
        checkContainerSize(npcContainer, 15);
        this.npcContainer = npcContainer;
        npcContainer.startOpen(playerInventory.player);

        // 1. Slots de Armadura (Columna en x = 8)
        for (int i = 0; i < 3; ++i) { // Nota: ajusta si necesitas los 4
            // Dejamos los 4 slots de armadura como estaban:
        }
        for (int i = 0; i < 4; ++i) {
            this.addSlot(new Slot(npcContainer, i, 8, 14 + i * 18));
        }

        // 2. Arma principal y secundaria
        this.addSlot(new Slot(npcContainer, 4, 26, 32));
        this.addSlot(new Slot(npcContainer, 5, 26, 50));

        // 3. Inventario extra del NPC (2 filas de 4 slots, bajado 1 slot y movido 2 a la derecha)
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 4; ++j) {
                this.addSlot(new Slot(npcContainer, 6 + j + i * 4, 86 + j * 18, 32 + i * 18));
            }
        }

        // 4. Inventario principal del Jugador
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 101 + i * 18));
            }
        }

        // 5. Barra rápida (Hotbar) del Jugador
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 159));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < this.npcContainer.getContainerSize()) {
                if (!this.moveItemStackTo(itemstack1, this.npcContainer.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.npcContainer.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.npcContainer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.npcContainer.stopOpen(player);
    }
}