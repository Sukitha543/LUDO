package com.ludo.model;

import com.ludo.enums.CoinSide;
import java.util.Random;

/**
 * Singleton Coin representing a two-sided coin (HEAD and TAIL).
 * Adheres to the Singleton Pattern so that all players share the same coin,
 * mirroring tabletop gameplay and Rule T-1 requirements.
 */
public class Coin {

    private static final Coin INSTANCE = new Coin();
    private static final CoinSide[] POSSIBLE_SIDES = CoinSide.values();

    private final Random random;

    // Private constructor prevents direct instantiation
    private Coin() {
        this.random = com.ludo.util.GameRandom.getInstance();
    }

    /**
     * Returns the global singleton instance of the Coin.
     *
     * @return the single Coin instance
     */
    public static Coin getInstance() {
        return INSTANCE;
    }

    /**
     * Tosses the coin and returns HEAD or TAIL with equal probability.
     *
     * @return CoinSide.HEAD or CoinSide.TAIL
     */
    public CoinSide toss() {
        return random.nextBoolean() ? CoinSide.HEAD : CoinSide.TAIL;
    }

    /**
     * Returns the possible sides of the coin.
     *
     * @return array of possible CoinSides [HEAD, TAIL]
     */
    public CoinSide[] getPossibleSides() {
        return POSSIBLE_SIDES.clone();
    }
}
