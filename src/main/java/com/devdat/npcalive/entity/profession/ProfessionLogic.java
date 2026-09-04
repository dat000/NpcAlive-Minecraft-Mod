package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public interface ProfessionLogic {
    default BlockPos getTargetPosition(NpcEntity npc, BlockPos workPos) {
        return workPos;
    }

    void performWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos);

    default void tickWork(NpcEntity npc, BlockPos targetPos, BlockPos workPos, int workTimer) {
        // Por defecto vacío para profesiones que no requieran ticks intermedios
    }

    boolean canWork(NpcEntity npc);

    default ItemStack getWorkTool() {
        return net.minecraft.world.item.ItemStack.EMPTY; // Por defecto no usa herramienta
    }

    default long getSecondWorkStartTime() {
        return -1L; // -1 significa que no tiene segunda jornada por defecto
    }

    default long getSecondWorkEndTime() {
        return -1L;
    }

    default long getWorkStartTime() {
        return 0L;
    }

    default long getWorkEndTime() {
        return 12000L;
    }

}