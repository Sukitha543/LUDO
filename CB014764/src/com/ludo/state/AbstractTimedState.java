package com.ludo.state;

import com.ludo.model.Piece;

/**
 * Base class for states that last for a temporary number of rounds (e.g. 4 rounds).
 * Automatically handles decrementing the duration and reverting to NormalState when expired.
 */
public abstract class AbstractTimedState implements PieceState {

    protected int roundsRemaining;

    public AbstractTimedState(int initialRounds) {
        this.roundsRemaining = initialRounds;
    }

    @Override
    public int getRemainingRounds() {
        return roundsRemaining;
    }

    @Override
    public void onRoundEnd(Piece piece) {
        if (roundsRemaining > 0) {
            roundsRemaining--;
        }
        if (roundsRemaining <= 0) {
            // Revert back to Normal state once duration expires
            piece.setState(new NormalState());
        }
    }
}
