package com.ludo.model;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single cell on the Ludo board.
 * Holds its unique square ID, cell type, optional color association,
 * and tracks the list of pieces currently occupying this cell (supporting blockades).
 */
public class Cell {

    private final String id;
    private final CellType type;
    private final PlayerColor color; // Null for standard white cells
    private final List<Piece> occupyingPieces;
    private boolean mysteryCell;

    public Cell(String id, CellType type, PlayerColor color) {
        this.id = id;
        this.type = type;
        this.color = color;
        this.occupyingPieces = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public CellType getType() {
        return type;
    }

    public PlayerColor getColor() {
        return color;
    }

    public List<Piece> getOccupyingPieces() {
        return Collections.unmodifiableList(occupyingPieces);
    }

    public List<Piece> getPieces() {
        return getOccupyingPieces();
    }

    public void addPiece(Piece piece) {
        if (!occupyingPieces.contains(piece)) {
            occupyingPieces.add(piece);
        }
    }

    public boolean removePiece(Piece piece) {
        return occupyingPieces.remove(piece);
    }

    public void clearPieces() {
        occupyingPieces.clear();
    }

    public boolean isOccupied() {
        return !occupyingPieces.isEmpty();
    }

    public boolean isBlock() {
        return occupyingPieces.size() >= 2;
    }

    public int getPieceCount() {
        return occupyingPieces.size();
    }

    public boolean isMysteryCell() {
        return mysteryCell;
    }

    public void setMysteryCell(boolean mysteryCell) {
        this.mysteryCell = mysteryCell;
    }

    /**
     * Determines the movement direction for a block on this cell (Step 9 / Rule T-4):
     * - If pieces came from the same direction: moves according to that direction.
     * - If pieces came from opposite directions: moves in the direction of the longest distance from home.
     */
    public Direction getBlockDirection(Board board, PlayerColor color) {
        if (!isBlock()) {
            return null;
        }

        List<Piece> friendlyPieces = occupyingPieces.stream()
                .filter(p -> p.getColor() == color)
                .toList();

        if (friendlyPieces.isEmpty()) {
            return null;
        }

        boolean hasCW = friendlyPieces.stream().anyMatch(p -> p.getDirection() == Direction.CLOCKWISE);
        boolean hasCCW = friendlyPieces.stream().anyMatch(p -> p.getDirection() == Direction.COUNTER_CLOCKWISE);

        if (hasCW && hasCCW) {
            // Opposite directions: direction of longest distance from home (Rule T-4)
            return board.getDirectionOfLongestDistanceFromHome(color, this);
        } else if (hasCW) {
            return Direction.CLOCKWISE;
        } else {
            return Direction.COUNTER_CLOCKWISE;
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
