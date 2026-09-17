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

public class SpouseDebuggerItem extends Item {
    public SpouseDebuggerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof NpcEntity npc) {
            if (!player.level().isClientSide()) {
                boolean isMarried = !npc.isMarried();
                npc.setMarried(isMarried);
                npc.setRomance(isMarried ? 250 : 0);
                player.sendSystemMessage(Component.literal("§d[Debug] ¿Están casados?: §e" + isMarried).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS; // <-- Ahora siempre devuelve SUCCESS
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}