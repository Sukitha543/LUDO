package com.ludo.model;

import java.util.Random;

/**
 * Singleton Dice representing a six-sided die with values 1 to 6.
 * Adheres to the Singleton Pattern to ensure a single shared die object
 * is used across all players, matching real-world tabletop game mechanics.
 */
public class Dice {

    private static final Dice INSTANCE = new Dice();

    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 6;
    public static final int[] POSSIBLE_VALUES = { 1, 2, 3, 4, 5, 6 };

    private final Random random;

    // Private constructor prevents direct instantiation from outside
    private Dice() {
        this.random = com.ludo.util.GameRandom.getInstance();
    }

    /**
     * Returns the global singleton instance of the Dice.
     *
     * @return the single Dice instance
     */
    public static Dice getInstance() {
        return INSTANCE;
    }

    /**
     * Rolls the die and returns a random value between 1 and 6 inclusive.
     *
     * @return face value of the die (1 to 6)
     */
    public int roll() {
        return random.nextInt(MAX_VALUE - MIN_VALUE + 1) + MIN_VALUE;
    }

    /**
     * Returns the possible face values of the die.
     *
     * @return copy of the array of possible values [1, 2, 3, 4, 5, 6]
     */
    public int[] getPossibleValues() {
        return POSSIBLE_VALUES.clone();
    }
}
