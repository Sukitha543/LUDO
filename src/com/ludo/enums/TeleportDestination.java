package com.ludo.enums;

/**
 * Represents the six teleport destinations when landing on a Mystery Cell (Rule T-11 / Step 13).
 */
public enum TeleportDestination {
    ALPHA("Alpha"),
    BETA("Beta"),
    GAMMA("Gamma"),
    BASE("Base"),
    STARTING_X("X"),
    APPROACH("Approach");

    private final String displayName;

    TeleportDestination(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
