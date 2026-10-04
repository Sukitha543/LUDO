package com.ludo.state;

import com.ludo.model.Piece;

/**
 * Briefing State (Rule T-13):
 * Triggered when teleported to Beta.
 * The piece has to attend a briefing and cannot move for the next four (4) rounds.
 * Movement distance is 0 and canMove returns false.
 */
public class BriefingState extends AbstractTimedState {

    public static final int DURATION_ROUNDS = 4;

    public BriefingState() {
        super(DURATION_ROUNDS);
    }

    public BriefingState(int rounds) {
        super(rounds);
    }

    @Override
    public String getStateName() {
        return "Briefing";
    }

    @Override
    public boolean canMove(Piece piece, int diceRoll) {
        // Cannot move while attending briefing (Rule T-13)
        return false;
    }

    @Override
    public int calculateMovementDistance(int diceRoll) {
        return 0;
    }

    @Override
    public String toString() {
        return getStateName() + " (" + roundsRemaining + " rounds left - FROZEN)";
    }
}
