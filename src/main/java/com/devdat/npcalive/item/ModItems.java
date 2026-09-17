package com.devdat.npcalive.item;

import com.devdat.npcalive.NpcAlive;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(NpcAlive.MOD_ID);

    // Fíjate en el tercer parámetro: usamos una lambda 'p -> p.stacksTo(1)'
    public static final DeferredItem<Item> PROFESSION_DEBUGGER = ITEMS.registerItem(
            "profession_debugger",
            ProfessionDebuggerItem::new,
            p -> p.stacksTo(1)
    );

    public static final DeferredItem<Item> FAMILY_DEBUGGER = ITEMS.registerItem(
            "family_debugger", FamilyDebuggerItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> SPOUSE_DEBUGGER = ITEMS.registerItem(
            "spouse_debugger", SpouseDebuggerItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<Item> FRIENDSHIP_DEBUGGER = ITEMS.registerItem(
            "friendship_debugger", FriendshipDebuggerItem::new, p -> p.stacksTo(1));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}