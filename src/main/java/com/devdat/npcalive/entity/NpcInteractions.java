package com.devdat.npcalive.entity;

public enum NpcInteractions {
    GREET("Saludar"),
    ROMANCE("Romance"),
    MEAN("Insultar"),
    TRANSACTIONS("Comerciar");

    private final String displayName;

    NpcInteractions(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}