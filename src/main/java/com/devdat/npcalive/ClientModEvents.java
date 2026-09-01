package com.devdat.npcalive;

import com.devdat.npcalive.client.model.npc_model;
import com.devdat.npcalive.entity.ModEntities;
import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = NpcAlive.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(npc_model.LAYER_LOCATION, npc_model::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NPC.get(), context ->
                new HumanoidMobRenderer<NpcEntity, HumanoidRenderState, npc_model>(
                        context,
                        new npc_model(context.bakeLayer(npc_model.LAYER_LOCATION)),
                        0.5F
                ) {
                    @Override
                    public HumanoidRenderState createRenderState() {
                        return new HumanoidRenderState();
                    }

                    @Override
                    public Identifier getTextureLocation(HumanoidRenderState state) {
                        return Identifier.fromNamespaceAndPath(NpcAlive.MOD_ID, "textures/entity/npc.png");
                    }
                }
        );
    }
}