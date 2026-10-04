package com.ludo.engine;

import com.ludo.execptions.GameEngineException;
import com.ludo.model.Board;
import com.ludo.model.Dice;
import com.ludo.model.GameState;
import com.ludo.model.Move;
import com.ludo.model.Player;
import com.ludo.view.GameView;

import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles individual player turn execution, bonus rolls on six / captures,
 * three-sixes penalty and blockade-breaking triggers, and turn termination.
 * Follows Single Responsibility Principle (SRP) by isolating turn-level rules.
 */
public class TurnHandler {

    private static final Logger LOGGER = Logger.getLogger(TurnHandler.class.getName());

    private final GameState gameState;
    private final GameView view;
    private final MoveExecutor moveExecutor;
    private final TeleportHandler teleportHandler;

    public TurnHandler(GameState gameState, GameView view, MoveExecutor moveExecutor, TeleportHandler teleportHandler) {
        if (gameState == null || view == null) {
            throw new IllegalArgumentException("GameState and GameView cannot be null");
        }
        this.gameState = gameState;
        this.view = view;
        this.teleportHandler = (teleportHandler != null) ? teleportHandler : new TeleportHandler(gameState, view);
        this.moveExecutor = (moveExecutor != null) ? moveExecutor : new MoveExecutor(gameState, view, this.teleportHandler);
    }

    /**
     * Executes a complete turn for a player (Step 4, Step 7 / Rule 4 & Rule T-2):
     * - Rolling a 6 grants a bonus roll attempt.
     * - If 6 is rolled on the second attempt, a third attempt is given.
     * - However, if 6 is rolled three (3) consecutive times, the third roll is ignored,
     *   no move is made for the 3rd roll, and the dice passes to the next player!
     * - Capturing an opponent piece grants another roll attempt as a bonus (Rule T-2).
     */
    public void executePlayerTurn(Player player) {
        executePlayerTurn(player, null);
    }

    /**
     * Overloaded turn execution that accepts a scripted queue of dice rolls for testing/simulation.
     * If the queue is empty or null, rolls the singleton Dice.
     */
    public void executePlayerTurn(Player player, Queue<Integer> scriptedRolls) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        int attempt = 0;
        int consecutiveSixes = 0;
        final int MAX_ATTEMPTS = 3;
        Dice dice = gameState.getDice();
        Board board = gameState.getBoard();

        if (dice == null || board == null) {
            throw new GameEngineException("Dice or Board is not available in GameState");
        }

        while (attempt < MAX_ATTEMPTS) {
            attempt++;

            Integer scripted = (scriptedRolls != null) ? scriptedRolls.poll() : null;
            int roll = (scripted != null) ? scripted : dice.roll();

            if (roll < 1 || roll > 6) {
                throw new GameEngineException("Invalid dice roll: " + roll);
            }

            view.displayPlayerRoll(player, roll);

            // Rule T-13: Check if rolling 3 consecutively while piece/block is movement-restricted in Beta
            teleportHandler.checkBetaConsecutiveThrees(player, roll);

            if (roll == 6) {
                consecutiveSixes++;

                // If 6 occurs during the 3rd attempt, or three consecutive 6s occur:
                if (attempt == MAX_ATTEMPTS || consecutiveSixes == 3) {
                    if (board.hasBlockade(player.getColor())) {
                        moveExecutor.breakBlockadeOnThreeSixes(player);
                    } else {
                        view.displayThreeSixesPenalty(player);
                    }
                    break;
                }

                boolean captured = playMove(player, board, roll);

                if (gameState.isGameOver()) {
                    break;
                }

                // If attempts remaining, notify bonus roll
                if (attempt < MAX_ATTEMPTS) {
                    if (captured) {
                        view.displayBonusAttempt(player, "captured an opponent piece");
                    } else {
                        view.displayBonusAttempt(player, "rolled a six");
                    }
                } else {
                    break;
                }
            } else {
                // Normal roll != 6
                consecutiveSixes = 0;
                boolean captured = playMove(player, board, roll);

                if (gameState.isGameOver()) {
                    break;
                }

                if (captured && attempt < MAX_ATTEMPTS) {
                    // Rule T-2 / Step 7: Capturing an opponent piece grants another roll attempt as a bonus!
                    view.displayBonusAttempt(player, "captured an opponent piece");
                    continue;
                }

                break; // Normal turn ends
            }
        }
    }

    /**
     * Generates legal moves for the roll, lets the player choose one, and executes it.
     * Failures are logged and treated as "no move made" (no capture bonus).
     *
     * @return true if the executed move captured an opponent piece
     */
    private boolean playMove(Player player, Board board, int roll) {
        List<Move> legalMoves;
        try {
            legalMoves = MoveGenerator.generateLegalMoves(player, board, roll);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Move generation failed for " + player.getColor() + " (roll " + roll + ")", e);
            return false;
        }

        if (legalMoves == null || legalMoves.isEmpty()) {
            System.out.println(
                    "[" + player.getColor().getDisplayName() + "] has no valid moves with roll " + roll + ".");
            return false;
        }

        try {
            Move chosenMove = player.chooseMove(legalMoves, board, roll);
            if (chosenMove == null) {
                throw new GameEngineException("chooseMove returned null for " + player.getColor());
            }
            return moveExecutor.executeMove(player, chosenMove);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Move failed for " + player.getColor() + " (roll " + roll + ")", e);
            return false;
        }
    }
}
