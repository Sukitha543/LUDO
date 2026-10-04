package com.ludo.model;

import com.ludo.enums.Direction;

import java.util.Collections;
import java.util.List;

/**
 * Represents a proposed or executed move in the game.
 * Encapsulates the moving piece, origin, destination, and combat/blocking consequences.
 */
public class Move {

    private final Piece piece;
    private final Cell fromCell;
    private final Cell toCell;
    private final int rollValue;
    private final boolean fromBase;
    private final boolean isCapture;
    private final Piece capturedPiece;
    private final boolean createsBlock;
    private final boolean breaksBlock;
    private final boolean isBlockMove;
    private final boolean isBlocked;
    private final Piece blockingPiece;
    private final Cell intendedTargetCell;
    private final List<Piece> blockPieces;
    private final Direction blockDirection;
    private final int blockSteps;
    private final int steps;
    private final List<Piece> capturedBlockPieces;

    public Move(Piece piece, Cell fromCell, Cell toCell, int rollValue, int steps,
                boolean fromBase, boolean isCapture, Piece capturedPiece,
                boolean createsBlock, boolean breaksBlock, boolean isBlockMove,
                boolean isBlocked, Piece blockingPiece, Cell intendedTargetCell,
                List<Piece> blockPieces, Direction blockDirection, int blockSteps,
                List<Piece> capturedBlockPieces) {
        this.piece = piece;
        this.fromCell = fromCell;
        this.toCell = toCell;
        this.rollValue = rollValue;
        this.steps = steps > 0 ? steps : (isBlockMove ? blockSteps : rollValue);
        this.fromBase = fromBase;
        this.isCapture = isCapture;
        this.capturedPiece = capturedPiece;
        this.createsBlock = createsBlock;
        this.breaksBlock = breaksBlock;
        this.isBlockMove = isBlockMove;
        this.isBlocked = isBlocked;
        this.blockingPiece = blockingPiece;
        this.intendedTargetCell = intendedTargetCell;
        this.blockPieces = (blockPieces != null) ? Collections.unmodifiableList(blockPieces) : null;
        this.blockDirection = blockDirection;
        this.blockSteps = blockSteps;
        this.capturedBlockPieces = (capturedBlockPieces != null) ? Collections.unmodifiableList(capturedBlockPieces) : Collections.emptyList();
    }

    public Move(Piece piece, Cell fromCell, Cell toCell, int rollValue,
                boolean fromBase, boolean isCapture, Piece capturedPiece,
                boolean createsBlock, boolean breaksBlock, boolean isBlockMove,
                boolean isBlocked, Piece blockingPiece, Cell intendedTargetCell,
                List<Piece> blockPieces, Direction blockDirection, int blockSteps,
                List<Piece> capturedBlockPieces) {
        this(piece, fromCell, toCell, rollValue, isBlockMove ? blockSteps : rollValue, fromBase, isCapture, capturedPiece,
                createsBlock, breaksBlock, isBlockMove, isBlocked, blockingPiece, intendedTargetCell,
                blockPieces, blockDirection, blockSteps, capturedBlockPieces);
    }

    public Move(Piece piece, Cell fromCell, Cell toCell, int rollValue,
                boolean fromBase, boolean isCapture, Piece capturedPiece,
                boolean createsBlock, boolean breaksBlock, boolean isBlockMove,
                boolean isBlocked, Piece blockingPiece, Cell intendedTargetCell,
                List<Piece> blockPieces, Direction blockDirection, int blockSteps) {
        this(piece, fromCell, toCell, rollValue, fromBase, isCapture, capturedPiece,
                createsBlock, breaksBlock, isBlockMove, isBlocked, blockingPiece, intendedTargetCell,
                blockPieces, blockDirection, blockSteps, Collections.emptyList());
    }

    public Move(Piece piece, Cell fromCell, Cell toCell, int rollValue,
                boolean fromBase, boolean isCapture, Piece capturedPiece,
                boolean createsBlock, boolean breaksBlock, boolean isBlockMove,
                boolean isBlocked, Piece blockingPiece, Cell intendedTargetCell) {
        this(piece, fromCell, toCell, rollValue, fromBase, isCapture, capturedPiece,
                createsBlock, breaksBlock, isBlockMove, isBlocked, blockingPiece, intendedTargetCell,
                null, null, 0);
    }

    public Move(Piece piece, Cell fromCell, Cell toCell, int rollValue,
                boolean fromBase, boolean isCapture, Piece capturedPiece,
                boolean createsBlock, boolean breaksBlock, boolean isBlockMove) {
        this(piece, fromCell, toCell, rollValue, fromBase, isCapture, capturedPiece,
                createsBlock, breaksBlock, isBlockMove, false, null, null);
    }

    public Move(Piece piece, Cell fromCell, Cell toCell, int rollValue,
                boolean fromBase, boolean isCapture, Piece capturedPiece, boolean createsBlock) {
        this(piece, fromCell, toCell, rollValue, fromBase, isCapture, capturedPiece, createsBlock,
                (fromCell != null && fromCell.isBlock()), false);
    }

    public static Move createBaseToStart(Piece piece, Cell startCell) {
        boolean formsBlock = startCell.isOccupied() &&
                startCell.getOccupyingPieces().stream().anyMatch(p -> p.getColor() == piece.getColor());
        Piece opponent = startCell.getOccupyingPieces().stream()
                .filter(p -> p.getColor() != piece.getColor())
                .findFirst().orElse(null);
        boolean capture = (opponent != null && !startCell.isBlock());

        return new Move(piece, null, startCell, 6, true, capture, opponent, formsBlock, false, false);
    }

    public static Move createNormalMove(Piece piece, Cell from, Cell to, int roll, int steps,
                                        boolean isCapture, Piece capturedPiece, boolean createsBlock) {
        boolean breaks = (from != null && from.isBlock());
        return new Move(piece, from, to, roll, steps, false, isCapture, capturedPiece, createsBlock, breaks, false,
                false, null, null, null, null, 0, Collections.emptyList());
    }

    public static Move createNormalMove(Piece piece, Cell from, Cell to, int roll,
                                        boolean isCapture, Piece capturedPiece, boolean createsBlock) {
        return createNormalMove(piece, from, to, roll, roll, isCapture, capturedPiece, createsBlock);
    }

    public static Move createBlockedMove(Piece piece, Cell from, Cell toCellBeforeBlock,
                                         Cell intendedTarget, Piece blockingPiece, int roll,
                                         boolean isCapture, Piece capturedPiece, boolean createsBlock) {
        boolean breaks = (from != null && from.isBlock());
        return new Move(piece, from, toCellBeforeBlock, roll, false, isCapture, capturedPiece,
                createsBlock, breaks, false, true, blockingPiece, intendedTarget);
    }

    public static Move createBlockMove(List<Piece> blockPieces, Cell from, Cell to, int roll, int steps, Direction direction,
                                        boolean isCapture, List<Piece> capturedBlockPieces) {
        Piece representativePiece = blockPieces.get(0);
        Piece primaryCaptured = (capturedBlockPieces != null && !capturedBlockPieces.isEmpty()) ? capturedBlockPieces.get(0) : null;
        return new Move(representativePiece, from, to, roll, false, isCapture, primaryCaptured, false, false, true, false, null, null,
                blockPieces, direction, steps, capturedBlockPieces);
    }

    public static Move createBlockMove(List<Piece> blockPieces, Cell from, Cell to, int roll, int steps, Direction direction) {
        return createBlockMove(blockPieces, from, to, roll, steps, direction, false, Collections.emptyList());
    }

    public static Move createBlockMove(Piece representativePiece, Cell from, Cell to, int roll) {
        return new Move(representativePiece, from, to, roll, false, false, null, false, false, true);
    }

    public Piece getPiece() {
        return piece;
    }

    public Cell getFromCell() {
        return fromCell;
    }

    public Cell getToCell() {
        return toCell;
    }

    public int getRollValue() {
        return rollValue;
    }

    public boolean isFromBase() {
        return fromBase;
    }

    public boolean isCapture() {
        return isCapture;
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    public boolean createsBlock() {
        return createsBlock;
    }

    public boolean breaksBlock() {
        return breaksBlock;
    }

    public boolean isBlockMove() {
        return isBlockMove;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public Piece getBlockingPiece() {
        return blockingPiece;
    }

    public Cell getIntendedTargetCell() {
        return intendedTargetCell;
    }

    public List<Piece> getBlockPieces() {
        return (blockPieces != null) ? blockPieces : List.of(piece);
    }

    public Direction getBlockDirection() {
        return (blockDirection != null) ? blockDirection : piece.getDirection();
    }

    public int getBlockSteps() {
        return blockSteps;
    }

    public int getMovementSteps() {
        return (steps > 0) ? steps : (isBlockMove ? blockSteps : rollValue);
    }

    public List<Piece> getCapturedBlockPieces() {
        return capturedBlockPieces;
    }

    public boolean isBlockCapture() {
        return isBlockMove && isCapture && !capturedBlockPieces.isEmpty();
    }

    @Override
    public String toString() {
        String fromId = (fromCell == null) ? "Base" : fromCell.getId();
        String toId = (toCell == null) ? "Home" : toCell.getId();
        String flags = (isCapture ? " [CAPTURE " + capturedPiece.getName() + "]" : "")
                + (createsBlock ? " [CREATE BLOCK]" : "")
                + (breaksBlock ? " [BREAK BLOCK]" : "")
                + (isBlockMove ? " [BLOCK MOVE T-4]" : "")
                + (fromBase ? " [FROM BASE]" : "");
        return piece.getName() + ": " + fromId + " -> " + toId + " (roll: " + rollValue + ")" + flags;
    }
}
