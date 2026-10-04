package com.ludo.engine;

import com.ludo.execptions.GameEngineException;
import com.ludo.model.Dice;
import com.ludo.model.GameState;
import com.ludo.model.Piece;
import com.ludo.model.Player;
import com.ludo.view.GameView;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages first-player determination (rolling for six) and establishing clockwise turn order.
 * Follows Single Responsibility Principle (SRP) by isolating turn scheduling and seating setup.
 */
public class TurnOrderManager {

    private static final int MAX_FIRST_PLAYER_ATTEMPTS = 1000;

    private final GameState gameState;
    private final GameView view;
    private final MoveExecutor moveExecutor;
    private List<Player> roundOrder;

    public TurnOrderManager(GameState gameState, GameView view, MoveExecutor moveExecutor) {
        if (gameState == null || view == null) {
            throw new IllegalArgumentException("GameState and GameView cannot be null");
        }
        this.gameState = gameState;
        this.view = view;
        this.moveExecutor = moveExecutor;
        this.roundOrder = new ArrayList<>();
    }

    /**
     * Rolls the dice sequentially for each player until a player rolls a six (6).
     * The first player to roll a six begins the game immediately:
     * - The clockwise turn order is established starting from that player.
     * - That player moves their first piece from the Base to their starting square 'X'.
     *
     * @return the winning Player who rolled 6 and begins the game
     * @throws GameEngineException if players are missing or nobody rolls a six within MAX_FIRST_PLAYER_ATTEMPTS rounds
     */
    public Player determineFirstPlayer() {
        Dice dice = gameState.getDice();
        List<Player> allPlayers = gameState.getPlayers();

        if (dice == null) {
            throw new GameEngineException("Dice is not available in GameState");
        }
        if (allPlayers == null || allPlayers.isEmpty()) {
            throw new GameEngineException("No players available to determine the first player");
        }

        System.out.println("Once the First Player is Chosen :");

        int attempts = 0;
        while (attempts++ < MAX_FIRST_PLAYER_ATTEMPTS) {
            for (Player player : allPlayers) {
                int roll = dice.roll();
                view.displayInitialRoll(player, roll);

                if (roll == 6) {
                    // 1. Establish clockwise round order starting from this player
                    this.roundOrder = buildClockwiseOrder(player, allPlayers);
                    view.displayFirstPlayerChosen(player, this.roundOrder);

                    // 2. Move first piece from Base to Starting Square 'X' and Toss Coin (Step 2 / Rule T-1)
                    List<Piece> piecesInBase = player.getPiecesInBase();
                    if (piecesInBase == null || piecesInBase.isEmpty()) {
                        throw new GameEngineException(
                                "No pieces in base for " + player.getColor() + " at game start");
                    }
                    if (moveExecutor != null) {
                        moveExecutor.deployPieceFromBaseToX(player, piecesInBase.get(0));
                    }

                    return player;
                }
            }
        }

        throw new GameEngineException(
                "No player rolled a six after " + MAX_FIRST_PLAYER_ATTEMPTS + " rounds; check the Dice implementation");
    }

    /**
     * Builds the clockwise round order starting from the winning player:
     * [winner], [winner + left], [winner + 2left], [winner + 3left]
     */
    public List<Player> buildClockwiseOrder(Player startingPlayer, List<Player> standardOrder) {
        if (startingPlayer == null || standardOrder == null || standardOrder.isEmpty()) {
            throw new IllegalArgumentException("Starting player and player list must be non-empty");
        }

        int startIndex = standardOrder.indexOf(startingPlayer);
        if (startIndex < 0) {
            throw new IllegalArgumentException("Starting player is not part of the player list");
        }

        int total = standardOrder.size();
        List<Player> order = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            order.add(standardOrder.get((startIndex + i) % total));
        }

        return order;
    }

    public List<Player> getRoundOrder() {
        return roundOrder;
    }

    public void setRoundOrder(List<Player> roundOrder) {
        this.roundOrder = (roundOrder != null) ? roundOrder : new ArrayList<>();
    }
}
