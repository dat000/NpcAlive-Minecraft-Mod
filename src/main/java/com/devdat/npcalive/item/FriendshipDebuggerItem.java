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

public class FriendshipDebuggerItem extends Item {
    public FriendshipDebuggerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof NpcEntity npc) {
            if (!player.level().isClientSide()) {
                int currentFriendship = npc.getFriendship();
                int newFriendship = (currentFriendship == 250) ? 0 : 250;
                npc.setFriendship(newFriendship);
                player.sendSystemMessage(Component.literal("§a[Debug] Nivel de Amistad: §e" + newFriendship).withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.SUCCESS; // <-- Ahora siempre devuelve SUCCESS
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}