package com.devdat.npcalive.client.renderer;

import com.devdat.npcalive.NpcAlive;
import com.devdat.npcalive.client.model.npc_model;
import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, NpcRenderState, npc_model> {

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new npc_model(context.bakeLayer(npc_model.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(NpcEntity entity, NpcRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.variant = entity.getNpcVariant();
        state.gender = entity.getNpcGender();
    }

    @Override
    public Identifier getTextureLocation(NpcRenderState state) {
        String genderStr = (state.gender == NpcEntity.NpcGender.MALE) ? "male" : "female";
        return Identifier.fromNamespaceAndPath(
                NpcAlive.MOD_ID,
                "textures/entity/npc_" + genderStr + "_" + state.variant + ".png"
        );
    }
}