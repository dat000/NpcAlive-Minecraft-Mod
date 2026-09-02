package com.devdat.npcalive.client.gui;

import com.devdat.npcalive.inventory.NpcMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class NpcScreen extends AbstractContainerScreen<NpcMenu> {

    // Panel extendido para cubrir tanto el inventario del NPC como el del jugador
    private final int panelHeight = 181;

    public NpcScreen(NpcMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.inventoryLabelY = 90; // Etiqueta del inventario del jugador bien posicionada
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 1. Panel de fondo general que abarca toda la interfaz
        graphics.fill(x, y, x + this.imageWidth, y + panelHeight, 0xCC111111);
        graphics.outline(x, y, this.imageWidth, panelHeight, 0xFF555555);

        // 2. Fondos de los slots del NPC (Armadura, armas y almacenamiento)
        for (int i = 0; i < 4; ++i) {
            drawSlotBackground(graphics, x + 8, y + 14 + (i * 18));
        }

        drawSlotBackground(graphics, x + 26, y + 32);
        drawSlotBackground(graphics, x + 26, y + 50);

        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 4; ++j) {
                drawSlotBackground(graphics, x + 86 + (j * 18), y + 32 + (i * 18));
            }
        }

        // 3. Fondos de los slots del inventario principal del Jugador (3x9)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                drawSlotBackground(graphics, x + 8 + (j * 18), y + 101 + (i * 18));
            }
        }

        // 4. Fondos de los slots de la barra rápida (Hotbar)
        for (int k = 0; k < 9; ++k) {
            drawSlotBackground(graphics, x + 8 + (k * 18), y + 159);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawSlotBackground(GuiGraphicsExtractor graphics, int posX, int posY) {
        graphics.fill(posX, posY, posX + 16, posY + 16, 0xFF8B8B8B);
        graphics.fill(posX + 1, posY + 1, posX + 15, posY + 15, 0xFF373737);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}