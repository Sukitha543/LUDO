package com.ludo.state;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NormalState Unit Tests")
class NormalStateTest {

    private NormalState normalState;
    private Piece piece;

    @BeforeEach
    void setUp() {
        normalState = new NormalState();
        piece = new Piece(PlayerColor.RED, 1);
        piece.setState(normalState);
    }

    @Test
    @DisplayName("Verify state name and string representation")
    void testStateNameAndToString() {
        assertEquals("Normal", normalState.getStateName());
        assertEquals("Normal", normalState.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify piece in NormalState can always move on any roll 1 through 6")
    void testCanMoveOnAnyRoll(int roll) {
        assertTrue(normalState.canMove(piece, roll),
                "NormalState should allow movement on roll " + roll);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify movement distance matches the exact dice roll (1x speed)")
    void testCalculateMovementDistance(int roll) {
        assertEquals(roll, normalState.calculateMovementDistance(roll),
                "Movement distance must equal the dice roll value");
    }

    @Test
    @DisplayName("Verify NormalState does not expire on round end (permanent)")
    void testDoesNotExpire() {
        assertEquals(-1, normalState.getRemainingRounds(), "Permanent states return -1 for remaining rounds");

        // Simulate 10 round ends
        for (int i = 0; i < 10; i++) {
            normalState.onRoundEnd(piece);
        }

        assertEquals(-1, normalState.getRemainingRounds());
        assertSame(normalState, piece.getState(), "Piece state must remain NormalState after round ends");
    }
}
