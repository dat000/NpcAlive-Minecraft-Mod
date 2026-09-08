package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;

import java.util.ArrayList;
import java.util.List;

public class ButcherProfession implements ProfessionLogic {

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        // Prioridad absoluta: Guardar ítems en el cofre
        if ((endingFirst || endingSecond || hasEnoughItemsToStore(inventory)) && hasAnyItemInBackpack(inventory)) {
            BlockPos chestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (chestPos != null) {
                return chestPos.north().immutable();
            }
        }

        // Detecta hacia dónde apunta el frente del ahumador principal para pararse enfrente
        net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(workPos);
        net.minecraft.core.Direction facing = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                : net.minecraft.core.Direction.NORTH;

        BlockPos frontPos = workPos.relative(facing).immutable();

        List<BlockPos> availableStations = new ArrayList<>();
        availableStations.add(frontPos); // Posición dinámica al frente

        // Si hay un ahumador adyacente extra, también calcula su frente
        BlockPos smokerPos = getAdjacentBlock(serverLevel, workPos, Blocks.SMOKER);
        if (smokerPos != null) {
            net.minecraft.world.level.block.state.BlockState smokerState = serverLevel.getBlockState(smokerPos);
            net.minecraft.core.Direction smokerFacing = smokerState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    ? smokerState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    : net.minecraft.core.Direction.NORTH;
            availableStations.add(smokerPos.relative(smokerFacing).immutable());
        }

        long timeSlot = serverLevel.getGameTime() / 200;
        int index = (int) Math.abs((npc.getUUID().getLeastSignificantBits() + timeSlot) % availableStations.size());

        return availableStations.get(index);
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        BlockPos smoker = getAdjacentBlock(serverLevel, targetPos, Blocks.SMOKER);

        // Efectos visuales si está trabajando en un Ahumador o en la mesa principal (Ahumador de la aldea)
        if (smoker != null || targetPos.equals(workPos)) {
            BlockPos effectPos = smoker != null ? smoker : workPos;

            if (workTimer % 20 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        effectPos.getX() + 0.5D, effectPos.getY() + 0.8D, effectPos.getZ() + 0.5D,
                        1, 0.05D, 0.05D, 0.05D, 0.02D);
                serverLevel.playSound(null, effectPos, SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        SimpleContainer inventory = npc.getInventory();

        // 1. Verificación de cofre para vaciar inventario
        BlockPos chestPos = getAdjacentBlock(serverLevel, targetPos, Blocks.CHEST, Blocks.TRAPPED_CHEST);
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

        // 2. Trabajo en su estación para conseguir recompensas
        BlockPos smoker = getAdjacentBlock(serverLevel, targetPos, Blocks.SMOKER);
        if (smoker != null || targetPos.equals(workPos)) {
            BlockPos effectPos = smoker != null ? smoker : workPos;
            npc.swing(InteractionHand.MAIN_HAND, true);

            // Nerfeo: Solo otorga recompensa cada 200 ticks (10 segundos) de trabajo continuo
            if (serverLevel.getGameTime() % 200 == 0) {
                giveButcherReward(npc, serverLevel, effectPos);
            }
        }
    }

    private void giveButcherReward(NpcEntity npc, ServerLevel serverLevel, BlockPos pos) {
        float roll = serverLevel.getRandom().nextFloat();
        ItemStack reward;

        // Distribución balanceada de comida
        if (roll < 0.60F) { // 60%
            reward = new ItemStack(Items.BEEF, 1);
        } else if (roll < 0.90F) { // 30%
            reward = new ItemStack(Items.PORKCHOP, 1);
        } else if (roll < 0.98F) { // 8%
            reward = new ItemStack(Items.COOKED_BEEF, 1);
        } else { // 2% Excepcional
            reward = new ItemStack(Items.RABBIT_STEW, 1);
        }

        npc.addItemToBackpack(reward);
        // Sonido de cortar carne (usando sonido de ataque a entidad carnosa o simplemente comer)
        serverLevel.playSound(null, pos, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.8F, 0.9F);
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return true;
    }

    @Override
    public ItemStack getWorkTool() {
        // Le damos un hacha para que parezca un cuchillo de carnicero
        return new ItemStack(Items.IRON_AXE);
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