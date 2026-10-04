package com.ludo.model;

import com.ludo.enums.CellType;
import com.ludo.enums.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cell Unit Tests")
class CellTest {

    private Cell standardCell;
    private Cell baseCell;
    private Piece redPiece1;
    private Piece redPiece2;

    @BeforeEach
    void setUp() {
        standardCell = new Cell("10", CellType.STANDARD, null);
        baseCell = new Cell("BASE_RED", CellType.BASE, PlayerColor.RED);
        redPiece1 = new Piece(PlayerColor.RED, 1);
        redPiece2 = new Piece(PlayerColor.RED, 2);
    }

    @Test
    @DisplayName("Verify cell initialization properties")
    void testCellInitialization() {
        assertEquals("10", standardCell.getId());
        assertEquals(CellType.STANDARD, standardCell.getType());
        assertNull(standardCell.getColor(), "Standard cell has no color restriction");
        assertEquals("10", standardCell.toString());

        assertEquals("BASE_RED", baseCell.getId());
        assertEquals(CellType.BASE, baseCell.getType());
        assertEquals(PlayerColor.RED, baseCell.getColor());
    }

    @Test
    @DisplayName("Verify newly created cell is empty and not a block")
    void testInitialEmptyState() {
        assertFalse(standardCell.isOccupied(), "New cell should not be occupied");
        assertFalse(standardCell.isBlock(), "New cell should not be a block");
        assertEquals(0, standardCell.getPieceCount());
        assertTrue(standardCell.getPieces().isEmpty());
    }

    @Test
    @DisplayName("Verify adding single piece updates occupancy but does not form a block")
    void testAddSinglePiece() {
        standardCell.addPiece(redPiece1);

        assertTrue(standardCell.isOccupied());
        assertFalse(standardCell.isBlock(), "1 piece is not a block");
        assertEquals(1, standardCell.getPieceCount());
        assertEquals(redPiece1, standardCell.getPieces().get(0));
    }

    @Test
    @DisplayName("Verify adding same piece twice does not create duplicate entries")
    void testAddDuplicatePiece() {
        standardCell.addPiece(redPiece1);
        standardCell.addPiece(redPiece1); // Add again

        assertEquals(1, standardCell.getPieceCount(), "Duplicate piece should not be added twice");
        assertFalse(standardCell.isBlock());
    }

    @Test
    @DisplayName("Verify block formation when two or more pieces occupy the same cell")
    void testBlockFormation() {
        standardCell.addPiece(redPiece1);
        standardCell.addPiece(redPiece2);

        assertTrue(standardCell.isOccupied());
        assertTrue(standardCell.isBlock(), "Two pieces on the same cell should form a block");
        assertEquals(2, standardCell.getPieceCount());
    }

    @Test
    @DisplayName("Verify removing existing and non-existing pieces")
    void testRemovePiece() {
        standardCell.addPiece(redPiece1);

        boolean removedExisting = standardCell.removePiece(redPiece1);
        assertTrue(removedExisting, "Removing existing piece should return true");
        assertFalse(standardCell.isOccupied());
        assertEquals(0, standardCell.getPieceCount());

        boolean removedNonExisting = standardCell.removePiece(redPiece2);
        assertFalse(removedNonExisting, "Removing piece that is not on the cell should return false");
    }

    @Test
    @DisplayName("Verify clearPieces removes all pieces from cell")
    void testClearPieces() {
        standardCell.addPiece(redPiece1);
        standardCell.addPiece(redPiece2);
        assertEquals(2, standardCell.getPieceCount());

        standardCell.clearPieces();
        assertEquals(0, standardCell.getPieceCount());
        assertFalse(standardCell.isOccupied());
        assertFalse(standardCell.isBlock());
    }

    @Test
    @DisplayName("Verify mystery cell flag toggling")
    void testMysteryCellFlag() {
        assertFalse(standardCell.isMysteryCell(), "Cell should not be mystery by default");

        standardCell.setMysteryCell(true);
        assertTrue(standardCell.isMysteryCell());

        standardCell.setMysteryCell(false);
        assertFalse(standardCell.isMysteryCell());
    }

    @Test
    @DisplayName("Verify occupying pieces list is unmodifiable to protect encapsulation")
    void testOccupyingPiecesImmutability() {
        List<Piece> pieces = standardCell.getOccupyingPieces();
        assertThrows(UnsupportedOperationException.class, () -> pieces.add(redPiece1),
                "External callers should not be able to modify the returned pieces list directly");
    }
}
