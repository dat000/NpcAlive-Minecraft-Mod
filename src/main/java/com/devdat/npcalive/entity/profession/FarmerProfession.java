package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;

import java.util.ArrayList;
import java.util.List;

public class FarmerProfession implements ProfessionLogic {

    private static final int BACKPACK_START = 6;
    private static final int BACKPACK_END = 13;
    private static final int WORK_RADIUS = 8;

    @Override
    public BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return workPos;

        SimpleContainer inventory = npc.getInventory();
        long timeOfDay = serverLevel.getDefaultClockTime() % 24000;

        // VENTANA DE CIERRE: Faltando 400 ticks para acabar el turno, si tiene ítems, va al cofre obligatoriamente
        boolean endingFirst = timeOfDay >= (getWorkEndTime() - 400) && timeOfDay < getWorkEndTime();
        boolean endingSecond = getSecondWorkEndTime() != -1 && timeOfDay >= (getSecondWorkEndTime() - 400) && timeOfDay < getSecondWorkEndTime();

        if ((endingFirst || endingSecond) && hasAnyItemInBackpack(inventory)) {
            BlockPos stationChestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (stationChestPos != null) {
                return stationChestPos.north().immutable(); // O la posición adyacente que use tu lógica de cofre
            }
        }

        // 1. Si el inventario tiene suficientes ítems: Buscar el cofre de la mesa de trabajo
        if (hasEnoughItemsToStore(inventory)) {
            BlockPos stationChestPos = findChestAdjacentToWorkstation(serverLevel, workPos);
            if (stationChestPos != null) {
                for (BlockPos neighbor : BlockPos.betweenClosed(stationChestPos.offset(-1, 0, -1), stationChestPos.offset(1, 0, 1))) {
                    if (neighbor.distManhattan(stationChestPos) == 1) {
                        BlockState neighborState = serverLevel.getBlockState(neighbor);
                        BlockState belowState = serverLevel.getBlockState(neighbor.below());
                        if ((neighborState.isAir() || neighborState.canBeReplaced()) && !belowState.isAir()) {
                            return neighbor.immutable();
                        }
                    }
                }
                return stationChestPos.north().immutable();
            }
        }

        // 2. Cultivos maduros
        for (BlockPos pos : BlockPos.betweenClosed(
                workPos.offset(-WORK_RADIUS, -1, -WORK_RADIUS),
                workPos.offset(WORK_RADIUS, 2, WORK_RADIUS)
        )) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock cropBlock && !(state.getBlock() instanceof StemBlock)) {
                if (cropBlock.isMaxAge(state)) {
                    return pos.immutable();
                }
            }
        }

        // 3. Calabazas o melones
        for (BlockPos pos : BlockPos.betweenClosed(
                workPos.offset(-WORK_RADIUS, -1, -WORK_RADIUS),
                workPos.offset(WORK_RADIUS, 2, WORK_RADIUS)
        )) {
            BlockState state = serverLevel.getBlockState(pos);
            if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
                BlockPos bestNeighbor = null;
                for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
                    if (neighbor.distManhattan(pos) == 1) {
                        BlockState neighborState = serverLevel.getBlockState(neighbor);
                        BlockState belowState = serverLevel.getBlockState(neighbor.below());
                        if ((neighborState.isAir() || neighborState.getBlock() instanceof StemBlock || neighborState.canBeReplaced())
                                && !belowState.isAir() && !belowState.is(Blocks.WATER)) {
                            bestNeighbor = neighbor.immutable();
                            break;
                        }
                    }
                }
                if (bestNeighbor != null) return bestNeighbor;
                return pos.south().immutable();
            }
        }

        // 4. Compostador (Mesa de trabajo) solo si tiene semillas
        if (hasCompostableItems(inventory)) {
            if (serverLevel.getBlockState(workPos).is(Blocks.COMPOSTER)) {
                for (BlockPos neighbor : BlockPos.betweenClosed(workPos.offset(-1, 0, -1), workPos.offset(1, 0, 1))) {
                    if (neighbor.distManhattan(workPos) == 1) {
                        BlockState neighborState = serverLevel.getBlockState(neighbor);
                        BlockState belowState = serverLevel.getBlockState(neighbor.below());
                        if ((neighborState.isAir() || neighborState.canBeReplaced()) && !belowState.isAir()) {
                            return neighbor.immutable();
                        }
                    }
                }
                return workPos.north().immutable();
            }
        }

        // 5. PATRULLAJE TRANQUILO SOBRE CULTIVOS: Recorre de forma fluida las zonas de plantación
        long timeSlot = serverLevel.getGameTime() / 120; // Cambia de posición cada 6 segundos aprox
        List<BlockPos> farmSpots = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                workPos.offset(-WORK_RADIUS, 0, -WORK_RADIUS),
                workPos.offset(WORK_RADIUS, 0, WORK_RADIUS)
        )) {
            if (serverLevel.getBlockState(pos.below()).is(Blocks.FARMLAND) || serverLevel.getBlockState(pos).getBlock() instanceof CropBlock) {
                if (serverLevel.getBlockState(pos).isAir() || serverLevel.getBlockState(pos).getBlock() instanceof CropBlock) {
                    farmSpots.add(pos.immutable());
                }
            }
        }

        if (!farmSpots.isEmpty()) {
            int index = (int) Math.abs((npc.getUUID().getLeastSignificantBits() + timeSlot) % farmSpots.size());
            return farmSpots.get(index);
        }

        return workPos;
    }

    @Override
    public void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;
        SimpleContainer inventory = npc.getInventory();

        BlockPos targetPumpkinOrMelon = getAdjacentBlock(serverLevel, targetPos, Blocks.PUMPKIN, Blocks.MELON);
        if (targetPumpkinOrMelon != null) {
            int progress = Math.min(9, (workTimer * 10) / 40);
            serverLevel.destroyBlockProgress(npc.getId(), targetPumpkinOrMelon, progress);
            if (workTimer % 10 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.playSound(null, targetPumpkinOrMelon, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.5F, 0.8F);
            }
            return;
        }

        BlockPos targetChest = getAdjacentBlock(serverLevel, targetPos, Blocks.CHEST, Blocks.TRAPPED_CHEST);
        if (targetChest != null && hasAnyItemInBackpack(inventory)) {
            if (workTimer == 0 || workTimer % 20 == 0) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                serverLevel.blockEvent(targetChest, serverLevel.getBlockState(targetChest).getBlock(), 1, 1); // <-- Animación de abrir
                serverLevel.playSound(null, targetChest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
            return;
        }

        BlockPos targetComposter = getAdjacentBlock(serverLevel, targetPos, Blocks.COMPOSTER);
        if (targetComposter != null && workTimer % 15 == 0) {
            BlockState composterState = serverLevel.getBlockState(targetComposter);
            boolean isFull = composterState.hasProperty(ComposterBlock.LEVEL) && composterState.getValue(ComposterBlock.LEVEL) == 8;

            if (hasCompostableItems(inventory) || isFull) {
                npc.swing(InteractionHand.MAIN_HAND, true);
                double px = targetComposter.getX() + 0.5D;
                double py = targetComposter.getY() + 0.8D;
                double pz = targetComposter.getZ() + 0.5D;
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 2, 0.2, 0.2, 0.2, 0.05);
                serverLevel.playSound(null, targetComposter, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.5F, 1.0F);
            }
        }
    }

    @Override
    public void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos) {
        if (!(npc.level() instanceof ServerLevel serverLevel)) return;

        npc.swing(InteractionHand.MAIN_HAND, true);
        SimpleContainer inventory = npc.getInventory();

        // 1. Cofre
        BlockPos chestPos = getAdjacentBlock(serverLevel, targetPos, Blocks.CHEST, Blocks.TRAPPED_CHEST);
        if (chestPos != null) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(chestPos);
            if (blockEntity instanceof Container chestContainer) {
                if (transferBackpackToChest(inventory, chestContainer)) {
                    serverLevel.blockEvent(chestPos, serverLevel.getBlockState(chestPos).getBlock(), 1, 0); // <-- Animación de cerrar
                    serverLevel.playSound(null, chestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
                    return;
                }
            }
        }

        // 2. Calabaza o melón
        BlockPos pumpkinOrMelonPos = getAdjacentBlock(serverLevel, targetPos, Blocks.PUMPKIN, Blocks.MELON);
        if (pumpkinOrMelonPos != null) {
            serverLevel.destroyBlockProgress(npc.getId(), pumpkinOrMelonPos, -1);
            BlockState harvestState = serverLevel.getBlockState(pumpkinOrMelonPos);
            serverLevel.destroyBlock(pumpkinOrMelonPos, false, npc);
            giveHarvestDrops(npc, harvestState, serverLevel);
            serverLevel.playSound(null, pumpkinOrMelonPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
            return;
        }

        // 3. Compostador
        BlockPos composterPos = getAdjacentBlock(serverLevel, targetPos, Blocks.COMPOSTER);
        if (composterPos != null) {
            BlockState composterState = serverLevel.getBlockState(composterPos);
            if (composterState.hasProperty(ComposterBlock.LEVEL)) {
                int level = composterState.getValue(ComposterBlock.LEVEL);

                if (level == 8) {
                    serverLevel.setBlock(composterPos, composterState.setValue(ComposterBlock.LEVEL, 0), 3);
                    serverLevel.playSound(null, composterPos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    addItemToBackpack(inventory, new ItemStack(Items.BONE_MEAL, 1));
                    return;
                } else if (level < 7 && hasCompostableItems(inventory)) {
                    if (consumeCompostableItem(inventory)) {
                        BlockState nextState = composterState.setValue(ComposterBlock.LEVEL, level + 1);
                        serverLevel.setBlock(composterPos, nextState, 3);
                        serverLevel.playSound(null, composterPos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                        if (level + 1 == 7 && serverLevel.getRandom().nextFloat() < 0.6F) {
                            serverLevel.setBlock(composterPos, nextState.setValue(ComposterBlock.LEVEL, 8), 3);
                        }
                        return;
                    }
                }
            }
        }

        // 4. Cultivo tradicional
        BlockState targetState = serverLevel.getBlockState(targetPos);
        Block block = targetState.getBlock();
        if (block instanceof CropBlock cropBlock && !(block instanceof StemBlock)) {
            serverLevel.destroyBlock(targetPos, false, npc);
            giveHarvestDrops(npc, targetState, serverLevel);

            BlockPos below = targetPos.below();
            if (serverLevel.getBlockState(below).is(Blocks.FARMLAND)) {
                Item requiredSeed = getSeedItemForCrop(cropBlock);
                if (requiredSeed != null && consumeItemFromBackpack(inventory, requiredSeed)) {
                    serverLevel.setBlock(targetPos, cropBlock.defaultBlockState(), 3);
                    serverLevel.playSound(null, targetPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            return;
        }

        // 5. ESTADO DE PASEO SOBRE CULTIVOS: Cuando llega al punto de patrulla, inspecciona con calma
        if (targetPos.distManhattan(workPos) > WORK_RADIUS) {
            return;
        }

        double px = targetPos.getX() + 0.5D;
        double py = targetPos.getY() + 0.8D;
        double pz = targetPos.getZ() + 0.5D;
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 1, 0.2, 0.2, 0.2, 0.02);
    }

    private boolean isBackpackFull(SimpleContainer inventory) {
        if (inventory == null) return false;
        for (int i = BACKPACK_START; i <= BACKPACK_END; i++) {
            if (inventory.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAnyItemInBackpack(SimpleContainer inventory) {
        if (inventory == null) return false;
        for (int i = BACKPACK_START; i <= BACKPACK_END; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private BlockPos findChestAdjacentToWorkstation(ServerLevel level, BlockPos workPos) {
        for (BlockPos neighbor : BlockPos.betweenClosed(workPos.offset(-1, 0, -1), workPos.offset(1, 1, 1))) {
            if (neighbor.distManhattan(workPos) <= 1) {
                if (level.getBlockState(neighbor).is(Blocks.CHEST) || level.getBlockState(neighbor).is(Blocks.TRAPPED_CHEST)) {
                    return neighbor.immutable();
                }
            }
        }
        return null;
    }

    private boolean transferBackpackToChest(SimpleContainer inventory, Container chest) {
        boolean transferredSomething = false;
        for (int i = BACKPACK_START; i <= BACKPACK_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                int originalCount = stack.getCount();
                ItemStack remainder = insertItemIntoChest(chest, stack);
                inventory.setItem(i, remainder);
                if (remainder.getCount() < originalCount) {
                    transferredSomething = true;
                }
            }
        }
        if (transferredSomething) {
            inventory.setChanged();
        }
        return transferredSomething;
    }

    private ItemStack insertItemIntoChest(Container chest, ItemStack stack) {
        for (int i = 0; i < chest.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack existing = chest.getItem(i);
            if (existing.isEmpty()) {
                chest.setItem(i, stack.copy());
                stack.setCount(0);
                chest.setChanged();
                break;
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toTransfer = Math.min(space, stack.getCount());
                    existing.grow(toTransfer);
                    stack.shrink(toTransfer);
                    chest.setChanged();
                }
            }
        }
        return stack;
    }

    private BlockPos getAdjacentBlock(ServerLevel level, BlockPos pos, Block... targetBlocks) {
        for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
            if (neighbor.distManhattan(pos) <= 1) {
                BlockState neighborState = level.getBlockState(neighbor);
                for (Block target : targetBlocks) {
                    if (neighborState.is(target)) {
                        return neighbor;
                    }
                }
            }
        }
        return null;
    }

    private boolean hasCompostableItems(SimpleContainer inventory) {
        if (inventory == null) return false;
        for (int i = BACKPACK_START; i <= BACKPACK_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.TORCHFLOWER_SEEDS))) {
                return true;
            }
        }
        return false;
    }

    private boolean consumeCompostableItem(SimpleContainer inventory) {
        if (inventory == null) return false;
        for (int i = BACKPACK_START; i <= BACKPACK_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && (stack.is(Items.WHEAT_SEEDS) || stack.is(Items.BEETROOT_SEEDS) || stack.is(Items.TORCHFLOWER_SEEDS))) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private Item getSeedItemForCrop(Block cropBlock) {
        if (cropBlock == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (cropBlock == Blocks.CARROTS) return Items.CARROT;
        if (cropBlock == Blocks.POTATOES) return Items.POTATO;
        if (cropBlock == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (cropBlock == Blocks.TORCHFLOWER_CROP) return Items.TORCHFLOWER_SEEDS;
        return null;
    }

    private boolean consumeItemFromBackpack(SimpleContainer inventory, Item itemToConsume) {
        if (inventory == null) return false;
        for (int i = BACKPACK_START; i <= BACKPACK_END; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(itemToConsume)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private void giveHarvestDrops(NpcEntity npc, BlockState cropState, ServerLevel level) {
        Block block = cropState.getBlock();
        List<ItemStack> drops = new ArrayList<>();

        if (block == Blocks.WHEAT) {
            drops.add(new ItemStack(Items.WHEAT, 1));
            drops.add(new ItemStack(Items.WHEAT_SEEDS, 1 + level.getRandom().nextInt(3)));
        } else if (block == Blocks.CARROTS) {
            drops.add(new ItemStack(Items.CARROT, 1 + level.getRandom().nextInt(3)));
        } else if (block == Blocks.POTATOES) {
            drops.add(new ItemStack(Items.POTATO, 1 + level.getRandom().nextInt(3)));
            if (level.getRandom().nextFloat() < 0.02F) {
                drops.add(new ItemStack(Items.POISONOUS_POTATO, 1));
            }
        } else if (block == Blocks.BEETROOTS) {
            drops.add(new ItemStack(Items.BEETROOT, 1));
            drops.add(new ItemStack(Items.BEETROOT_SEEDS, 1 + level.getRandom().nextInt(3)));
        } else if (block == Blocks.TORCHFLOWER_CROP) {
            drops.add(new ItemStack(Items.TORCHFLOWER, 1));
        } else if (block == Blocks.PUMPKIN) {
            drops.add(new ItemStack(Items.PUMPKIN, 1));
        } else if (block == Blocks.MELON) {
            drops.add(new ItemStack(Items.MELON_SLICE, 3 + level.getRandom().nextInt(5)));
        }

        SimpleContainer inventory = npc.getInventory();
        if (inventory != null) {
            for (ItemStack stack : drops) {
                addItemToBackpack(inventory, stack);
            }
        }
    }

    private void addItemToBackpack(SimpleContainer inventory, ItemStack stack) {
        for (int i = BACKPACK_START; i <= BACKPACK_END && !stack.isEmpty(); i++) {
            ItemStack existing = inventory.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toTransfer = Math.min(space, stack.getCount());
                    existing.grow(toTransfer);
                    stack.shrink(toTransfer);
                    inventory.setChanged();
                }
            }
        }

        for (int i = BACKPACK_START; i <= BACKPACK_END && !stack.isEmpty(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, stack.copy());
                stack.setCount(0);
                inventory.setChanged();
                break;
            }
        }
    }

    @Override
    public boolean canWork(NpcEntity npc) {
        return true;
    }

    private boolean hasEnoughItemsToStore(SimpleContainer inventory) {
        if (inventory == null) return false;
        int count = 0;
        for (int i = BACKPACK_START; i <= BACKPACK_END; i++) {
            if (!inventory.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count >= 3; // Se activará en cuanto tenga 3 o más slots ocupados
    }

    @Override
    public ItemStack getWorkTool() {
        return new ItemStack(Items.IRON_HOE);
    }

    @Override
    public long getWorkStartTime() {
        return 1000L; // Comienza por la mañana (7:00 AM)
    }

    @Override
    public long getWorkEndTime() {
        return 6000L; // Pausa del mediodía (12:00 PM)
    }

    @Override
    public long getSecondWorkStartTime() {
        return 8000L; // Regresa de la pausa (2:00 PM)
    }

    @Override
    public long getSecondWorkEndTime() {
        return 11500L; // Termina la jornada tarde (5:30 PM)
    }
}