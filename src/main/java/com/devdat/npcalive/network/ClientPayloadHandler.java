package com.devdat.npcalive.network;

import com.devdat.npcalive.client.gui.NpcDialogScreen;
import net.minecraft.client.Minecraft;

public class ClientPayloadHandler {
    public static void handleOpenNpcGui(OpenNpcGuiPacket payload) {
        Minecraft.getInstance().setScreenAndShow(
                new NpcDialogScreen(payload.entityId(), payload.npcTitle(), payload.genderStr())
        );
    }
}