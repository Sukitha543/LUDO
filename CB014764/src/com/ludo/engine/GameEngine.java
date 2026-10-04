package com.ludo.engine;

import com.ludo.enums.TeleportDestination;
import com.ludo.execptions.GameEngineException;
import com.ludo.model.GameState;
import com.ludo.model.Move;
import com.ludo.model.Piece;
import com.ludo.model.Player;
import com.ludo.view.GameView;

import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameEngine {

    private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());
    private static final int MAX_ROUNDS = 5000;

    private final GameState gameState;
    private final GameView view;
    private final MysteryCellManager mysteryCellManager;
    private final TeleportHandler teleportHandler;
    private final MoveExecutor moveExecutor;
    private final TurnOrderManager turnOrderManager;
    private final TurnHandler turnHandler;

    public GameEngine(GameState gameState, GameView view) {
        this(gameState, view, new MysteryCellManager());
    }

    public GameEngine(GameState gameState, GameView view, MysteryCellManager mysteryCellManager) {
        if (gameState == null || view == null) {
            throw new IllegalArgumentException("GameState and GameView cannot be null");
        }
        this.gameState = gameState;
        this.view = view;
        this.mysteryCellManager = (mysteryCellManager != null) ? mysteryCellManager : new MysteryCellManager();

        this.teleportHandler = new TeleportHandler(gameState, view);
        this.moveExecutor = new MoveExecutor(gameState, view, this.teleportHandler);
        this.turnOrderManager = new TurnOrderManager(gameState, view, this.moveExecutor);
        this.turnHandler = new TurnHandler(gameState, view, this.moveExecutor, this.teleportHandler);
    }

    public void setupGame() {
        try {
            view.displayBeforeGameBegins(gameState.getPlayers());
            determineFirstPlayer();
        } catch (GameEngineException e) {
            LOGGER.log(Level.SEVERE, "Game setup failed: " + e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during game setup", e);
            throw new GameEngineException("Game setup failed", e);
        }
    }

    public void runSimulation() {
        if (getRoundOrder() == null || getRoundOrder().isEmpty()) {
            throw new GameEngineException("Round order is empty. Call setupGame() before runSimulation().");
        }

        view.displaySimulationBanner();

        int roundCount = 1;
        try {
            while (!gameState.isGameOver()) {
                view.displayRoundBanner(roundCount);

                executeRound();
                roundCount++;

                if (roundCount > MAX_ROUNDS) {
                    view.displaySimulationTerminated(MAX_ROUNDS);
                    break;
                }
            }
        } catch (GameEngineException e) {
            LOGGER.log(Level.SEVERE, "Simulation aborted in round " + roundCount + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in round " + roundCount + ", simulation aborted", e);
        }
    }

    public void executeRound() {
        for (Player player : getRoundOrder()) {
            try {
                executePlayerTurn(player);
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE,
                        "Turn failed for " + player.getColor().getDisplayName() + ", skipping to next player", e);
            }

            if (gameState.isGameOver()) {
                break;
            }
        }
        endRound();
    }

    public void endRound() {
        try {
            for (Player player : gameState.getPlayers()) {
                for (Piece piece : player.getPieces()) {
                    piece.onRoundEnd();
                }
            }

            mysteryCellManager.updateOnRoundEnd(gameState.getBoard(), view);

            view.displayRoundStatus(gameState.getPlayers(), mysteryCellManager.getActiveMysteryCell(),
                    mysteryCellManager.getRoundsRemaining());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "End-of-round processing failed", e);
        }
    }

    // =========================================================================
    // Delegation to TurnOrderManager
    // =========================================================================

    public Player determineFirstPlayer() {
        return turnOrderManager.determineFirstPlayer();
    }

    public List<Player> buildClockwiseOrder(Player startingPlayer, List<Player> standardOrder) {
        return turnOrderManager.buildClockwiseOrder(startingPlayer, standardOrder);
    }

    public List<Player> getRoundOrder() {
        return turnOrderManager.getRoundOrder();
    }

    // =========================================================================
    // Delegation to TurnHandler
    // =========================================================================

    public void executePlayerTurn(Player player) {
        turnHandler.executePlayerTurn(player);
    }

    public void executePlayerTurn(Player player, Queue<Integer> scriptedRolls) {
        turnHandler.executePlayerTurn(player, scriptedRolls);
    }

    // =========================================================================
    // Delegation to MoveExecutor
    // =========================================================================

    public boolean deployPieceFromBaseToX(Player player, Piece piece) {
        return moveExecutor.deployPieceFromBaseToX(player, piece);
    }

    public boolean executeMove(Player player, Move move) {
        return moveExecutor.executeMove(player, move);
    }

    public void breakBlockadeOnThreeSixes(Player player) {
        moveExecutor.breakBlockadeOnThreeSixes(player);
    }

    // =========================================================================
    // Delegation to TeleportHandler
    // =========================================================================

    public TeleportDestination handleMysteryCellLanding(Player player, Piece piece, TeleportDestination destination) {
        return teleportHandler.handleMysteryCellLanding(player, piece, destination);
    }

    public TeleportDestination handleMysteryCellLanding(Player player, Piece piece, TeleportDestination destination,
            Boolean forceEnergized) {
        return teleportHandler.handleMysteryCellLanding(player, piece, destination, forceEnergized);
    }

    public boolean applyAlphaEffect(Player player, Piece piece, Boolean forceEnergized) {
        return teleportHandler.applyAlphaEffect(player, piece, forceEnergized);
    }

    public void applyBetaEffect(Player player, Piece piece) {
        teleportHandler.applyBetaEffect(player, piece);
    }

    public boolean checkBetaConsecutiveThrees(Player player, int roll) {
        return teleportHandler.checkBetaConsecutiveThrees(player, roll);
    }

    public TeleportDestination handleMysteryCellBlockLanding(Player player, List<Piece> blockPieces,
            TeleportDestination destination) {
        return teleportHandler.handleMysteryCellBlockLanding(player, blockPieces, destination);
    }

    public TeleportDestination handleMysteryCellBlockLanding(Player player, List<Piece> blockPieces,
            TeleportDestination destination, Boolean forceEnergized) {
        return teleportHandler.handleMysteryCellBlockLanding(player, blockPieces, destination, forceEnergized);
    }

    public boolean applyAlphaBlockEffect(Player player, List<Piece> blockPieces, Boolean forceEnergized) {
        return teleportHandler.applyAlphaBlockEffect(player, blockPieces, forceEnergized);
    }

    public void applyBetaBlockEffect(Player player, List<Piece> blockPieces) {
        teleportHandler.applyBetaBlockEffect(player, blockPieces);
    }

    // =========================================================================
    // Getters & Component Accessors
    // =========================================================================

    public GameState getGameState() {
        return gameState;
    }

    public GameView getView() {
        return view;
    }

    public MysteryCellManager getMysteryCellManager() {
        return mysteryCellManager;
    }

    public TurnOrderManager getTurnOrderManager() {
        return turnOrderManager;
    }

    public TurnHandler getTurnHandler() {
        return turnHandler;
    }

    public MoveExecutor getMoveExecutor() {
        return moveExecutor;
    }

    public TeleportHandler getTeleportHandler() {
        return teleportHandler;
    }
}