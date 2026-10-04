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
 * Strategy implementation for the Yellow Player.
 * - Always prioritizes winning and reaching Home.
 * - Base management: Always empties the base on rolling a 6 (moves piece to
 * 'X').
 * - Capturing: Prioritizes captures only for pieces that still need a capture
 * (captureCount == 0)
 * to fulfill Rule T-7 for entering the Home Straight.
 * - Progress: When no necessary captures can be made, moves the piece closest
 * to its home.
 */
public class YellowWinningStrategy implements PlayerStrategy {

    @Override
    public Move selectMove(Player player, List<Move> legalMoves, Board board, int diceRoll) {
        if (legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }

        // Categorize available moves
        List<Move> baseMoves = legalMoves.stream().filter(Move::isFromBase).toList();

        // 1. Roll is 6 & Pieces in Base:
        // "Yellow always likes to keep an empty base. Therefore, anytime a six is
        // thrown,
        // if there are any pieces in the base, they will be moved to X."
        if (diceRoll == 6 && !baseMoves.isEmpty()) {
            return baseMoves.get(0);
        }

        // 2. Prioritize pieces that need captures first (Rule T-7 qualification)
        // "If no piece is in the base or has rolled a value that cannot transfer a
        // piece from base to X,
        // Yellow will prioritise the pieces that need captures first to see whether any
        // opponent piece is within range."
        List<Move> necessaryCaptures = legalMoves.stream()
                .filter(Move::isCapture)
                .filter(m -> m.getPiece().getCaptureCount() == 0)
                .toList();

        if (!necessaryCaptures.isEmpty()) {
            // If multiple pieces need captures, prioritize the one closest to home
            return necessaryCaptures.stream()
                    .min(Comparator.comparingInt(m -> getDistanceToHome(m.getPiece(), board)))
                    .orElse(necessaryCaptures.get(0));
        }

        // 3. Move the piece closest to its home
        // "In case no captures could be done, Yellow moves the piece closest to its
        // home by the number specified in the roll."
        return legalMoves.stream()
                .min(Comparator.comparingInt(m -> getDistanceToHome(m.getPiece(), board)))
                .orElse(legalMoves.get(0));
    }

    /**
     * Calculates the distance of a Yellow piece to its home.
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

        // In home straight (yellowhomepath0 to yellowhomepath4)
        if (cell.getType() == CellType.HOME_PATH) {
            String id = cell.getId();
            try {
                int pathIndex = Integer.parseInt(id.replaceAll("\\D+", ""));
                return 5 - pathIndex; // Steps to reach Home
            } catch (NumberFormatException e) {
                return 2;
            }
        }

        // On standard path: steps to Yellow Approach (cell 51) + 6 steps to Home
        int approachIdx = board.getApproachIndex(PlayerColor.YELLOW); // 51
        try {
            int currentIdx = Integer.parseInt(cell.getId());
            int stepsToApproach = (approachIdx - currentIdx + Board.TOTAL_STANDARD_CELLS) % Board.TOTAL_STANDARD_CELLS;
            return stepsToApproach + 6;
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
