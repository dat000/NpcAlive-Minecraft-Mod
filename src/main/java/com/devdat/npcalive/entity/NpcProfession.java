package com.devdat.npcalive.entity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

public enum NpcProfession {
    NONE(null),
    FARMER(Blocks.COMPOSTER); // 1. Granjero -> Compostador

    private final Block workstationBlock;

    // Mapa estático global para búsqueda ultra rápida O(1)
    private static final Map<Block, NpcProfession> WORKSTATION_MAP = new HashMap<>();

    static {
        for (NpcProfession profession : values()) {
            if (profession.workstationBlock != null) {
                WORKSTATION_MAP.put(profession.workstationBlock, profession);
            }
        }
    }

    NpcProfession(Block workstationBlock) {
        this.workstationBlock = workstationBlock;
    }

    public Block getWorkstationBlock() {
        return this.workstationBlock;
    }

    public boolean matchesWorkstation(BlockState state) {
        return this.workstationBlock != null && state.is(this.workstationBlock);
    }

    // Búsqueda instantánea por bloque
    public static NpcProfession getByWorkstation(BlockState state) {
        return WORKSTATION_MAP.getOrDefault(state.getBlock(), NONE);
    }

    public static NpcProfession randomProfession(RandomSource random) {
        NpcProfession[] values = values();
        if (values.length <= 1) return NONE;
        // Selecciona aleatoriamente excluyendo NONE (índice 0)
        int index = 1 + random.nextInt(values.length - 1);
        return values[index];
    }
}