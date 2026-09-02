package com.devdat.npcalive.inventory;

import com.devdat.npcalive.NpcAlive;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, NpcAlive.MOD_ID);

    public static final Supplier<MenuType<NpcMenu>> NPC_MENU = MENUS.register("npc_menu",
            () -> IMenuTypeExtension.create(NpcMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}