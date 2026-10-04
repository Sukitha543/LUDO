package com.ludo.state;

import com.ludo.model.Piece;

/**
 * Represents a piece that has safely reached Home.
 * Removed from active play and cannot move again.
 */
public class HomeState implements PieceState {

    @Override
    public String getStateName() {
        return "Home";
    }

    @Override
    public boolean canMove(Piece piece, int diceRoll) {
        return false; // Piece is finished
    }

    @Override
    public int calculateMovementDistance(int diceRoll) {
        return 0;
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
