package com.devdat.npcalive.client.gui;

import com.devdat.npcalive.entity.NpcEntity;
import com.devdat.npcalive.network.PerformNpcActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

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
        int boxHeight = 220;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;

        int startY = boxY + 75;
        int btnWidth = 190;
        int btnHeight = 20;
        int btnX = boxX + 15;

        // Obtener el estado actual del comportamiento desde la entidad del cliente
        String behaviorText = "Vagar";
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(this.entityId);
            if (entity instanceof NpcEntity npc) {
                behaviorText = switch (npc.getBehavior()) {
                    case WANDER -> "Vagar";
                    case FOLLOW -> "Seguir";
                    case STAY -> "Esperar";
                };
            }
        }

        // Botones de acción
        this.addRenderableWidget(Button.builder(Component.literal("Modo: " + behaviorText), button -> {
            sendAction("FOLLOW");
        }).bounds(btnX, startY + 96, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Saludar"), button -> {
            sendAction("GREET");
        }).bounds(btnX, startY, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Romance"), button -> {
            sendAction("ROMANCE");
        }).bounds(btnX, startY + 24, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Ofender"), button -> {
            sendAction("MEAN");
        }).bounds(btnX, startY + 48, btnWidth, btnHeight).build());

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
        int boxHeight = 185;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;

        // 1. Dibujar el fondo
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xCC111111);
        graphics.outline(boxX, boxY, boxWidth, boxHeight, 0xFF555555);

        // 2. Obtener el valor actual de amistad desde la entidad del cliente
        int friendshipValue = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(this.entityId);
            if (entity instanceof NpcEntity npc) {
                friendshipValue = npc.getFriendship();
            }
        }

        // 3. Textos informativos con la variable integrada
        graphics.text(this.font, Component.literal("¡Hola! Me llamo " + this.npcTitle), boxX + 15, boxY + 15, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.literal("Género: " + this.genderStr), boxX + 15, boxY + 32, 0xFFCCCCCC, true);
        graphics.text(this.font, Component.literal("Amistad: " + friendshipValue), boxX + 15, boxY + 49, 0xFF55FF55, true);

        // 4. Renderizar widgets encima
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}