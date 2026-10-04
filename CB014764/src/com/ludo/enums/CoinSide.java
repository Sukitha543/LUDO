package com.ludo.enums;

/**
 * Represents the two sides of a coin: HEAD and TAIL.
 * Used in Rule T-1 to determine piece direction upon leaving the base.
 */
public enum CoinSide {
    HEAD("Head"),
    TAIL("Tail");

    private final String displayName;

    CoinSide(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Maps the coin toss result to piece movement direction (Rule T-1):
     * HEAD -> CLOCKWISE
     * TAIL -> COUNTER_CLOCKWISE
     */
    public Direction toDirection() {
        return (this == HEAD) ? Direction.CLOCKWISE : Direction.COUNTER_CLOCKWISE;
    }
}
