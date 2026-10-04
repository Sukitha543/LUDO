package com.ludo.state;

import com.ludo.model.Piece;

/**
 * Standard active state for a piece on the board.
 * Moves at normal 1x speed and has no status restrictions.
 */
public class NormalState implements PieceState {

    @Override
    public String getStateName() {
        return "Normal";
    }

    @Override
    public boolean canMove(Piece piece, int diceRoll) {
        return true;
    }

    @Override
    public int calculateMovementDistance(int diceRoll) {
        return diceRoll;
    }

    @Override
    public void onRoundEnd(Piece piece) {
        // Normal state does not expire
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
