package com.ludo.enums;

/**
 * Represents the type of a cell on the Ludo board.
 */
public enum CellType {
    STANDARD,      // Standard white cells on the circular track
    STARTING_X,    // Starting square marked with 'X' for a specific color
    APPROACH,      // Approach cell marked with a circle before home straight
    HOME_PATH,     // Color-specific home straight cells leading to Home
    HOME,          // Final destination cell in the center
    BASE           // Starting base area for each color
}
