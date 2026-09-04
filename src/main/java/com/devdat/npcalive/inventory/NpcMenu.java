package com.devdat.npcalive.inventory;

import com.devdat.npcalive.entity.NpcEntity;
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
    private final NpcEntity npc;

    // Constructor para el cliente (lee el ID de la entidad enviado desde el servidor)
    public NpcMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getEntityFromBuffer(playerInventory, extraData));
    }

    private static NpcEntity getEntityFromBuffer(Inventory playerInventory, FriendlyByteBuf extraData) {
        int entityId = extraData.readInt();
        net.minecraft.world.entity.Entity entity = playerInventory.player.level().getEntity(entityId);
        return entity instanceof NpcEntity foundNpc ? foundNpc : null;
    }

    // Constructor principal que recibe la entidad NPC
    public NpcMenu(int containerId, Inventory playerInventory, NpcEntity npc) {
        super(ModMenuTypes.NPC_MENU.get(), containerId);
        this.npc = npc;
        this.npcContainer = npc != null ? npc.getInventory() : new SimpleContainer(15);

        checkContainerSize(this.npcContainer, 15);
        this.npcContainer.startOpen(playerInventory.player);

        // 1. Slots de Armadura (HEAD=1, CHEST=2, LEGS=3, FEET=4)
        EquipmentSlot[] armorSlots = new EquipmentSlot[] {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
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

        // 2. Arma principal y secundaria (Índices 0 y 5)
        this.addSlot(new Slot(npcContainer, 0, 26, 32)); // Mainhand
        this.addSlot(new Slot(npcContainer, 5, 26, 50)); // Offhand

        // 3. Inventario extra del NPC (Mochila de 2x4, desde el índice 6)
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
        return this.npc != null && this.npc.isAlive() && this.npc.distanceToSqr(player) < 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.npcContainer.stopOpen(player);
    }

    public NpcEntity getNpc() {
        return this.npc;
    }
}