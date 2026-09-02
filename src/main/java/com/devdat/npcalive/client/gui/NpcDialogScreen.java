package com.devdat.npcalive.client.gui;

import com.devdat.npcalive.network.PerformNpcActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NpcDialogScreen extends Screen {
    private final int entityId;
    private final String npcTitle;
    private final String genderStr;

    public NpcDialogScreen(int entityId, String npcTitle, String genderStr) {
        super(Component.literal("Diálogo de NPC"));
        this.entityId = entityId;
        this.npcTitle = npcTitle;
        this.genderStr = genderStr;
    }

    @Override
    protected void init() {
        super.init();

        int boxWidth = 220;
        int boxHeight = 175; // Aumentamos un poco la altura para que entren más botones
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;

        int startY = boxY + 60;
        int btnWidth = 190;
        int btnHeight = 20;
        int btnX = boxX + 15;

        // 1. Botón Saludar
        this.addRenderableWidget(Button.builder(Component.literal("Saludar"), button -> {
            sendAction("GREET");
        }).bounds(btnX, startY, btnWidth, btnHeight).build());

        // 2. Botón Romance
        this.addRenderableWidget(Button.builder(Component.literal("Romance"), button -> {
            sendAction("ROMANCE");
        }).bounds(btnX, startY + 24, btnWidth, btnHeight).build());

        // 3. Botón Insultar / Malo
        this.addRenderableWidget(Button.builder(Component.literal("Ofender"), button -> {
            sendAction("MEAN");
        }).bounds(btnX, startY + 48, btnWidth, btnHeight).build());

        // 4. Botón Transacciones / Tienda
        this.addRenderableWidget(Button.builder(Component.literal("Comerciar"), button -> {
            sendAction("TRANSACTIONS");
        }).bounds(btnX, startY + 72, btnWidth, btnHeight).build());
    }

    private void sendAction(String actionName) {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().send(new PerformNpcActionPacket(this.entityId, actionName));
        }
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int boxWidth = 220;
        int boxHeight = 175;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;

        // 1. DIBUJAR EL FONDO PRIMERO para que los botones queden ENCIMA
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xCC111111);
        graphics.outline(boxX, boxY, boxWidth, boxHeight, 0xFF555555);

        // 2. Textos descriptivos
        graphics.text(this.font, Component.literal("¡Hola! Me llamo " + this.npcTitle), boxX + 15, boxY + 15, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.literal("Género: " + this.genderStr), boxX + 15, boxY + 32, 0xFFCCCCCC, true);

        // 3. Renderizar widgets al final para que estén en la capa superior
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}