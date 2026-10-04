package com.ludo.strategy;

import com.ludo.model.Board;
import com.ludo.model.Move;
import com.ludo.model.Player;

import java.util.List;

/**
 * Strategy interface for player decision making.
 * Implements the Strategy Design Pattern to decouple AI move selection
 * algorithms from the Player model and Game Engine.
 */
public interface PlayerStrategy {

    /**
     * Selects the best move among legal moves according to the player's unique
     * personality.
     *
     * @param player     the player making the move
     * @param legalMoves list of legal moves available for the current roll
     * @param board      current state of the board
     * @param diceRoll   the face value of the dice
     * @return chosen Move, or null if no legal moves exist
     */
    Move selectMove(Player player, List<Move> legalMoves, Board board, int diceRoll);
}
