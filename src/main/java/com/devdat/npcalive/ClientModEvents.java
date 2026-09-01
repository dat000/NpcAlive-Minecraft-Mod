package com.devdat.npcalive;

import com.devdat.npcalive.client.model.npc_model;
import com.devdat.npcalive.entity.ModEntities;
import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
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
                new MobRenderer<NpcEntity, LivingEntityRenderState, npc_model>(
                        context,
                        new npc_model(context.bakeLayer(npc_model.LAYER_LOCATION)),
                        0.5F
                ) {
                    @Override
                    public LivingEntityRenderState createRenderState() {
                        return new LivingEntityRenderState();
                    }

                    @Override
                    public void extractRenderState(NpcEntity entity, LivingEntityRenderState state, float partialTick) {
                        super.extractRenderState(entity, state, partialTick);
                    }

                    @Override
                    public Identifier getTextureLocation(LivingEntityRenderState state) {
                        return Identifier.fromNamespaceAndPath(NpcAlive.MOD_ID, "textures/entity/npc.png");
                    }
                }
        );
    }
}