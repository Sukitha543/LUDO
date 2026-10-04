package com.ludo.engine;

import com.ludo.enums.TeleportDestination;
import com.ludo.execptions.GameEngineException;
import com.ludo.model.Board;
import com.ludo.model.Cell;
import com.ludo.model.GameState;
import com.ludo.model.Piece;
import com.ludo.model.Player;
import com.ludo.view.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Handles Mystery Cell landing, teleportation logic, and associated special cell effects
 * (Alpha aura, Beta briefing, Gamma redirection, and Beta 3-consecutive-threes release).
 * Follows Single Responsibility Principle (SRP) by isolating teleportation rules.
 */
public class TeleportHandler {

    private static final Logger LOGGER = Logger.getLogger(TeleportHandler.class.getName());

    private final GameState gameState;
    private final GameView view;
    private final Random random;

    public TeleportHandler(GameState gameState, GameView view) {
        this(gameState, view, com.ludo.util.GameRandom.getInstance());
    }

    public TeleportHandler(GameState gameState, GameView view, Random random) {
        if (gameState == null || view == null) {
            throw new IllegalArgumentException("GameState and GameView cannot be null");
        }
        this.gameState = gameState;
        this.view = view;
        this.random = (random != null) ? random : com.ludo.util.GameRandom.getInstance();
    }

    /**
     * Handles Rule T-11 / Step 13:
     * If a piece lands on the Mystery cell, it randomly selects one of the six
     * options below and teleports the piece into the specified location:
     * 1. Alpha
     * 2. Beta
     * 3. Gamma
     * 4. Base
     * 5. X of the piece colour
     * 6. Approach of the piece colour
     */
    public TeleportDestination handleMysteryCellLanding(Player player, Piece piece, TeleportDestination destination) {
        return handleMysteryCellLanding(player, piece, destination, null);
    }

    public TeleportDestination handleMysteryCellLanding(Player player, Piece piece, TeleportDestination destination,
            Boolean forceEnergized) {
        if (player == null || piece == null) {
            throw new IllegalArgumentException("Player and Piece cannot be null");
        }

        Board board = gameState.getBoard();
        Cell current = piece.getCurrentCell();

        if (destination == null) {
            TeleportDestination[] allDestinations = TeleportDestination.values();
            destination = allDestinations[random.nextInt(allDestinations.length)];
        }

        Cell targetCell = board.getTeleportCell(destination, player.getColor());
        if (targetCell == null && destination != TeleportDestination.BASE) {
            throw new GameEngineException("No teleport cell for " + destination + " (" + player.getColor() + ")");
        }

        // 1. Remove piece from mystery cell
        if (current != null) {
            current.removePiece(piece);
        }

        // 2. Teleport to target
        if (destination == TeleportDestination.BASE) {
            Cell baseCell = board.getBaseCell(player.getColor());
            if (baseCell == null) {
                throw new GameEngineException("No base cell for " + player.getColor());
            }
            piece.reset(baseCell);
            baseCell.addPiece(piece);
        } else {
            // Check for opponent piece on target cell (Rule 6 capture on landing)
            Piece opponent = targetCell.getOccupyingPieces().stream()
                    .filter(p -> p.getColor() != player.getColor())
                    .findFirst().orElse(null);

            boolean wasCapture = false;
            if (opponent != null && !targetCell.isBlock()) {
                Cell opponentBase = board.getBaseCell(opponent.getColor());
                if (opponentBase == null) {
                    throw new GameEngineException("No base cell for " + opponent.getColor());
                }
                targetCell.removePiece(opponent);
                opponent.reset(opponentBase); // Rule T-9 reset on base return
                opponentBase.addPiece(opponent);
                piece.incrementCaptureCount();
                wasCapture = true;
            }

            piece.setInBase(false);
            piece.setCurrentCell(targetCell);
            targetCell.addPiece(piece);

            if (wasCapture) {
                Player opponentPlayer = gameState.getPlayer(opponent.getColor());
                view.displayCapture(player, piece, targetCell, opponentPlayer, opponent);
            }
        }

        // 3. Notify view (Section 3.1)
        view.displayMysteryCellTeleport(player, piece, destination, targetCell);

        // 4. Rule T-12 & T-13: Alpha/Beta cell effects
        if (destination == TeleportDestination.ALPHA) {
            applyAlphaEffect(player, piece, forceEnergized);
        } else if (destination == TeleportDestination.BETA) {
            applyBetaEffect(player, piece);
        }

        return destination;
    }

    public boolean applyAlphaEffect(Player player, Piece piece, Boolean forceEnergized) {
        if (piece == null) {
            throw new IllegalArgumentException("Piece cannot be null");
        }
        boolean isEnergized = (forceEnergized != null) ? forceEnergized : random.nextBoolean();
        if (isEnergized) {
            piece.energize();
        } else {
            piece.makeSick();
        }
        view.displayPieceAlphaEffect(player, piece, isEnergized);
        return isEnergized;
    }

    public void applyBetaEffect(Player player, Piece piece) {
        if (piece == null) {
            throw new IllegalArgumentException("Piece cannot be null");
        }
        piece.attendBriefing();
        view.displayPieceBetaEffect(player, piece);
    }

    /**
     * Handles Rule T-13 / Step 15:
     * If a piece or block is in Beta (attending briefing and movement-restricted
     * during next four rounds),
     * and the player rolls value three (3) three times in a row, the piece or block
     * is teleported to the player's base.
     */
    public boolean checkBetaConsecutiveThrees(Player player, int roll) {
        Board board = gameState.getBoard();
        Cell betaCell = board.getBetaCell();

        if (betaCell == null) {
            LOGGER.warning("Beta cell is not available on the board; skipping Beta check");
            player.resetConsecutiveThrees();
            return false;
        }

        List<Piece> betaPieces = betaCell.getOccupyingPieces().stream()
                .filter(p -> p.getColor() == player.getColor() && p.getState() instanceof com.ludo.state.BriefingState)
                .toList();

        if (betaPieces.isEmpty()) {
            player.resetConsecutiveThrees();
            return false;
        }

        if (roll == 3) {
            player.incrementConsecutiveThrees();
            if (player.getConsecutiveThrees() == 3) {
                Cell baseCell = board.getBaseCell(player.getColor());
                if (baseCell == null) {
                    throw new GameEngineException("No base cell for " + player.getColor());
                }
                if (betaPieces.size() > 1) {
                    for (Piece p : betaPieces) {
                        betaCell.removePiece(p);
                        p.reset(baseCell);
                        baseCell.addPiece(p);
                    }
                    view.displayBlockBetaTeleportToBase(player, betaPieces);
                } else {
                    Piece p = betaPieces.get(0);
                    betaCell.removePiece(p);
                    p.reset(baseCell);
                    baseCell.addPiece(p);
                    view.displayPieceBetaTeleportToBase(player, p);
                }
                player.resetConsecutiveThrees();
                return true;
            }
        } else {
            player.resetConsecutiveThrees();
        }

        return false;
    }

    /**
     * Handles Rule T-11 / Step 13 for an entire block:
     * When an entire block lands on the Mystery cell, it randomly selects one of
     * the six options below and teleports the ENTIRE BLOCK into the specified location together:
     * 1. Alpha
     * 2. Beta
     * 3. Gamma
     * 4. Base
     * 5. X of the piece colour
     * 6. Approach of the piece colour
     */
    public TeleportDestination handleMysteryCellBlockLanding(Player player, List<Piece> blockPieces,
            TeleportDestination destination) {
        return handleMysteryCellBlockLanding(player, blockPieces, destination, null);
    }

    public TeleportDestination handleMysteryCellBlockLanding(Player player, List<Piece> blockPieces,
            TeleportDestination destination, Boolean forceEnergized) {
        if (blockPieces == null || blockPieces.isEmpty()) {
            return null;
        }
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        Board board = gameState.getBoard();
        Cell current = blockPieces.get(0).getCurrentCell();

        if (destination == null) {
            TeleportDestination[] allDestinations = TeleportDestination.values();
            destination = allDestinations[random.nextInt(allDestinations.length)];
        }

        Cell targetCell = board.getTeleportCell(destination, player.getColor());
        if (targetCell == null && destination != TeleportDestination.BASE) {
            throw new GameEngineException("No teleport cell for " + destination + " (" + player.getColor() + ")");
        }

        // 1. Remove all block pieces from mystery cell
        if (current != null) {
            for (Piece p : blockPieces) {
                current.removePiece(p);
            }
        }

        // 2. Teleport to target
        if (destination == TeleportDestination.BASE) {
            Cell baseCell = board.getBaseCell(player.getColor());
            if (baseCell == null) {
                throw new GameEngineException("No base cell for " + player.getColor());
            }
            for (Piece p : blockPieces) {
                p.reset(baseCell);
                baseCell.addPiece(p);
            }
        } else {
            // Check for opponent pieces on target cell (Rule 6, Rule T-8)
            List<Piece> opponentPieces = new ArrayList<>();
            for (Piece p : targetCell.getOccupyingPieces()) {
                if (p.getColor() != player.getColor()) {
                    opponentPieces.add(p);
                }
            }

            if (!opponentPieces.isEmpty()) {
                // If opponent pieces form a block of the SAME size (Rule T-8) or single piece vs single piece
                if (opponentPieces.size() == blockPieces.size() || opponentPieces.size() == 1) {
                    Player opponentPlayer = gameState.getPlayer(opponentPieces.get(0).getColor());
                    Cell opponentBase = board.getBaseCell(opponentPlayer.getColor());
                    if (opponentBase == null) {
                        throw new GameEngineException("No base cell for " + opponentPlayer.getColor());
                    }

                    for (Piece opp : opponentPieces) {
                        targetCell.removePiece(opp);
                        opp.reset(opponentBase); // Rule T-9 reset on base return
                        opponentBase.addPiece(opp);
                    }

                    // Rule T-8: Each piece participating in the capturing block gets +1 capture count
                    for (Piece p : blockPieces) {
                        p.incrementCaptureCount();
                    }

                    if (opponentPieces.size() > 1) {
                        for (Piece p : blockPieces) {
                            p.incrementBlockCaptureCount();
                        }
                        view.displayBlockadeCaptured(player, blockPieces, targetCell, opponentPlayer, opponentPieces);
                    } else {
                        view.displayCapture(player, blockPieces.get(0), targetCell, opponentPlayer,
                                opponentPieces.get(0));
                    }
                }
            }

            for (Piece p : blockPieces) {
                p.setInBase(false);
                p.setCurrentCell(targetCell);
                targetCell.addPiece(p);
            }
        }

        // 3. Notify view (Section 3.1)
        view.displayMysteryCellBlockTeleport(player, blockPieces, destination, targetCell);

        // 4. Rule T-12 & T-13: Alpha/Beta cell effects for entire block
        if (destination == TeleportDestination.ALPHA) {
            applyAlphaBlockEffect(player, blockPieces, forceEnergized);
        } else if (destination == TeleportDestination.BETA) {
            applyBetaBlockEffect(player, blockPieces);
        }

        return destination;
    }

    public boolean applyAlphaBlockEffect(Player player, List<Piece> blockPieces, Boolean forceEnergized) {
        if (blockPieces == null || blockPieces.isEmpty()) {
            throw new IllegalArgumentException("Block pieces cannot be null or empty");
        }
        boolean isEnergized = (forceEnergized != null) ? forceEnergized : random.nextBoolean();
        for (Piece p : blockPieces) {
            if (isEnergized) {
                p.energize();
            } else {
                p.makeSick();
            }
        }
        view.displayBlockAlphaEffect(player, blockPieces, isEnergized);
        return isEnergized;
    }

    public void applyBetaBlockEffect(Player player, List<Piece> blockPieces) {
        if (blockPieces == null || blockPieces.isEmpty()) {
            throw new IllegalArgumentException("Block pieces cannot be null or empty");
        }
        for (Piece p : blockPieces) {
            p.attendBriefing();
        }
        view.displayBlockBetaEffect(player, blockPieces);
    }
}
