package com.ludo.state;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InBaseState Unit Tests")
class InBaseStateTest {

    private InBaseState inBaseState;
    private Piece piece;

    @BeforeEach
    void setUp() {
        inBaseState = new InBaseState();
        piece = new Piece(PlayerColor.RED, 1);
        piece.setState(inBaseState);
    }

    @Test
    @DisplayName("Verify state name and string representation")
    void testStateNameAndToString() {
        assertEquals("InBase", inBaseState.getStateName());
        assertEquals("InBase", inBaseState.toString());
    }

    @Test
    @DisplayName("Verify piece in base CAN move when dice roll is 6 (Rule 2)")
    void testCanMoveOnRollSix() {
        assertTrue(inBaseState.canMove(piece, 6),
                "A piece in base must be allowed to move when rolling a 6");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("Verify piece in base CANNOT move on rolls 1 through 5 (Rule 2)")
    void testCannotMoveOnRollsOneToFive(int roll) {
        assertFalse(inBaseState.canMove(piece, roll),
                "A piece in base must not be allowed to move on roll " + roll);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify calculateMovementDistance is always 0 (transfers directly to starting square 'X')")
    void testMovementDistanceIsZero(int roll) {
        assertEquals(0, inBaseState.calculateMovementDistance(roll),
                "InBaseState movement distance must be 0 regardless of roll");
    }

    @Test
    @DisplayName("Verify InBaseState does not expire on round end (permanent until piece leaves)")
    void testDoesNotExpire() {
        assertEquals(-1, inBaseState.getRemainingRounds());

        for (int i = 0; i < 5; i++) {
            inBaseState.onRoundEnd(piece);
        }

        assertEquals(-1, inBaseState.getRemainingRounds());
        assertSame(inBaseState, piece.getState());
    }
}
