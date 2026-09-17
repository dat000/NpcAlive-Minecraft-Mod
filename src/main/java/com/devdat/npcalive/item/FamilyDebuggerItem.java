package com.devdat.npcalive.item;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class FamilyDebuggerItem extends Item {
    public FamilyDebuggerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof NpcEntity npc) {
            if (!player.level().isClientSide()) {
                boolean newValue = !npc.isFamily();
                npc.setFamily(newValue);
                player.sendSystemMessage(Component.literal("§b[Debug] ¿Es Familia?: §e" + newValue).withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.SUCCESS; // <-- Ahora siempre devuelve SUCCESS
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}