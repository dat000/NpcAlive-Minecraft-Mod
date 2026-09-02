package com.devdat.npcalive.client.renderer;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class NpcRenderState extends HumanoidRenderState {
    public int variant = 0;
    public NpcEntity.NpcGender gender = NpcEntity.NpcGender.MALE;
}