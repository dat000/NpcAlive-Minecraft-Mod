package com.devdat.npcalive.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
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

        // 1. Slots de Armadura (Ordenados visualmente de arriba a abajo, pero apuntando al índice exacto del NpcEntity)
        // NpcEntity espera: HEAD=1, CHEST=2, LEGS=3, FEET=4
        EquipmentSlot[] armorSlots = new EquipmentSlot[] {
                EquipmentSlot.HEAD,   // Visual Arriba -> Índice 1 en NpcEntity
                EquipmentSlot.CHEST,  // Visual Segundo -> Índice 2 en NpcEntity
                EquipmentSlot.LEGS,   // Visual Tercero -> Índice 3 en NpcEntity
                EquipmentSlot.FEET    // Visual Abajo -> Índice 4 en NpcEntity
        };

        int[] entityIndices = new int[] { 1, 2, 3, 4 };

        for (int i = 0; i < 4; ++i) {
            EquipmentSlot slotType = armorSlots[i];
            int containerIndex = entityIndices[i];

            this.addSlot(new Slot(npcContainer, containerIndex, 8, 14 + (i * 18)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.canEquip(slotType, playerInventory.player);
                }
            });
        }

        // 2. Arma principal y secundaria (Mapeadas a los índices 0 y 5 del NpcEntity)
        this.addSlot(new Slot(npcContainer, 0, 26, 32)); // Mainhand (Mano principal)
        this.addSlot(new Slot(npcContainer, 5, 26, 50)); // Offhand (Mano secundaria)

        // 3. Inventario extra del NPC (Mochila de 2x4, empieza desde el índice 6 en adelante)
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 4; ++j) {
                this.addSlot(new Slot(npcContainer, 6 + j + (i * 4), 80 + (j * 18), 32 + (i * 18)));
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