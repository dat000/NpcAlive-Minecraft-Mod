package com.devdat.npcalive.entity.profession;

import com.devdat.npcalive.entity.NpcProfession;
import java.util.EnumMap;
import java.util.Map;

public class ProfessionRegistry {
    private static final Map<NpcProfession, ProfessionLogic> LOGIC_MAP = new EnumMap<>(NpcProfession.class);
    private static final ProfessionLogic DEFAULT_LOGIC = new DefaultProfession();

    static {
        // Registramos las profesiones que tengan lógica personalizada
        LOGIC_MAP.put(NpcProfession.FARMER, new FarmerProfession());
        // Aquí agregaremos más adelante:
        // LOGIC_MAP.put(NpcProfession.BLACKSMITH, new BlacksmithProfession());
    }

    public static ProfessionLogic getLogic(NpcProfession profession) {
        if (profession == null) return DEFAULT_LOGIC;
        return LOGIC_MAP.getOrDefault(profession, DEFAULT_LOGIC);
    }
}