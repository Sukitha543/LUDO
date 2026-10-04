package com.ludo.state;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AbstractTimedState Unit Tests")
class AbstractTimedStateTest {

    // Concrete test implementation of the abstract base class
    private static class DummyTimedState extends AbstractTimedState {
        public DummyTimedState(int initialRounds) {
            super(initialRounds);
        }

        @Override
        public String getStateName() {
            return "DummyTimed";
        }

        @Override
        public boolean canMove(Piece piece, int diceRoll) {
            return true;
        }

        @Override
        public int calculateMovementDistance(int diceRoll) {
            return diceRoll;
        }
    }

    private Piece piece;
    private DummyTimedState timedState;

    @BeforeEach
    void setUp() {
        piece = new Piece(PlayerColor.RED, 1);
        timedState = new DummyTimedState(3);
        piece.setState(timedState);
    }

    @Test
    @DisplayName("Verify initial remaining rounds matches constructor value")
    void testInitialRemainingRounds() {
        assertEquals(3, timedState.getRemainingRounds(), "Remaining rounds should match initial constructor value");
    }

    @Test
    @DisplayName("Verify roundsRemaining decrements by 1 on each onRoundEnd invocation")
    void testRoundCountdown() {
        assertEquals(3, timedState.getRemainingRounds());

        timedState.onRoundEnd(piece);
        assertEquals(2, timedState.getRemainingRounds());
        assertSame(timedState, piece.getState(), "Piece should retain timed state while rounds > 0");

        timedState.onRoundEnd(piece);
        assertEquals(1, timedState.getRemainingRounds());
        assertSame(timedState, piece.getState());
    }

    @Test
    @DisplayName("Verify state automatically reverts to NormalState when roundsRemaining reaches 0")
    void testAutomaticReversionToNormalState() {
        // Round 1
        timedState.onRoundEnd(piece);
        assertEquals(2, timedState.getRemainingRounds());

        // Round 2
        timedState.onRoundEnd(piece);
        assertEquals(1, timedState.getRemainingRounds());

        // Round 3 (Final round: expires)
        timedState.onRoundEnd(piece);
        assertEquals(0, timedState.getRemainingRounds());

        // Piece must now be reverted to NormalState!
        assertInstanceOf(NormalState.class, piece.getState(),
                "Piece should automatically transition to NormalState upon expiration");
    }

    @Test
    @DisplayName("Verify behavior when initialized with zero rounds (immediate expiration)")
    void testImmediateExpirationWithZeroRounds() {
        DummyTimedState zeroRoundState = new DummyTimedState(0);
        piece.setState(zeroRoundState);

        zeroRoundState.onRoundEnd(piece);

        assertEquals(0, zeroRoundState.getRemainingRounds(), "Rounds should not drop below 0");
        assertInstanceOf(NormalState.class, piece.getState(),
                "Zero-round state should immediately revert piece to NormalState");
    }

    @Test
    @DisplayName("Verify roundsRemaining does not decrement below zero if onRoundEnd called repeatedly")
    void testDoesNotDecrementBelowZero() {
        DummyTimedState oneRoundState = new DummyTimedState(1);
        piece.setState(oneRoundState);

        oneRoundState.onRoundEnd(piece); // reaches 0 -> reverts
        assertEquals(0, oneRoundState.getRemainingRounds());

        oneRoundState.onRoundEnd(piece); // extra call
        assertEquals(0, oneRoundState.getRemainingRounds(), "Rounds remaining should clamp at 0");
    }
}
