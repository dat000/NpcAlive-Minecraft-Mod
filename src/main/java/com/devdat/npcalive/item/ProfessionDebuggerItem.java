package com.devdat.npcalive.item;

import com.devdat.npcalive.entity.NpcEntity;
import com.devdat.npcalive.entity.NpcProfession;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ProfessionDebuggerItem extends Item {
    public ProfessionDebuggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand hand) {
        // Verificamos si la entidad clicada es un NPC de nuestro mod
        if (interactionTarget instanceof NpcEntity npc) {
            if (!player.level().isClientSide()) {
                // Ciclar entre las profesiones disponibles
                NpcProfession[] professions = NpcProfession.values();
                NpcProfession current = npc.getProfession(); // Asegúrate de que este metodo exista en tu NpcEntity

                int nextIndex = 0;
                for (int i = 0; i < professions.length; i++) {
                    if (professions[i] == current) {
                        nextIndex = (i + 1) % professions.length;
                        break;
                    }
                }

                NpcProfession nextProfession = professions[nextIndex];
                npc.setProfession(nextProfession); // Asegúrate de tener un setter para la profesión en tu NpcEntity

                // Mensaje de feedback en el chat del desarrollador
                player.sendSystemMessage(Component.literal("§a[Debug] Profesión cambiada a: §e" + nextProfession.name())
                        .withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Esto activa el brillo de encantamiento por defecto
    }

}