package com.devdat.npcalive;

import com.devdat.npcalive.client.model.npc_model;
import com.devdat.npcalive.client.renderer.NpcRenderer;
import com.devdat.npcalive.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = NpcAlive.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(npc_model.LAYER_LOCATION, npc_model::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(com.devdat.npcalive.inventory.ModMenuTypes.NPC_MENU.get(), com.devdat.npcalive.client.gui.NpcScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NPC.get(), NpcRenderer::new);
    }
}