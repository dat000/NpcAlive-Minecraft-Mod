package com.devdat.npcalive;

import com.devdat.npcalive.entity.ModEntities;
import com.devdat.npcalive.entity.NpcEntity;
import com.devdat.npcalive.inventory.ModMenuTypes;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;

@Mod(NpcAlive.MOD_ID)
public class NpcAlive {
    public static final String MOD_ID = "npcalive";
    private static final Logger LOGGER = LogUtils.getLogger();

    public NpcAlive(IEventBus modEventBus, ModContainer modContainer) {
        ModEntities.register(modEventBus);
        modEventBus.addListener(this::addEntityAttributes);
        ModMenuTypes.register(modEventBus);
        LOGGER.info("Iniciando Npc Alive...");
    }

    private void addEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NPC.get(), NpcEntity.createAttributes().build());
    }

}