package com.devdat.npcalive.client.gui;

import com.devdat.npcalive.entity.NpcEntity;
import com.devdat.npcalive.network.PerformNpcActionPacket;
import com.devdat.npcalive.network.SetNpcHomePacket;
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
        int boxHeight = 285; // Altura ajustada para alojar todos los botones
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;

        int startY = boxY + 80;
        int btnWidth = 190;
        int btnHeight = 20;
        int btnX = boxX + 15;

        String behaviorText = "Vagar";
        boolean isMarried = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(this.entityId);
            if (entity instanceof NpcEntity npc) {
                behaviorText = switch (npc.getBehavior()) {
                    case WANDER -> "Vagar";
                    case FOLLOW -> "Seguir";
                    case STAY -> "Esperar";
                };
                isMarried = npc.isMarried();
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Saludar"), button -> {
            sendAction("GREET");
        }).bounds(btnX, startY, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Regalar"), button -> {
            sendAction("GIFT");
        }).bounds(btnX, startY + 24, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Romance"), button -> {
            sendAction("ROMANCE");
        }).bounds(btnX, startY + 48, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Ofender"), button -> {
            sendAction("MEAN");
        }).bounds(btnX, startY + 72, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Comerciar"), button -> {
            sendAction("TRANSACTIONS");
        }).bounds(btnX, startY + 96, btnWidth, btnHeight).build());

        // Botón de Establecer Hogar integrado en el menú principal
        this.addRenderableWidget(Button.builder(Component.literal("Establecer Hogar"), button -> {
            if (Minecraft.getInstance().getConnection() != null) {
                Minecraft.getInstance().getConnection().send(new SetNpcHomePacket(this.entityId));
            }
            this.onClose();
        }).bounds(btnX, startY + 120, btnWidth, btnHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Modo: " + behaviorText), button -> {
            sendAction("FOLLOW");
        }).bounds(btnX, startY + 144, btnWidth, btnHeight).build());

        // EL BOTÓN CONDICIONAL: Proponer o Inventario
        Component specialButtonText = isMarried ? Component.literal("Inventario") : Component.literal("Proponer");
        String specialAction = isMarried ? "INVENTORY" : "PROPOSE";

        this.addRenderableWidget(Button.builder(specialButtonText, button -> {
            sendAction(specialAction);
        }).bounds(btnX, startY + 168, btnWidth, btnHeight).build());
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
        int boxHeight = 285;
        int boxX = (this.width - boxWidth) / 2;
        int boxY = (this.height - boxHeight) / 2;

        // 1. Dibujar el fondo
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xCC111111);
        graphics.outline(boxX, boxY, boxWidth, boxHeight, 0xFF555555);

        // 2. Obtener los valores actuales desde la entidad del cliente
        int friendshipValue = 0;
        int romanceValue = 0;
        String dialogueKey = "dialog.npcalive.mood.neutral";

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(this.entityId);
            if (entity instanceof NpcEntity npc) {
                friendshipValue = npc.getFriendship();
                romanceValue = npc.getRomance();
                dialogueKey = npc.getDialogueKey();
            }
        }

        // 3. Textos informativos
        graphics.text(this.font, Component.translatable(dialogueKey), boxX + 15, boxY + 15, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("dialog.npcalive.gender", this.genderStr), boxX + 15, boxY + 30, 0xFFCCCCCC, true);
        graphics.text(this.font, Component.translatable("dialog.npcalive.friendship", friendshipValue), boxX + 15, boxY + 45, 0xFF55FF55, true);
        graphics.text(this.font, Component.translatable("dialog.npcalive.romance", romanceValue), boxX + 15, boxY + 60, 0xFFFF55FF, true);

        // 4. Renderizar widgets encima
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}