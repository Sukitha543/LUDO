package com.ludo.model;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Move Unit Tests")
class MoveTest {

    private Piece redPiece1;
    private Piece redPiece2;
    private Piece bluePiece1;
    private Piece bluePiece2;
    private Cell fromCell;
    private Cell toCell;
    private Cell startCell;

    @BeforeEach
    void setUp() {
        redPiece1 = new Piece(PlayerColor.RED, 1);
        redPiece2 = new Piece(PlayerColor.RED, 2);
        bluePiece1 = new Piece(PlayerColor.BLUE, 1);
        bluePiece2 = new Piece(PlayerColor.BLUE, 2);

        fromCell = new Cell("10", CellType.STANDARD, null);
        toCell = new Cell("14", CellType.STANDARD, null);
        startCell = new Cell("28", CellType.STARTING_X, PlayerColor.RED);
    }

    @Test
    @DisplayName("Verify createBaseToStart on empty starting cell")
    void testCreateBaseToStartEmptyCell() {
        Move move = Move.createBaseToStart(redPiece1, startCell);

        assertNotNull(move);
        assertEquals(redPiece1, move.getPiece());
        assertNull(move.getFromCell(), "Move from base has no starting board cell");
        assertEquals(startCell, move.getToCell());
        assertEquals(6, move.getRollValue());
        assertTrue(move.isFromBase());
        assertFalse(move.isCapture());
        assertNull(move.getCapturedPiece());
        assertFalse(move.createsBlock());
        assertFalse(move.isBlocked());
    }

    @Test
    @DisplayName("Verify createBaseToStart forms block when friendly piece already occupies start cell")
    void testCreateBaseToStartWithFriendlyPiece() {
        startCell.addPiece(redPiece2); // Friendly piece already on start cell

        Move move = Move.createBaseToStart(redPiece1, startCell);

        assertTrue(move.createsBlock(), "Should flag create block when another friendly piece is present");
        assertFalse(move.isCapture());
    }

    @Test
    @DisplayName("Verify createBaseToStart captures enemy piece on start cell")
    void testCreateBaseToStartCapturesEnemy() {
        startCell.addPiece(bluePiece1); // Enemy piece on start cell

        Move move = Move.createBaseToStart(redPiece1, startCell);

        assertTrue(move.isCapture(), "Should flag capture when landing on an enemy piece");
        assertEquals(bluePiece1, move.getCapturedPiece());
        assertFalse(move.createsBlock());
    }

    @Test
    @DisplayName("Verify createNormalMove sets steps, capture, and block break flags")
    void testCreateNormalMove() {
        // Place 2 pieces on fromCell to make it a block
        fromCell.addPiece(redPiece1);
        fromCell.addPiece(redPiece2);
        assertTrue(fromCell.isBlock());

        Move move = Move.createNormalMove(redPiece1, fromCell, toCell, 4, true, bluePiece1, false);

        assertEquals(redPiece1, move.getPiece());
        assertEquals(fromCell, move.getFromCell());
        assertEquals(toCell, move.getToCell());
        assertEquals(4, move.getRollValue());
        assertEquals(4, move.getMovementSteps());
        assertFalse(move.isFromBase());
        assertTrue(move.isCapture());
        assertEquals(bluePiece1, move.getCapturedPiece());
        assertTrue(move.breaksBlock(), "Moving a piece away from a 2-piece cell should break the block");
        assertFalse(move.createsBlock());
    }

    @Test
    @DisplayName("Verify createBlockedMove records obstacle piece and intended destination")
    void testCreateBlockedMove() {
        Cell cellBeforeBlock = new Cell("12", CellType.STANDARD, null);
        Cell intendedTarget = new Cell("14", CellType.STANDARD, null);

        Move blockedMove = Move.createBlockedMove(redPiece1, fromCell, cellBeforeBlock, intendedTarget,
                bluePiece1, 4, false, null, false);

        assertTrue(blockedMove.isBlocked());
        assertEquals(bluePiece1, blockedMove.getBlockingPiece());
        assertEquals(intendedTarget, blockedMove.getIntendedTargetCell());
        assertEquals(cellBeforeBlock, blockedMove.getToCell());
    }

    @Test
    @DisplayName("Verify createBlockMove for moving blockades (Rule T-4)")
    void testCreateBlockMove() {
        List<Piece> blockPieces = List.of(redPiece1, redPiece2);

        Move blockMove = Move.createBlockMove(blockPieces, fromCell, toCell, 4, 2, Direction.CLOCKWISE);

        assertTrue(blockMove.isBlockMove());
        assertEquals(blockPieces, blockMove.getBlockPieces());
        assertEquals(Direction.CLOCKWISE, blockMove.getBlockDirection());
        assertEquals(2, blockMove.getBlockSteps());
        assertEquals(2, blockMove.getMovementSteps());
        assertFalse(blockMove.isBlockCapture());
    }

    @Test
    @DisplayName("Verify isBlockCapture when a block captures opponent block pieces")
    void testBlockCapture() {
        List<Piece> blockPieces = List.of(redPiece1, redPiece2);
        List<Piece> capturedBlock = List.of(bluePiece1, bluePiece2);

        Move blockMove = Move.createBlockMove(blockPieces, fromCell, toCell, 4, 2, Direction.CLOCKWISE,
                true, capturedBlock);

        assertTrue(blockMove.isBlockMove());
        assertTrue(blockMove.isCapture());
        assertTrue(blockMove.isBlockCapture());
        assertEquals(capturedBlock, blockMove.getCapturedBlockPieces());
    }

    @Test
    @DisplayName("Verify captured block pieces list is unmodifiable")
    void testCapturedBlockPiecesImmutability() {
        Move move = Move.createNormalMove(redPiece1, fromCell, toCell, 4, false, null, false);
        assertThrows(UnsupportedOperationException.class, () -> move.getCapturedBlockPieces().add(bluePiece1));
    }
}
