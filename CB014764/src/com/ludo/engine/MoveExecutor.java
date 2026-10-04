package com.ludo.engine;

import com.ludo.enums.CellType;
import com.ludo.execptions.GameEngineException;
import com.ludo.model.Board;
import com.ludo.model.Cell;
import com.ludo.model.GameState;
import com.ludo.model.Move;
import com.ludo.model.Piece;
import com.ludo.model.Player;
import com.ludo.view.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MoveExecutor {

    private static final Logger LOGGER = Logger.getLogger(MoveExecutor.class.getName());

    private final GameState gameState;
    private final GameView view;
    private final TeleportHandler teleportHandler;

    public MoveExecutor(GameState gameState, GameView view, TeleportHandler teleportHandler) {
        if (gameState == null || view == null) {
            throw new IllegalArgumentException("GameState and GameView cannot be null");
        }
        this.gameState = gameState;
        this.view = view;
        this.teleportHandler = (teleportHandler != null) ? teleportHandler : new TeleportHandler(gameState, view);
    }

    public boolean deployPieceFromBaseToX(Player player, Piece piece) {
        if (player == null || piece == null) {
            throw new IllegalArgumentException("Player and Piece cannot be null");
        }

        Board board = gameState.getBoard();
        Cell startCell = board.getStartingCell(player.getColor());
        Cell baseCellOfPlayer = board.getBaseCell(player.getColor());

        if (startCell == null) {
            throw new GameEngineException("No starting cell for " + player.getColor());
        }
        if (baseCellOfPlayer == null) {
            throw new GameEngineException("No base cell for " + player.getColor());
        }

        Piece opponent = startCell.getOccupyingPieces().stream()
                .filter(p -> p.getColor() != player.getColor())
                .findFirst().orElse(null);

        boolean wasCapture = false;
        Piece capturedPiece = null;
        if (opponent != null && !startCell.isBlock()) {
            startCell.removePiece(opponent);
            Cell opponentBase = board.getBaseCell(opponent.getColor());
            if (opponentBase == null) {
                throw new GameEngineException("No base cell for " + opponent.getColor());
            }
            opponent.reset(opponentBase);
            opponentBase.addPiece(opponent);
            piece.incrementCaptureCount();
            wasCapture = true;
            capturedPiece = opponent;
        }

        piece.setInBase(false);
        piece.setCurrentCell(startCell);
        baseCellOfPlayer.removePiece(piece);
        startCell.addPiece(piece);

        if (wasCapture) {
            Player opponentPlayer = gameState.getPlayer(capturedPiece.getColor());
            view.displayCapture(player, piece, startCell, opponentPlayer, capturedPiece);
        } else {
            view.displayBaseToStart(player, piece,
                    player.getPiecesOnBoard().size(),
                    player.getPiecesInBase().size());
        }

        if (startCell.isBlock()) {
            view.displayBlockCreated(player, piece, startCell, startCell.getOccupyingPieces());
        }

        if (gameState.getCoin() == null) {
            throw new GameEngineException("Coin is not available in GameState");
        }
        com.ludo.enums.CoinSide toss = gameState.getCoin().toss();
        com.ludo.enums.Direction direction = toss.toDirection();
        piece.setDirection(direction);
        piece.setOriginalDirection(direction);

        view.displayCoinToss(piece, toss, direction);

        return wasCapture;
    }

    /**
     * Handles Rule T-6 / Step 10:
     * When a player with a blockade rolls six consecutively three (3) times:
     * - The blockade has to be broken by removing all pieces, baring one (one
     * remains).
     * - Every piece except one moves exactly 6 cells in its own original direction
     * which was decided during the coin toss when the piece was in the X position.
     * - The dice is passed to the next player.
     */
    public void breakBlockadeOnThreeSixes(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        Board board = gameState.getBoard();
        List<Cell> blockades = board.getBlockades(player.getColor());

        if (blockades == null || blockades.isEmpty()) {
            LOGGER.warning("breakBlockadeOnThreeSixes called but no blockades found for " + player.getColor());
            view.displayDicePassedToNextPlayer(player);
            return;
        }

        // Copy to avoid ConcurrentModificationException if the board list is live
        for (Cell blockCell : new ArrayList<>(blockades)) {
            if (blockCell == null) {
                continue;
            }

            List<Piece> friendlyPieces = new ArrayList<>(blockCell.getOccupyingPieces().stream()
                    .filter(p -> p.getColor() == player.getColor())
                    .toList());

            if (friendlyPieces.size() < 2) {
                continue;
            }

            // Piece at index 0 remains; all other pieces move exactly 6 cells in their
            // original direction
            Piece remainingPiece = friendlyPieces.get(0);
            List<Piece> movingPieces = new ArrayList<>(friendlyPieces.subList(1, friendlyPieces.size()));

            // Ensure the piece left behind also reverts to its original coin-toss direction
            remainingPiece.setDirection(remainingPiece.getOriginalDirection());

            view.displayBlockadeBrokenByThreeSixes(player, blockCell, movingPieces, remainingPiece);

            for (Piece piece : movingPieces) {
                try {
                    moveBrokenBlockPiece(player, piece, blockCell, board);
                } catch (RuntimeException e) {
                    LOGGER.log(Level.SEVERE,
                            "Failed to move piece out of broken blockade for " + player.getColor(), e);
                    restorePiece(piece, blockCell);
                }
            }
        }

        view.displayDicePassedToNextPlayer(player);
    }

    /**
     * Moves a single piece 6 cells from a broken blockade (Rule T-6).
     */
    private void moveBrokenBlockPiece(Player player, Piece piece, Cell blockCell, Board board) {
        // Revert to original direction decided during coin toss at X
        piece.setDirection(piece.getOriginalDirection());

        Cell from = blockCell;
        from.removePiece(piece);

        List<Cell> path = board.getPath(piece, from, 6);
        if (path == null) {
            path = new ArrayList<>();
        }
        Cell landing = from;

        // Check for opponent blocks along the path (Rule T-3)
        int blockIdx = -1;
        Piece blockingPiece = null;
        for (int i = 0; i < path.size(); i++) {
            Cell stepCell = path.get(i);
            if (stepCell.isBlock() && stepCell.getOccupyingPieces().stream()
                    .anyMatch(p -> p.getColor() != player.getColor())) {
                blockIdx = i;
                blockingPiece = stepCell.getOccupyingPieces().get(0);
                break;
            }
        }

        if (blockIdx != -1) {
            landing = (blockIdx == 0) ? from : path.get(blockIdx - 1);
            view.displayPieceBlocked(piece, from, path.get(path.size() - 1), blockingPiece);
            if (landing == from) {
                view.displayBlockedCannotMove(player);
            } else {
                view.displayBlockedMoveAdvance(player, piece, landing);
            }
        } else if (!path.isEmpty()) {
            landing = board.calculateLandingCell(piece, from, 6, true);
            if (landing == null) {
                landing = path.get(path.size() - 1);
            }
        }

        // Check for opponent piece on landing cell (Rule 6, Rule T-9)
        Piece opponent = landing.getOccupyingPieces().stream()
                .filter(p -> p.getColor() != player.getColor())
                .findFirst().orElse(null);

        boolean wasCapture = false;
        Piece capturedPiece = null;
        if (opponent != null && !landing.isBlock()) {
            Cell opponentBase = board.getBaseCell(opponent.getColor());
            if (opponentBase == null) {
                throw new GameEngineException("No base cell for " + opponent.getColor());
            }
            landing.removePiece(opponent);
            opponent.reset(opponentBase); // Rule T-9
            opponentBase.addPiece(opponent);
            piece.incrementCaptureCount();
            wasCapture = true;
            capturedPiece = opponent;
        }

        piece.setCurrentCell(landing);
        landing.addPiece(piece);

        if (landing.getType() == CellType.HOME) {
            piece.reachHome();
            if (player.hasWon()) {
                gameState.setWinner(player);
                view.displayGameWon(player);
            }
        }

        if (wasCapture) {
            Player opponentPlayer = gameState.getPlayer(capturedPiece.getColor());
            view.displayCapture(player, piece, landing, opponentPlayer, capturedPiece);
        } else if (blockIdx == -1) {
            view.displayPieceMoved(player, piece, from, landing, 6, piece.getDirection());
        }

        if (landing.isBlock()) {
            view.displayBlockCreated(player, piece, landing, landing.getOccupyingPieces());
        }
    }

    /**
     * Executes the chosen move on the board and notifies the view.
     * Returns true if a capture occurred.
     *
     * If the move fails midway, moving pieces are restored to their original cell
     * (best effort) and a GameEngineException is thrown.
     */
    public boolean executeMove(Player player, Move move) {
        if (move == null) {
            return false;
        }
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        // Snapshot for rollback
        Cell originalCell = move.getFromCell();
        List<Piece> movingPieces = new ArrayList<>();
        if (move.isBlockMove() && move.getBlockPieces() != null) {
            movingPieces.addAll(move.getBlockPieces());
        } else if (move.getPiece() != null) {
            movingPieces.add(move.getPiece());
        }

        try {
            return executeMoveInternal(player, move);
        } catch (GameEngineException e) {
            rollbackPieces(movingPieces, originalCell, move.isFromBase());
            throw e;
        } catch (RuntimeException e) {
            rollbackPieces(movingPieces, originalCell, move.isFromBase());
            throw new GameEngineException("executeMove failed for " + player.getColor(), e);
        }
    }

    private void rollbackPieces(List<Piece> pieces, Cell originalCell, boolean fromBase) {
        if (fromBase || originalCell == null) {
            return; // nothing reliable to restore to
        }
        for (Piece p : pieces) {
            try {
                restorePiece(p, originalCell);
            } catch (RuntimeException rollbackError) {
                LOGGER.log(Level.SEVERE, "Rollback failed for a piece", rollbackError);
            }
        }
    }

    /**
     * Best-effort: puts a piece back on the given cell and removes it from any
     * other cell it may currently be recorded on.
     */
    public void restorePiece(Piece piece, Cell cell) {
        if (piece == null || cell == null) {
            return;
        }
        Cell current = piece.getCurrentCell();
        if (current != null && current != cell) {
            current.removePiece(piece);
        }
        piece.setCurrentCell(cell);
        if (!cell.getOccupyingPieces().contains(piece)) {
            cell.addPiece(piece);
        }
    }

    private boolean executeMoveInternal(Player player, Move move) {
        Piece piece = move.getPiece();
        Board board = gameState.getBoard();

        if (move.isFromBase()) {
            if (piece == null) {
                throw new GameEngineException("Base move has no piece");
            }
            return deployPieceFromBaseToX(player, piece);
        }

        Cell from = move.getFromCell();
        Cell to = move.getToCell();

        // Handle blocked move notifications (Rule T-3, Section 3.1)
        if (move.isBlocked()) {
            view.displayPieceBlocked(piece, from, move.getIntendedTargetCell(), move.getBlockingPiece());
            if (to == from || to == null) {
                view.displayBlockedCannotMove(player);
                return false;
            } else {
                view.displayBlockedMoveAdvance(player, piece, to);
            }
        }

        // Validate before mutating any state
        if (from == null) {
            throw new GameEngineException("Move has no source cell");
        }
        if (to == null) {
            throw new GameEngineException("Move has no destination cell");
        }
        if (!move.isBlockMove() && piece == null) {
            throw new GameEngineException("Move has no piece");
        }
        if (move.isBlockMove() && (move.getBlockPieces() == null || move.getBlockPieces().isEmpty())) {
            throw new GameEngineException("Block move has no pieces");
        }

        if (from != to) {
            boolean startedAtApproach = (from == board.getApproachCell(player.getColor()));
            if (move.isBlockMove()) {
                if (move.getBlockDirection() == com.ludo.enums.Direction.COUNTER_CLOCKWISE) {
                    Cell curr = from;
                    for (int i = 0; i < 52; i++) {
                        if (curr == to)
                            break;
                        curr = board.getNextCellForDirection(curr, move.getBlockDirection(), player.getColor(),
                                move.getBlockPieces(), true, startedAtApproach);
                        if (curr == null)
                            break;
                    }
                }
            } else {
                if (piece.getDirection() == com.ludo.enums.Direction.COUNTER_CLOCKWISE) {
                    Cell curr = from;
                    for (int i = 0; i < 52; i++) {
                        if (curr == to)
                            break;
                        curr = board.getNextCell(piece, curr, true, startedAtApproach);
                        if (curr == null)
                            break;
                    }
                }
            }
        }

        if (move.isBlockMove()) {
            List<Piece> blockPieces = move.getBlockPieces();
            for (Piece p : blockPieces) {
                from.removePiece(p);
            }

            boolean wasCapture = false;
            if (move.isCapture() && move.getCapturedBlockPieces() != null
                    && !move.getCapturedBlockPieces().isEmpty()) {
                List<Piece> capturedPieces = move.getCapturedBlockPieces();
                Player opponentPlayer = gameState.getPlayer(capturedPieces.get(0).getColor());
                Cell opponentBase = board.getBaseCell(opponentPlayer.getColor());
                if (opponentBase == null) {
                    throw new GameEngineException("No base cell for " + opponentPlayer.getColor());
                }

                for (Piece cap : capturedPieces) {
                    to.removePiece(cap);
                    cap.reset(opponentBase); // Rule T-9: All information in that piece will be reset
                    opponentBase.addPiece(cap);
                }

                // Rule T-8: The number of captures for each piece participating in the captured
                // block will be incremented by one (1)
                for (Piece p : blockPieces) {
                    p.incrementCaptureCount();
                }

                wasCapture = true;
                if (capturedPieces.size() > 1) {
                    for (Piece p : blockPieces) {
                        p.incrementBlockCaptureCount();
                    }
                    view.displayBlockadeCaptured(player, blockPieces, to, opponentPlayer, capturedPieces);
                } else {
                    view.displayCapture(player, blockPieces.get(0), to, opponentPlayer, capturedPieces.get(0));
                }
            }

            for (Piece p : blockPieces) {
                p.setCurrentCell(to);
                p.setDirection(move.getBlockDirection());
                to.addPiece(p);
            }

            if (to.getType() == CellType.HOME) {
                for (Piece p : blockPieces) {
                    p.reachHome();
                }
                if (player.hasWon()) {
                    gameState.setWinner(player);
                    view.displayGameWon(player);
                }
            }

            if (!wasCapture) {
                view.displayBlockMoved(player, blockPieces, from, to, move.getRollValue(), move.getBlockSteps(),
                        move.getBlockDirection());
            }

            // Check if landing on a Mystery Cell (Rule T-11 / Step 13)
            if (to.isMysteryCell() || to == board.getMysteryCell()) {
                teleportHandler.handleMysteryCellBlockLanding(player, blockPieces, null);
            }

            return wasCapture;
        }

        // If an individual piece breaks away from a block, restore original direction
        // (Rule T-5)
        if (move.breaksBlock()) {
            piece.setDirection(piece.getOriginalDirection());

            // Revert any pieces left behind in the broken block to their original
            // directions
            for (Piece p : from.getOccupyingPieces()) {
                if (p.getColor() == piece.getColor() && p != piece) {
                    p.setDirection(p.getOriginalDirection());
                }
            }
        }

        // 1. Remove piece from source
        from.removePiece(piece);

        // 2. Handle capture if applicable (Rule 6, Rule T-9)
        boolean wasCapture = false;
        Piece captured = null;
        if (move.isCapture() && move.getCapturedPiece() != null) {
            captured = move.getCapturedPiece();
            Cell opponentBase = board.getBaseCell(captured.getColor());
            if (opponentBase == null) {
                throw new GameEngineException("No base cell for " + captured.getColor());
            }
            to.removePiece(captured);
            captured.reset(opponentBase); // Rule T-9: reset all information upon return to base
            opponentBase.addPiece(captured);
            piece.incrementCaptureCount();
            wasCapture = true;
        }

        // 3. Place piece on destination
        piece.setCurrentCell(to);
        to.addPiece(piece);

        if (to.getType() == CellType.HOME) {
            piece.reachHome();
            if (player.hasWon()) {
                gameState.setWinner(player);
                view.displayGameWon(player);
            }
        }

        // 4. Display action (Section 3.1)
        if (wasCapture) {
            Player opponentPlayer = gameState.getPlayer(captured.getColor());
            view.displayCapture(player, piece, to, opponentPlayer, captured);
        } else if (!move.isBlocked()) {
            view.displayPieceMoved(player, piece, from, to, move.getMovementSteps(), piece.getDirection());
        }

        // 5. Notify if a block was created/joined (Rule T-3)
        if (move.createsBlock()) {
            view.displayBlockCreated(player, piece, to, to.getOccupyingPieces());
        }

        // 6. Check if landing on a Mystery Cell (Rule T-11 / Step 13)
        if (to.isMysteryCell() || to == board.getMysteryCell()) {
            teleportHandler.handleMysteryCellLanding(player, piece, null);
        }

        return wasCapture;
    }
}
