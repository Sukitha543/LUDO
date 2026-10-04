package com.ludo.state;

import com.ludo.model.Piece;

/**
 * State interface for the Piece State Pattern.
 * Alters the piece's behavior (movement distance, movement eligibility, and status effects)
 * dynamically based on its current condition (Rules T-12, T-13, T-9).
 */
public interface PieceState {

    /**
     * Returns the human-readable name of the current state.
     */
    String getStateName();

    /**
     * Determines whether the piece is eligible to move given the current dice roll.
     */
    boolean canMove(Piece piece, int diceRoll);

    /**
     * Calculates the actual number of cells the piece moves based on the dice roll
     * and active buffs/debuffs (e.g. double speed for Energized, half speed for Sick).
     */
    int calculateMovementDistance(int diceRoll);

    /**
     * Called at the end of each round to handle status durations (e.g., 4 rounds).
     */
    void onRoundEnd(Piece piece);

    /**
     * Returns the number of rounds remaining for temporary effects (or -1 if permanent/indefinite).
     */
    int getRemainingRounds();
}
