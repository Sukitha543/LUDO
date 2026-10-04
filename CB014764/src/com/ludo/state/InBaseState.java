package com.ludo.state;

import com.ludo.model.Piece;

/**
 * Represents a piece resting in its player's Base.
 * Can only move if a six (6) is rolled on the dice (Rule 2).
 */
public class InBaseState implements PieceState {

    @Override
    public String getStateName() {
        return "InBase";
    }

    @Override
    public boolean canMove(Piece piece, int diceRoll) {
        // Can only leave base on a 6 (Rule 2)
        return diceRoll == 6;
    }

    @Override
    public int calculateMovementDistance(int diceRoll) {
        return 0; // Moves directly from base onto starting square 'X'
    }

    @Override
    public void onRoundEnd(Piece piece) {
        // Does not expire
    }

    @Override
    public int getRemainingRounds() {
        return -1;
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
