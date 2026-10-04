package com.ludo.engine;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.execptions.MoveGeneratorException;
import com.ludo.model.*;
import com.ludo.state.BriefingState;
import com.ludo.state.EnergizedState;
import com.ludo.state.SickState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoveGenerator {

    public static List<Move> generateLegalMoves(Player player, Board board, int diceRoll) {
        if (player == null) {
            throw new IllegalArgumentException("Player must not be null");
        }
        if (board == null) {
            throw new IllegalArgumentException("Board must not be null");
        }
        if (diceRoll < 1 || diceRoll > 6) {
            throw new IllegalArgumentException("Invalid dice roll: " + diceRoll + " (expected 1–6)");
        }

        try {
            return doGenerateLegalMoves(player, board, diceRoll);
        } catch (MoveGeneratorException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MoveGeneratorException(
                    "Move generation failed for " + player.getColor() + " with roll " + diceRoll, e);
        }
    }

    // ---------------------------------------------------------------------------
    // Private implementation — logic is unchanged from the original
    // ---------------------------------------------------------------------------

    private static List<Move> doGenerateLegalMoves(Player player, Board board, int diceRoll) {
        List<Move> legalMoves = new ArrayList<>();
        PlayerColor color = player.getColor();

        // 1. Move from Base to starting square 'X' (Requires rolling a 6)
        if (diceRoll == 6 && player.hasPiecesInBase()) {
            Cell startCell = board.getStartingCell(color);
            if (startCell == null) {
                throw new MoveGeneratorException("No starting cell found for color: " + color);
            }
            Piece firstBasePiece = player.getPiecesInBase().get(0);

            // Check if starting cell is blocked by an opponent block
            boolean blockedByOpponentBlock = startCell.isBlock() &&
                    startCell.getOccupyingPieces().stream().anyMatch(p -> p.getColor() != color);

            if (!blockedByOpponentBlock) {
                legalMoves.add(Move.createBaseToStart(firstBasePiece, startCell));
            }
        }

        // 2. Normal piece moves on the board
        for (Piece piece : player.getPiecesOnBoard()) {
            if (!piece.canMove(diceRoll)) {
                continue; // Piece cannot move (e.g. frozen in BriefingState)
            }

            int actualSteps = piece.calculateMovementDistance(diceRoll);
            if (actualSteps <= 0) {
                continue;
            }

            Cell currentCell = piece.getCurrentCell();
            Direction originalDirBefore = piece.getDirection();
            if (currentCell != null && currentCell.isBlock()) {
                piece.setDirection(piece.getOriginalDirection());
            }
            List<Cell> path = board.getPath(piece, currentCell, actualSteps);
            if (currentCell != null && currentCell.isBlock()) {
                piece.setDirection(originalDirBefore);
            }
            if (path.isEmpty()) {
                continue;
            }

            // Check if piece overshot home
            Cell intendedLanding = path.get(path.size() - 1);
            if (path.size() < actualSteps) {
                continue; // Overshot home
            }

            // Check for opponent blocks along the path (Rule T-3: no opponent piece can
            // jump over a block)
            int blockIndex = -1;
            Cell blockingCell = null;
            Piece blockingPiece = null;

            for (int i = 0; i < path.size(); i++) {
                Cell stepCell = path.get(i);
                if (stepCell.isBlock() && stepCell.getOccupyingPieces().stream().anyMatch(p -> p.getColor() != color)) {
                    blockIndex = i;
                    blockingCell = stepCell;
                    blockingPiece = stepCell.getOccupyingPieces().get(0);
                    break; // Block encountered, cannot pass!
                }
            }

            if (blockIndex != -1) {
                // Opponent block encountered along path!
                // Rule T-3: No opponent piece can jump over the block.
                // However, the opponent piece can move up to the adjacent cell before the
                // block.
                Cell toCellBeforeBlock = (blockIndex == 0) ? currentCell : path.get(blockIndex - 1);

                if (toCellBeforeBlock == currentCell) {
                    // Piece was already adjacent to the block and cannot move forward at all
                    legalMoves.add(Move.createBlockedMove(piece, currentCell, currentCell, intendedLanding,
                            blockingPiece, diceRoll, false, null, false));
                } else {
                    // Can advance up to the cell before the block!
                    boolean createsBlock = false;
                    boolean isCapture = false;
                    Piece targetOpponent = null;

                    if (toCellBeforeBlock.isOccupied()) {
                        List<Piece> occupants = toCellBeforeBlock.getOccupyingPieces();
                        boolean hasFriendly = occupants.stream().anyMatch(p -> p.getColor() == color);
                        boolean hasOpponent = occupants.stream().anyMatch(p -> p.getColor() != color);

                        if (hasFriendly) {
                            createsBlock = true;
                        } else if (hasOpponent && !toCellBeforeBlock.isBlock()) {
                            isCapture = true;
                            targetOpponent = occupants.get(0);
                        }
                    }

                    legalMoves.add(Move.createBlockedMove(piece, currentCell, toCellBeforeBlock, intendedLanding,
                            blockingPiece, diceRoll, isCapture, targetOpponent, createsBlock));
                }
            } else {
                // No opponent block along the path! Can advance full distance to
                // intendedLanding.
                boolean createsBlock = false;
                boolean isCapture = false;
                Piece targetOpponent = null;

                if (intendedLanding.getType() == CellType.HOME) {
                    legalMoves.add(
                            Move.createNormalMove(piece, currentCell, intendedLanding, diceRoll, false, null, false));
                    continue;
                }

                if (intendedLanding.isOccupied()) {
                    List<Piece> occupants = intendedLanding.getOccupyingPieces();
                    boolean hasFriendly = occupants.stream().anyMatch(p -> p.getColor() == color);
                    boolean hasOpponent = occupants.stream().anyMatch(p -> p.getColor() != color);

                    if (hasFriendly) {
                        createsBlock = true;
                    } else if (hasOpponent && !intendedLanding.isBlock()) {
                        isCapture = true;
                        targetOpponent = occupants.get(0);
                    }
                }

                legalMoves.add(Move.createNormalMove(piece, currentCell, intendedLanding, diceRoll, actualSteps,
                        isCapture, targetOpponent, createsBlock));
            }
        }

        // 3. Block moves (Rule T-4 & Rule T-8):
        // If a player has a block, all pieces in the block move together by
        // (diceRoll / pieces_in_block) positions in the block's direction.
        List<Cell> checkedBlockCells = new ArrayList<>();
        for (Piece piece : player.getPiecesOnBoard()) {
            Cell cell = piece.getCurrentCell();
            if (cell != null && cell.isBlock() && !checkedBlockCells.contains(cell)) {
                checkedBlockCells.add(cell);

                List<Piece> blockPieces = cell.getOccupyingPieces().stream()
                        .filter(p -> p.getColor() == color)
                        .toList();
                int blockSize = blockPieces.size();
                if (blockSize >= 2) {
                    // Rule T-13: If block pieces are attending a briefing, the block cannot move
                    if (blockPieces.stream()
                            .anyMatch(p -> !p.canMove(diceRoll) || p.getState() instanceof BriefingState)) {
                        continue;
                    }

                    int baseBlockSteps = diceRoll / blockSize;
                    boolean isEnergized = blockPieces.stream().allMatch(p -> p.getState() instanceof EnergizedState);
                    boolean isSick = blockPieces.stream().allMatch(p -> p.getState() instanceof SickState);

                    int blockSteps = baseBlockSteps;
                    if (isEnergized) {
                        blockSteps = baseBlockSteps * 2;
                    } else if (isSick) {
                        blockSteps = baseBlockSteps / 2;
                    }

                    if (blockSteps > 0) {
                        com.ludo.enums.Direction blockDir = cell.getBlockDirection(board, color);

                        // Traverse block trajectory
                        List<Cell> blockPath = new ArrayList<>();
                        Cell curr = cell;
                        boolean blockedIntermediate = false;

                        boolean startedAtApproach = (cell == board.getApproachCell(color));
                        for (int s = 0; s < blockSteps; s++) {
                            curr = board.getNextCellForDirection(curr, blockDir, color, blockPieces, false,
                                    startedAtApproach);
                            if (curr == null) {
                                break;
                            }
                            if (s < blockSteps - 1) {
                                // Rule T-3: Opponent block along intermediate cells prevents jumping
                                if (curr.isBlock()
                                        && curr.getOccupyingPieces().stream().anyMatch(p -> p.getColor() != color)) {
                                    blockedIntermediate = true;
                                    break;
                                }
                            }
                            blockPath.add(curr);
                        }

                        if (!blockedIntermediate && !blockPath.isEmpty() && blockPath.size() == blockSteps) {
                            Cell landingCell = blockPath.get(blockPath.size() - 1);
                            boolean canLand = true;
                            boolean isCapture = false;
                            List<Piece> capturedOpponents = Collections.emptyList();

                            if (landingCell.isOccupied()) {
                                List<Piece> opponentPieces = landingCell.getOccupyingPieces().stream()
                                        .filter(p -> p.getColor() != color)
                                        .toList();

                                if (!opponentPieces.isEmpty()) {
                                    if (landingCell.isBlock()) {
                                        // Rule T-8: A blockade of the SAME SIZE can capture a blockade
                                        if (opponentPieces.size() == blockSize) {
                                            isCapture = true;
                                            capturedOpponents = opponentPieces;
                                        } else {
                                            // Different sizes cannot capture and cannot land on opponent block
                                            canLand = false;
                                        }
                                    } else {
                                        // Blocks cannot capture single pieces, but CAN land on them and share the cell
                                        // isCapture remains false, canLand remains true
                                    }
                                }
                            }

                            if (canLand) {
                                legalMoves.add(Move.createBlockMove(blockPieces, cell, landingCell, diceRoll,
                                        blockSteps, blockDir, isCapture, capturedOpponents));
                            }
                        }
                    }
                }
            }
        }

        return legalMoves;
    }
}
