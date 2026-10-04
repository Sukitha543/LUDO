package com.ludo.model;

import com.ludo.enums.CoinSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Coin Unit Tests")
class CoinTest {

    private Coin coin;

    @BeforeEach
    void setUp() {
        coin = Coin.getInstance();
    }

    @Test
    @DisplayName("Verify Coin is a Singleton and not null")
    void testSingletonInstance() {
        assertNotNull(coin, "Coin instance should not be null");
        Coin anotherInstance = Coin.getInstance();
        assertSame(coin, anotherInstance, "Coin should follow singleton pattern and return the exact same instance");
    }

    @Test
    @DisplayName("Verify possible sides contains HEAD and TAIL")
    void testPossibleSides() {
        CoinSide[] possibleSides = coin.getPossibleSides();
        assertNotNull(possibleSides);
        assertEquals(2, possibleSides.length, "Coin should have exactly 2 sides");
        assertArrayEquals(new CoinSide[]{CoinSide.HEAD, CoinSide.TAIL}, possibleSides);
    }

    @Test
    @DisplayName("Verify toss produces valid CoinSide (HEAD or TAIL) repeatedly")
    void testTossReturnsValidSides() {
        EnumSet<CoinSide> observedSides = EnumSet.noneOf(CoinSide.class);

        for (int i = 0; i < 100; i++) {
            CoinSide result = coin.toss();
            assertNotNull(result, "Toss result should never be null");
            assertTrue(result == CoinSide.HEAD || result == CoinSide.TAIL, "Result must be HEAD or TAIL");
            observedSides.add(result);
        }

        // Over 100 tosses, both HEAD and TAIL should appear
        assertTrue(observedSides.contains(CoinSide.HEAD), "Coin toss should produce HEAD over multiple tosses");
        assertTrue(observedSides.contains(CoinSide.TAIL), "Coin toss should produce TAIL over multiple tosses");
    }
}
