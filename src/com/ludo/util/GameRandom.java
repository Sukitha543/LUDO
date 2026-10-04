package com.ludo.util;

import java.util.Random;

public class GameRandom {

    private static final Random instance = new Random();

    private GameRandom() {
    }

    public static Random getInstance() {
        return instance;
    }

    public static void setSeed(long seed) {
        instance.setSeed(seed);
    }
}
