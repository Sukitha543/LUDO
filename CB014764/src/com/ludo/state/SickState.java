package com.ludo.state;

import com.ludo.model.Piece;

/**
 * Sick State (Rule T-12):
 * Triggered when teleported to Alpha and receives negative aura.
 * For the next 4 rounds, the piece moves at half speed (floor(roll / 2)).
 */
public class SickState extends AbstractTimedState {

    public static final int DURATION_ROUNDS = 4;

    public SickState() {
        super(DURATION_ROUNDS);
    }

    public SickState(int rounds) {
        super(rounds);
    }

    @Override
    public String getStateName() {
        return "Sick";
    }

    @Override
    public boolean canMove(Piece piece, int diceRoll) {
        return true;
    }

    @Override
    public int calculateMovementDistance(int diceRoll) {
        // Half the value of the roll (Rule T-12)
        return Math.max(1, diceRoll / 2);
    }

    @Override
    public String toString() {
        return getStateName() + " (" + roundsRemaining + " rounds left)";
    }
}
