package com.ludo.strategy;

import com.ludo.enums.CellType;
import com.ludo.enums.PlayerColor;
import com.ludo.model.Board;
import com.ludo.model.Cell;
import com.ludo.model.Move;
import com.ludo.model.Piece;
import com.ludo.model.Player;

import java.util.Comparator;
import java.util.List;

/**
 * Strategy implementation for the Green Player.
 * - Prioritizes winning through defensive blocking.
 * - Only captures when required to enter the home straight (Rule T-7: piece has
 * 0 captures).
 * - Base management on roll 6: Always deploys from base to 'X' UNLESS moving 6
 * enables
 * creating a block.
 * - Protects blocks: prioritizes moving other pieces home or moving the block
 * together (Rule T-4).
 * - Breaking a block is strictly a last resort when pieces in front cannot use
 * the roll.
 */
public class GreenBlockerStrategy implements PlayerStrategy {

    @Override
    public Move selectMove(Player player, List<Move> legalMoves, Board board, int diceRoll) {
        if (legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }

        // Categorize available moves
        List<Move> blockCreatingMoves = legalMoves.stream().filter(Move::createsBlock).toList();
        List<Move> baseMoves = legalMoves.stream().filter(Move::isFromBase).toList();
        List<Move> blockMovesT4 = legalMoves.stream().filter(Move::isBlockMove).toList();
        List<Move> nonBreakingMoves = legalMoves.stream().filter(m -> !m.breaksBlock()).toList();

        // 1. Roll is 6:
        // "any pieces in the base will be moved to X whenever a six is thrown,
        // if there are any pieces in the base unless moving six cells enables green to
        // create a block."
        if (diceRoll == 6) {
            // A. Check if moving 6 creates a block
            if (!blockCreatingMoves.isEmpty()) {
                return blockCreatingMoves.get(0);
            }
            // B. Otherwise, deploy a piece from base to 'X' if any are in base
            if (!baseMoves.isEmpty()) {
                return baseMoves.get(0);
            }
        }

        // 2. Mandatory captures for Home Straight qualification (Rule T-7)
        // Green will not look to capture opponent pieces more than what is required to
        // enter home straight.
        List<Move> necessaryCaptures = legalMoves.stream()
                .filter(Move::isCapture)
                .filter(m -> m.getPiece().getCaptureCount() == 0)
                .toList();
        if (!necessaryCaptures.isEmpty()) {
            return necessaryCaptures.get(0);
        }

        // 3. Creating blocks on any roll (Green prioritizes winning by blocking)
        if (!blockCreatingMoves.isEmpty()) {
            return blockCreatingMoves.get(0);
        }

        // 4. Moving other pieces home or advancing block as a unit (Rule T-4) before
        // breaking a block
        if (!nonBreakingMoves.isEmpty()) {
            // If block move is available, Green attempts to move forward using block move
            if (!blockMovesT4.isEmpty()) {
                return blockMovesT4.get(0);
            }

            // Otherwise, prioritize moving pieces that are closest to home
            return nonBreakingMoves.stream()
                    .min(Comparator.comparingInt(m -> getDistanceToHome(m.getPiece(), board)))
                    .orElse(nonBreakingMoves.get(0));
        }

        // 5. Breaking a block (Last Resort):
        // Only done when pieces in front cannot use the roll and no non-breaking moves
        // exist
        return legalMoves.get(0);
    }

    /**
     * Calculates the distance of a piece to its home.
     * Lower value = closer to home.
     */
    public int getDistanceToHome(Piece piece, Board board) {
        if (piece == null || piece.isInHome()) {
            return 0;
        }

        Cell cell = piece.getCurrentCell();
        if (cell == null || piece.isInBase()) {
            return 60; // In base
        }

        PlayerColor color = piece.getColor();

        // In home straight
        if (cell.getType() == CellType.HOME_PATH) {
            String id = cell.getId();
            try {
                int pathIndex = Integer.parseInt(id.replaceAll("\\D+", ""));
                return 5 - pathIndex; // Steps to home
            } catch (NumberFormatException e) {
                return 2;
            }
        }

        // On standard path
        int approachIdx = board.getApproachIndex(color);
        try {
            int currentIdx = Integer.parseInt(cell.getId());
            int stepsToApproach = (approachIdx - currentIdx + Board.TOTAL_STANDARD_CELLS) % Board.TOTAL_STANDARD_CELLS;
            return stepsToApproach + 6;
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
