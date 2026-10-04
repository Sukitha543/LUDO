package com.ludo.enums;

/**
 * Represents the movement direction of a piece on the board.
 * In LUDO-T, determined by coin toss upon leaving the base (Rule T-1).
 */
public enum Direction {
    CLOCKWISE("clockwise"),
    COUNTER_CLOCKWISE("counter-clockwise");

    private final String displayName;

    Direction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
