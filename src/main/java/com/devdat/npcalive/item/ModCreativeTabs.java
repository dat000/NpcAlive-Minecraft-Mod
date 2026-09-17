package com.devdat.npcalive.item;

import com.devdat.npcalive.NpcAlive;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NpcAlive.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> NPC_ALIVE_TAB = TABS.register("npcalive_tab",
            () -> CreativeModeTab.builder()
                    //titulo
                    .title(Component.translatable("itemGroup.npcalive.tab"))
                    //icono
                    .icon(() -> new ItemStack(Items.EMERALD))
                    //items dentro
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.PROFESSION_DEBUGGER.get());
                        output.accept(ModItems.FAMILY_DEBUGGER.get());    // Nuevo
                        output.accept(ModItems.SPOUSE_DEBUGGER.get());    // Nuevo
                        output.accept(ModItems.FRIENDSHIP_DEBUGGER.get());// Nuevo
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}