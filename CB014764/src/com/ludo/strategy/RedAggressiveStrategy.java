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
 * Strategy implementation for the Red Player.
 * - Extremely aggressive: prioritizes capturing opponent pieces over winning.
 * - When rolling 6: will NOT deploy from base if an existing piece can capture.
 * Only deploys from base to 'X' if no capture is possible with 6.
 * - If multiple captures are possible, prioritizes the opponent piece closest
 * to its home.
 * - Always avoids creating blocks unless unavoidable.
 */
public class RedAggressiveStrategy implements PlayerStrategy {

    @Override
    public Move selectMove(Player player, List<Move> legalMoves, Board board, int diceRoll) {
        if (legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }

        // Separate moves into tactical categories
        List<Move> captures = legalMoves.stream().filter(Move::isCapture).toList();
        List<Move> baseToXMoves = legalMoves.stream().filter(Move::isFromBase).toList();
        List<Move> nonBlockMoves = legalMoves.stream().filter(m -> !m.createsBlock()).toList();

        // 1. Handling Dice Roll = 6:
        // Red keeps one piece on the path and will NOT deploy from base
        // unless it cannot capture any piece by moving 6 cells.
        if (diceRoll == 6) {
            if (!captures.isEmpty()) {
                return chooseBestCapture(captures, board);
            }
            // Cannot capture with 6 -> deploy a piece from base to 'X' if available
            if (!baseToXMoves.isEmpty()) {
                // If deploying to X creates a block, only do it if non-block move isn't
                // available
                return baseToXMoves.get(0);
            }
        }

        // 2. For any other roll: If any opponent piece can be captured, prioritize
        // capturing!
        if (!captures.isEmpty()) {
            return chooseBestCapture(captures, board);
        }

        // 3. Red avoids creating blocks unless it is unavoidable
        if (!nonBlockMoves.isEmpty()) {
            // Pick the first valid non-blocking move
            return nonBlockMoves.get(0);
        }

        // 4. If creating a block is completely unavoidable, take any legal move
        return legalMoves.get(0);
    }

    /**
     * If more than one piece can be captured, Red prioritizes capturing
     * the opponent piece that is closest to its home.
     */
    private Move chooseBestCapture(List<Move> captures, Board board) {
        return captures.stream()
                .min(Comparator.comparingInt(m -> getOpponentDistanceToHome(m.getCapturedPiece(), board)))
                .orElse(captures.get(0));
    }

    /**
     * Calculates the distance of an opponent piece to its home.
     * Lower value = closer to home.
     */
    public int getOpponentDistanceToHome(Piece opponentPiece, Board board) {
        if (opponentPiece == null) {
            return Integer.MAX_VALUE;
        }

        Cell cell = opponentPiece.getCurrentCell();
        if (cell == null || cell.getType() == CellType.BASE) {
            return 58; // In base: furthest away
        }

        PlayerColor opponentColor = opponentPiece.getColor();

        // If in home straight, calculate remaining steps to home
        if (cell.getType() == CellType.HOME_PATH) {
            // e.g. "bluehomepath3" -> extract index 3
            String id = cell.getId();
            try {
                int pathIndex = Integer.parseInt(id.replaceAll("\\D+", ""));
                return 5 - pathIndex; // cells to home
            } catch (NumberFormatException e) {
                return 3;
            }
        }

        // If on standard track: calculate distance to color's approach cell + 6 (home
        // straight + home)
        int approachIdx = board.getApproachIndex(opponentColor);
        try {
            int currentIdx = Integer.parseInt(cell.getId());
            int stepsToApproach = (approachIdx - currentIdx + Board.TOTAL_STANDARD_CELLS) % Board.TOTAL_STANDARD_CELLS;
            return stepsToApproach + 6; // steps to finish
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
