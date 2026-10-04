package com.ludo.state;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BriefingState Unit Tests (Rule T-13)")
class BriefingStateTest {

    private BriefingState briefingState;
    private Piece piece;

    @BeforeEach
    void setUp() {
        briefingState = new BriefingState();
        piece = new Piece(PlayerColor.RED, 1);
        piece.setState(briefingState);
    }

    @Test
    @DisplayName("Verify state name, constant, and string representation")
    void testStateNameAndFormatting() {
        assertEquals("Briefing", briefingState.getStateName());
        assertEquals(4, BriefingState.DURATION_ROUNDS);
        assertEquals("Briefing (4 rounds left - FROZEN)", briefingState.toString());
    }

    @Test
    @DisplayName("Verify default and custom constructors set correct round counts")
    void testConstructors() {
        assertEquals(4, briefingState.getRemainingRounds(), "Default BriefingState duration must be 4 rounds per Rule T-13");

        BriefingState customState = new BriefingState(2);
        assertEquals(2, customState.getRemainingRounds());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify canMove returns false for all dice rolls (Piece is frozen while attending briefing, Rule T-13)")
    void testCannotMoveOnAnyRoll(int roll) {
        assertFalse(briefingState.canMove(piece, roll),
                "Briefing piece must be frozen and cannot move on roll " + roll);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify calculateMovementDistance is always 0")
    void testMovementDistanceIsZero(int roll) {
        assertEquals(0, briefingState.calculateMovementDistance(roll),
                "BriefingState movement distance must be 0 regardless of roll");
    }

    @Test
    @DisplayName("Verify 4-round frozen lifecycle and unfreezing back to NormalState upon expiration")
    void testLifecycleAndUnfreezing() {
        assertEquals(4, briefingState.getRemainingRounds());

        // Rounds 1, 2, 3: Still frozen
        for (int i = 3; i >= 1; i--) {
            briefingState.onRoundEnd(piece);
            assertEquals(i, briefingState.getRemainingRounds());
            assertSame(briefingState, piece.getState());
            assertFalse(piece.canMove(6), "Piece remains frozen during countdown");
        }

        // Round 4 (Expiry: Unfreezes!)
        briefingState.onRoundEnd(piece);
        assertEquals(0, briefingState.getRemainingRounds());

        // Piece must now be unfreezed and back to NormalState!
        assertInstanceOf(NormalState.class, piece.getState(),
                "Piece must unfreeze and revert to NormalState after 4 rounds of Briefing");
        assertTrue(piece.canMove(6), "Piece should now be able to move again in NormalState");
    }
}
