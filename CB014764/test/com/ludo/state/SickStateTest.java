package com.ludo.state;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SickState Unit Tests (Rule T-12)")
class SickStateTest {

    private SickState sickState;
    private Piece piece;

    @BeforeEach
    void setUp() {
        sickState = new SickState();
        piece = new Piece(PlayerColor.RED, 1);
        piece.setState(sickState);
    }

    @Test
    @DisplayName("Verify state name, constant, and string representation")
    void testStateNameAndFormatting() {
        assertEquals("Sick", sickState.getStateName());
        assertEquals(4, SickState.DURATION_ROUNDS);
        assertEquals("Sick (4 rounds left)", sickState.toString());
    }

    @Test
    @DisplayName("Verify default and custom constructors set correct round counts")
    void testConstructors() {
        assertEquals(4, sickState.getRemainingRounds(), "Default SickState duration must be 4 rounds per Rule T-12");

        SickState customState = new SickState(2);
        assertEquals(2, customState.getRemainingRounds());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify canMove returns true for all dice rolls 1 through 6")
    void testCanMoveOnAllRolls(int roll) {
        assertTrue(sickState.canMove(piece, roll),
                "Sick piece should be able to move on roll " + roll);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "2, 1",
            "3, 1",
            "4, 2",
            "5, 2",
            "6, 3"
    })
    @DisplayName("Verify calculateMovementDistance halves dice roll value (floor(roll / 2), minimum 1, Rule T-12)")
    void testHalfSpeedMovement(int roll, int expectedDistance) {
        assertEquals(expectedDistance, sickState.calculateMovementDistance(roll),
                "SickState must halve roll with minimum 1: roll " + roll + " should yield " + expectedDistance);
    }

    @Test
    @DisplayName("Verify 4-round lifecycle and automatic reversion to NormalState upon expiration")
    void testLifecycleAndExpiration() {
        assertEquals(4, sickState.getRemainingRounds());

        // Round 1
        sickState.onRoundEnd(piece);
        assertEquals(3, sickState.getRemainingRounds());
        assertSame(sickState, piece.getState());

        // Round 2
        sickState.onRoundEnd(piece);
        assertEquals(2, sickState.getRemainingRounds());
        assertSame(sickState, piece.getState());

        // Round 3
        sickState.onRoundEnd(piece);
        assertEquals(1, sickState.getRemainingRounds());
        assertSame(sickState, piece.getState());

        // Round 4 (Expiry)
        sickState.onRoundEnd(piece);
        assertEquals(0, sickState.getRemainingRounds());

        // Must automatically revert to NormalState
        assertInstanceOf(NormalState.class, piece.getState(),
                "Piece must automatically revert to NormalState after 4 rounds of Sick debuff");
    }
}
