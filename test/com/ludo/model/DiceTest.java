package com.ludo.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dice Unit Tests")
class DiceTest {

    private Dice dice;

    @BeforeEach
    void setUp() {
        dice = Dice.getInstance();
    }

    @Test
    @DisplayName("Verify that Dice is a singleton and not null")
    void testSingletonInstance() {
        assertNotNull(dice, "Dice instance should not be null");
        Dice anotherInstance = Dice.getInstance();
        assertSame(dice, anotherInstance, "Dice should follow singleton pattern and return the exact same instance");
    }

    @Test
    @DisplayName("Verify roll produces values between 1 and 6")
    void testRollWithinRange() {
        for (int i = 0; i < 100; i++) {
            int roll = dice.roll();
            assertTrue(roll >= Dice.MIN_VALUE && roll <= Dice.MAX_VALUE,
                    "Roll value " + roll + " should be between 1 and 6");
        }
    }

    @Test
    @DisplayName("Verify possible values contains 1 through 6")
    void testPossibleValues() {
        int[] expected = { 1, 2, 3, 4, 5, 6 };
        int[] actual = dice.getPossibleValues();
        assertArrayEquals(expected, actual, "Possible values array should contain numbers 1 to 6");
    }
}
