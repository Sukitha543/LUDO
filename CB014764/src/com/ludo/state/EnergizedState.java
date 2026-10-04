package com.ludo.state;

import com.ludo.model.Piece;

/**
 * Energized State (Rule T-12):
 * Triggered when teleported to Alpha and receives positive aura.
 * For the next 4 rounds, the piece moves at double speed (2 * roll).
 */
public class EnergizedState extends AbstractTimedState {

    public static final int DURATION_ROUNDS = 4;

    public EnergizedState() {
        super(DURATION_ROUNDS);
    }

    public EnergizedState(int rounds) {
        super(rounds);
    }

    @Override
    public String getStateName() {
        return "Energized";
    }

    @Override
    public boolean canMove(Piece piece, int diceRoll) {
        return true;
    }

    @Override
    public int calculateMovementDistance(int diceRoll) {
        // Double the value of the roll (Rule T-12)
        return diceRoll * 2;
    }

    @Override
    public String toString() {
        return getStateName() + " (" + roundsRemaining + " rounds left)";
    }
}
