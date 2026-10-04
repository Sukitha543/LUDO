package com.ludo.enums;

/**
 * Represents the four player colors in the Ludo game: Red, Green, Yellow, and Blue.
 */
public enum PlayerColor {
    RED("Red", "R"),
    GREEN("Green", "G"),
    YELLOW("Yellow", "Y"),
    BLUE("Blue", "B");

    private final String displayName;
    private final String prefix;

    PlayerColor(String displayName, String prefix) {
        this.displayName = displayName;
        this.prefix = prefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }
}
