package com.devdat.npcalive.entity;

import com.devdat.npcalive.NpcAlive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, NpcAlive.MOD_ID);

    public static final Supplier<EntityType<NpcEntity>> NPC = ENTITIES.register(
            "npc",
            () -> EntityType.Builder.of(NpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(NpcAlive.MOD_ID, "npc")))
    );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}