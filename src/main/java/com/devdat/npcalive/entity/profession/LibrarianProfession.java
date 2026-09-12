package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LibrarianProfession implements ProfessionLogic {

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        // 1. Prioridad: Guardar en el cofre si termina el turno o la mochila está llena
        if ((endingFirst || endingSecond || hasEnoughItemsToStore(inventory)) && hasAnyItemInBackpack(inventory)) {
            BlockPos chestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (chestPos != null) {
                return chestPos.north().immutable();
            }
        }

        // 2. Posicionamiento seguro garantizando que haya espacio libre y suelo sólido abajo
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos sidePos = workPos.relative(dir);
            if (!serverLevel.getBlockState(sidePos).blocksMotion() && serverLevel.getBlockState(sidePos.below()).blocksMotion()) {
                return sidePos.immutable();
            }
        }

        // Fallback seguro por si los 4 lados están ocupados
        return workPos.north().immutable();
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        // Sostiene un libro en la mano para leer
        npc.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOOK));

        // Fija la mirada en el atril
        npc.getLookControl().setLookAt(workPos.getX() + 0.5D, workPos.getY() + 1.0D, workPos.getZ() + 0.5D, 30.0F, 30.0F);

        // Efectos visuales de estudio (Letras mágicas) y cambio de página
        if (workTimer % 15 == 0) {
            // Partículas de letras volando desde el atril
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    workPos.getX() + 0.5D, workPos.getY() + 1.2D, workPos.getZ() + 0.5D,
                    3, 0.2, 0.2, 0.2, 0.05);
        }

        if (workTimer % 80 == 0) {
            // Animación de pasar de página
            npc.swing(InteractionHand.MAIN_HAND, true);
            serverLevel.playSound(null, workPos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.5F, serverLevel.getRandom().nextFloat() * 0.2F + 0.9F);
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        SimpleContainer inventory = npc.getInventory();

        // 1. Lógica de guardado en el cofre si hay uno adyacente y la mochila lo necesita
        BlockPos chestPos = getAdjacentBlock(serverLevel, workPos, Blocks.CHEST, Blocks.TRAPPED_CHEST);
        if (chestPos != null) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(chestPos);
            if (blockEntity instanceof Container chestContainer) {
                if (transferBackpackToChest(inventory, chestContainer)) {
                    serverLevel.blockEvent(chestPos, serverLevel.getBlockState(chestPos).getBlock(), 1, 0);
                    serverLevel.playSound(null, chestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
                    return;
                }
            }
        }

        // 2. Recompensas de estudio (Se ejecutan cada vez que el ciclo de 40 ticks termina)
        giveLibrarianReward(npc, serverLevel, workPos);
    }

    private void giveLibrarianReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        if (roll < 0.60F) {
            reward = new ItemStack(Items.PAPER, serverLevel.getRandom().nextInt(2) + 1); // 1-2 papeles
        } else if (roll < 0.85F) {
            reward = new ItemStack(Items.BOOK, 1);
        } else if (roll < 0.95F) {
            reward = new ItemStack(Items.NAME_TAG, 1);
        } else {
            reward = new ItemStack(Items.COMPASS, 1);
        }

        npc.addItemToBackpack(reward);

        // Sonido sutil cuando el NPC "crea" o "encuentra" conocimiento
        serverLevel.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.2F, 2.0F);
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return true;
    }

    @Override
    public ItemStack getWorkTool() {
        return new ItemStack(Items.BOOK);
    }

    @Override
    public long getWorkStartTime() {
        return 2000L;
    }

    @Override
    public long getWorkEndTime() {
        return 6000L;
    }

    @Override
    public long getSecondWorkStartTime() {
        return 8000L;
    }

    @Override
    public long getSecondWorkEndTime() {
        return 12000L;
    }
}