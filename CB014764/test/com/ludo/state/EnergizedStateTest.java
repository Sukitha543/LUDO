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

@DisplayName("EnergizedState Unit Tests (Rule T-12)")
class EnergizedStateTest {

    private EnergizedState energizedState;
    private Piece piece;

    @BeforeEach
    void setUp() {
        energizedState = new EnergizedState();
        piece = new Piece(PlayerColor.RED, 1);
        piece.setState(energizedState);
    }

    @Test
    @DisplayName("Verify state name, constant, and string representation")
    void testStateNameAndFormatting() {
        assertEquals("Energized", energizedState.getStateName());
        assertEquals(4, EnergizedState.DURATION_ROUNDS);
        assertEquals("Energized (4 rounds left)", energizedState.toString());
    }

    @Test
    @DisplayName("Verify default constructor sets duration to 4 rounds")
    void testDefaultDuration() {
        assertEquals(4, energizedState.getRemainingRounds(),
                "Default EnergizedState duration must be 4 rounds per Rule T-12");
    }

    @Test
    @DisplayName("Verify custom constructor sets specified round count")
    void testCustomDuration() {
        EnergizedState customState = new EnergizedState(2);
        assertEquals(2, customState.getRemainingRounds());
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
    @DisplayName("Verify canMove returns true for all dice rolls 1 through 6")
    void testCanMoveOnAllRolls(int roll) {
        assertTrue(energizedState.canMove(piece, roll),
                "Energized piece should be able to move on roll " + roll);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 2",
            "2, 4",
            "3, 6",
            "4, 8",
            "5, 10",
            "6, 12"
    })
    @DisplayName("Verify calculateMovementDistance doubles the dice roll value (2 * roll, Rule T-12)")
    void testDoubleSpeedMovement(int roll, int expectedDistance) {
        assertEquals(expectedDistance, energizedState.calculateMovementDistance(roll),
                "EnergizedState must double the dice roll: roll " + roll + " should yield " + expectedDistance);
    }

    @Test
    @DisplayName("Verify full 4-round lifecycle and automatic transition back to NormalState")
    void testLifecycleAndExpiration() {
        assertEquals(4, energizedState.getRemainingRounds());

        // Round 1
        energizedState.onRoundEnd(piece);
        assertEquals(3, energizedState.getRemainingRounds());
        assertSame(energizedState, piece.getState());

        // Round 2
        energizedState.onRoundEnd(piece);
        assertEquals(2, energizedState.getRemainingRounds());
        assertSame(energizedState, piece.getState());

        // Round 3
        energizedState.onRoundEnd(piece);
        assertEquals(1, energizedState.getRemainingRounds());
        assertSame(energizedState, piece.getState());

        // Round 4 (Expiry)
        energizedState.onRoundEnd(piece);
        assertEquals(0, energizedState.getRemainingRounds());

        // State must revert to NormalState
        assertInstanceOf(NormalState.class, piece.getState(),
                "Piece must revert to NormalState after 4 rounds of Energized effect");
    }
}
