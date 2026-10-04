package com.ludo.strategy;

import com.ludo.enums.Direction;
import com.ludo.model.Board;
import com.ludo.model.Cell;
import com.ludo.model.Move;
import com.ludo.model.Piece;
import com.ludo.model.Player;

import java.util.List;

/**
 * Strategy implementation for the Blue Player.
 * - Always moves pieces in a cyclic (round-robin) order: B1 -> B2 -> B3 -> B4
 * -> B1...
 * - Prioritizes mystery cells based on piece direction:
 * - If moving Counter-Clockwise: prioritizes landing on the mystery cell.
 * - If moving Clockwise: prioritizes avoiding landing on the mystery cell.
 */
public class BlueCyclicStrategy implements PlayerStrategy {

    private int currentPieceIndex = 0;

    public int getCurrentPieceIndex() {
        return currentPieceIndex;
    }

    public void setCurrentPieceIndex(int currentPieceIndex) {
        this.currentPieceIndex = currentPieceIndex % 4;
    }

    @Override
    public Move selectMove(Player player, List<Move> legalMoves, Board board, int diceRoll) {
        if (legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }

        List<Piece> pieces = player.getPieces();
        int totalPieces = pieces.size(); // usually 4

        // Check pieces in cyclic order starting from currentPieceIndex
        for (int i = 0; i < totalPieces; i++) {
            int candidateIdx = (currentPieceIndex + i) % totalPieces;
            Piece candidatePiece = pieces.get(candidateIdx);

            List<Move> candidateMoves = legalMoves.stream()
                    .filter(m -> m.getPiece() == candidatePiece)
                    .toList();

            if (!candidateMoves.isEmpty()) {
                // Found the next movable piece in cycle!
                Move chosenMove = selectBestMoveForPiece(candidatePiece, candidateMoves, board);

                // Update cyclic index to point to the next piece for the following turn
                this.currentPieceIndex = (candidateIdx + 1) % totalPieces;
                return chosenMove;
            }
        }

        // Fallback: If for any reason no piece matched, pick the first legal move
        return legalMoves.get(0);
    }

    /**
     * Evaluates moves for a specific piece based on its direction and mystery cell
     * preferences.
     */
    private Move selectBestMoveForPiece(Piece piece, List<Move> moves, Board board) {

        Cell mysteryCell = (board != null) ? board.getMysteryCell() : null;

        if (mysteryCell == null) {
            return moves.get(0);
        }

        // 1. Counter-Clockwise: Prioritizes landing on the mystery cell
        if (piece.getDirection() == Direction.COUNTER_CLOCKWISE) {
            List<Move> landingOnMystery = moves.stream()
                    .filter(m -> m.getToCell() == mysteryCell)
                    .toList();

            if (!landingOnMystery.isEmpty()) {
                return landingOnMystery.get(0); // Land on mystery cell!
            }
            return moves.get(0);
        }

        // 2. Clockwise: Prioritizes avoiding landing on the mystery cell
        if (piece.getDirection() == Direction.CLOCKWISE) {
            List<Move> avoidingMystery = moves.stream()
                    .filter(m -> m.getToCell() != mysteryCell)
                    .toList();

            if (!avoidingMystery.isEmpty()) {
                return avoidingMystery.get(0); // Avoid mystery cell!
            }
            return moves.get(0); // Forced to land if it's the only move
        }

        return moves.get(0);
    }
}
