package com.ludo.engine;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.model.*;
import com.ludo.state.BriefingState;
import com.ludo.state.EnergizedState;
import com.ludo.state.SickState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MoveGenerator Unit Tests")
class MoveGeneratorTest {

    private Board board;
    private Player redPlayer;
    private Piece red1;
    private Piece red2;

    @BeforeEach
    void setUp() {
        board = new Board();
        redPlayer = new Player(PlayerColor.RED, null);
        red1 = redPlayer.getPiece(1);
        red2 = redPlayer.getPiece(2);
    }

    // =========================================================================
    // Category 1: Input Validation & Boundary Defense
    // =========================================================================

    @Test
    @DisplayName("Verify null player throws IllegalArgumentException")
    void testNullPlayerThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> MoveGenerator.generateLegalMoves(null, board, 6),
                "Should reject null Player");
    }

    @Test
    @DisplayName("Verify null board throws IllegalArgumentException")
    void testNullBoardThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> MoveGenerator.generateLegalMoves(redPlayer, null, 6),
                "Should reject null Board");
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 7, 10})
    @DisplayName("Verify out-of-range dice rolls throw IllegalArgumentException")
    void testInvalidDiceRollsThrowException(int invalidRoll) {
        assertThrows(IllegalArgumentException.class, () -> MoveGenerator.generateLegalMoves(redPlayer, board, invalidRoll),
                "Should reject dice roll outside 1-6 range: " + invalidRoll);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify valid dice rolls 1 through 6 pass validation")
    void testValidDiceRollsPassValidation(int validRoll) {
        assertDoesNotThrow(() -> MoveGenerator.generateLegalMoves(redPlayer, board, validRoll));
    }

    // =========================================================================
    // Category 2: Leaving Base Mechanics (Rule 2)
    // =========================================================================

    @Test
    @DisplayName("Verify roll of 6 generates base-to-start move when pieces are in base")
    void testRollSixDeploysFromBase() {
        assertTrue(redPlayer.hasPiecesInBase());

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 6);

        assertFalse(legalMoves.isEmpty(), "Roll of 6 must generate at least 1 legal move from base");
        Move baseMove = legalMoves.get(0);
        assertTrue(baseMove.isFromBase());
        assertSame(board.getStartingCell(PlayerColor.RED), baseMove.getToCell());
        assertEquals(6, baseMove.getRollValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("Verify rolls 1 through 5 generate zero moves when all pieces are in base")
    void testRollsOneToFiveCannotLeaveBase(int roll) {
        assertTrue(redPlayer.hasPiecesInBase());
        assertEquals(4, redPlayer.getPiecesInBase().size());

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, roll);
        assertTrue(legalMoves.isEmpty(), "Roll of " + roll + " cannot move pieces out of base");
    }

    @Test
    @DisplayName("Verify deploying on 6 flags createsBlock when a friendly piece occupies starting 'X'")
    void testDeployingFormsBlockWithFriendlyPiece() {
        Cell startCell = board.getStartingCell(PlayerColor.RED);
        red2.setInBase(false);
        red2.setCurrentCell(startCell);
        startCell.addPiece(red2); // Friendly piece already on 'X'

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 6);

        Move deployMove = legalMoves.stream().filter(Move::isFromBase).findFirst().orElse(null);
        assertNotNull(deployMove);
        assertTrue(deployMove.createsBlock(), "Deploying onto a friendly piece must create a block");
    }

    @Test
    @DisplayName("Verify deploying on 6 captures lone opponent resting on starting 'X'")
    void testDeployingCapturesOpponentOnStart() {
        Cell startCell = board.getStartingCell(PlayerColor.RED);
        Piece bluePiece = new Piece(PlayerColor.BLUE, 1);
        bluePiece.setInBase(false);
        bluePiece.setCurrentCell(startCell);
        startCell.addPiece(bluePiece); // Enemy piece on starting 'X'

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 6);

        Move deployMove = legalMoves.stream().filter(Move::isFromBase).findFirst().orElse(null);
        assertNotNull(deployMove);
        assertTrue(deployMove.isCapture());
        assertSame(bluePiece, deployMove.getCapturedPiece());
    }

    @Test
    @DisplayName("Verify base deployment is prohibited when starting 'X' has an opponent blockade")
    void testDeployingProhibitedByOpponentBlockade() {
        Cell startCell = board.getStartingCell(PlayerColor.RED);
        Piece blue1 = new Piece(PlayerColor.BLUE, 1);
        Piece blue2 = new Piece(PlayerColor.BLUE, 2);
        blue1.setInBase(false);
        blue2.setInBase(false);
        startCell.addPiece(blue1);
        startCell.addPiece(blue2);
        assertTrue(startCell.isBlock());

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 6);

        boolean hasBaseMove = legalMoves.stream().anyMatch(Move::isFromBase);
        assertFalse(hasBaseMove, "Base deployment must be blocked if an opponent blockade is on starting square");
    }

    // =========================================================================
    // Category 3: Track Movement, Captures & Blockades
    // =========================================================================

    @Test
    @DisplayName("Verify normal forward movement advances piece by exact roll value")
    void testNormalForwardMovement() {
        Cell cell10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(cell10);
        cell10.addPiece(red1);

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 4);

        assertEquals(1, legalMoves.size());
        Move move = legalMoves.get(0);
        assertSame(red1, move.getPiece());
        assertSame(cell10, move.getFromCell());
        assertSame(board.getStandardCell(14), move.getToCell());
        assertEquals(4, move.getMovementSteps());
        assertFalse(move.isCapture());
        assertFalse(move.createsBlock());
    }

    @Test
    @DisplayName("Verify landing on friendly piece creates a block")
    void testLandingOnFriendlyPieceCreatesBlock() {
        Cell cell10 = board.getStandardCell(10);
        Cell cell13 = board.getStandardCell(13);

        red1.setInBase(false);
        red1.setCurrentCell(cell10);
        cell10.addPiece(red1);

        red2.setInBase(false);
        red2.setCurrentCell(cell13);
        cell13.addPiece(red2); // Friendly piece waiting at target cell

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 3);

        Move red1Move = legalMoves.stream().filter(m -> m.getPiece() == red1).findFirst().orElse(null);
        assertNotNull(red1Move);
        assertTrue(red1Move.createsBlock(), "Landing on red2 must create a block");
    }

    @Test
    @DisplayName("Verify landing on single opponent piece triggers capture")
    void testLandingOnOpponentTriggersCapture() {
        Cell cell10 = board.getStandardCell(10);
        Cell cell14 = board.getStandardCell(14);

        red1.setInBase(false);
        red1.setCurrentCell(cell10);
        cell10.addPiece(red1);

        Piece bluePiece = new Piece(PlayerColor.BLUE, 1);
        bluePiece.setInBase(false);
        bluePiece.setCurrentCell(cell14);
        cell14.addPiece(bluePiece);

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 4);

        Move move = legalMoves.get(0);
        assertTrue(move.isCapture());
        assertSame(bluePiece, move.getCapturedPiece());
    }

    // =========================================================================
    // Category 4: Opponent Blockade Obstructions (Rule T-3)
    // =========================================================================

    @Test
    @DisplayName("Verify piece cannot jump over opponent blockade and stops at adjacent cell before block (Rule T-3)")
    void testBlockadeObstructionStopsBeforeBlock() {
        Cell cell10 = board.getStandardCell(10);
        Cell cell12 = board.getStandardCell(12);

        red1.setInBase(false);
        red1.setCurrentCell(cell10);
        cell10.addPiece(red1);

        // Opponent blockade at cell 12
        Piece blue1 = new Piece(PlayerColor.BLUE, 1);
        Piece blue2 = new Piece(PlayerColor.BLUE, 2);
        blue1.setInBase(false);
        blue2.setInBase(false);
        cell12.addPiece(blue1);
        cell12.addPiece(blue2);
        assertTrue(cell12.isBlock());

        // Roll 4: normally reaches cell 14, but cell 12 has a block!
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 4);

        assertEquals(1, legalMoves.size());
        Move move = legalMoves.get(0);
        // Must stop at cell 11 (the cell immediately before the block)
        assertSame(board.getStandardCell(11), move.getToCell());
        assertSame(board.getStandardCell(14), move.getIntendedTargetCell());
        assertSame(blue1, move.getBlockingPiece());
        assertTrue(move.isBlocked(), "Move must be marked as blocked since intended target could not be reached");
    }

    @Test
    @DisplayName("Verify piece directly adjacent to an opponent blockade cannot move forward at all (Rule T-3)")
    void testTrappedAdjacentToBlockade() {
        Cell cell11 = board.getStandardCell(11);
        Cell cell12 = board.getStandardCell(12);

        red1.setInBase(false);
        red1.setCurrentCell(cell11);
        cell11.addPiece(red1); // Directly adjacent to cell 12

        // Opponent blockade at cell 12
        Piece blue1 = new Piece(PlayerColor.BLUE, 1);
        Piece blue2 = new Piece(PlayerColor.BLUE, 2);
        blue1.setInBase(false);
        blue2.setInBase(false);
        cell12.addPiece(blue1);
        cell12.addPiece(blue2);

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 3);

        assertEquals(1, legalMoves.size());
        Move move = legalMoves.get(0);
        assertTrue(move.isBlocked(), "Adjacent piece must be flagged as blocked");
        assertSame(cell11, move.getToCell(), "Cannot move forward, stays on cell 11");
    }

    // =========================================================================
    // Category 5: Group Blockade Movement & Combat (Rules T-4 & T-8)
    // =========================================================================

    @Test
    @DisplayName("Verify 2-piece blockade moves together by diceRoll / 2 steps (Rule T-4)")
    void testBlockadeGroupMovement() {
        Cell cell10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(cell10);
        red2.setCurrentCell(cell10);
        cell10.addPiece(red1);
        cell10.addPiece(red2);
        assertTrue(cell10.isBlock());

        // Roll 4: block steps = 4 / 2 = 2 cells -> landing cell is cell 12
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 4);

        Move blockMove = legalMoves.stream().filter(Move::isBlockMove).findFirst().orElse(null);
        assertNotNull(blockMove, "Must generate a group block move (Rule T-4)");
        assertEquals(2, blockMove.getBlockSteps());
        assertSame(board.getStandardCell(12), blockMove.getToCell());
    }

    @Test
    @DisplayName("Verify 2-piece blockade cannot move when diceRoll / 2 is 0 (Roll 1)")
    void testBlockadeCannotMoveOnRollOne() {
        Cell cell10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(cell10);
        red2.setCurrentCell(cell10);
        cell10.addPiece(red1);
        cell10.addPiece(red2);

        // Roll 1: 1 / 2 = 0 steps
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 1);

        boolean hasBlockMove = legalMoves.stream().anyMatch(Move::isBlockMove);
        assertFalse(hasBlockMove, "Blockade cannot move when roll / blockSize == 0");
    }

    @Test
    @DisplayName("Verify equal-sized blockade captures opponent blockade (Rule T-8)")
    void testBlockadeCapturesOpponentBlockade() {
        Cell cell10 = board.getStandardCell(10);
        Cell cell12 = board.getStandardCell(12);

        // Friendly 2-piece blockade at cell 10
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(cell10);
        red2.setCurrentCell(cell10);
        cell10.addPiece(red1);
        cell10.addPiece(red2);

        // Opponent 2-piece blockade at cell 12
        Piece blue1 = new Piece(PlayerColor.BLUE, 1);
        Piece blue2 = new Piece(PlayerColor.BLUE, 2);
        blue1.setInBase(false);
        blue2.setInBase(false);
        blue1.setCurrentCell(cell12);
        blue2.setCurrentCell(cell12);
        cell12.addPiece(blue1);
        cell12.addPiece(blue2);

        // Roll 4 -> block advances 2 steps landing on cell 12
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 4);

        Move blockMove = legalMoves.stream().filter(Move::isBlockMove).findFirst().orElse(null);
        assertNotNull(blockMove);
        assertTrue(blockMove.isCapture(), "Equal-sized block landing on opponent block must capture");
        assertTrue(blockMove.isBlockCapture());
        assertEquals(2, blockMove.getCapturedBlockPieces().size());
    }

    // =========================================================================
    // Category 6: Status Effects Integration (Briefing, Energized, Sick)
    // =========================================================================

    @Test
    @DisplayName("Verify piece in BriefingState is frozen and generates zero moves (Rule T-13)")
    void testBriefingStatePieceGeneratesZeroMoves() {
        Cell cell10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(cell10);
        cell10.addPiece(red1);
        red1.attendBriefing(); // Set BriefingState

        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 4);

        assertTrue(legalMoves.isEmpty(), "Frozen piece in BriefingState must generate zero moves");
    }

    @Test
    @DisplayName("Verify piece in EnergizedState advances double steps (2 * roll, Rule T-12)")
    void testEnergizedStateAdvancesDoubleSteps() {
        Cell cell10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(cell10);
        cell10.addPiece(red1);
        red1.energize(); // Set EnergizedState

        // Roll 3 -> 3 * 2 = 6 steps -> lands on cell 16
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 3);

        assertEquals(1, legalMoves.size());
        Move move = legalMoves.get(0);
        assertEquals(6, move.getMovementSteps());
        assertSame(board.getStandardCell(16), move.getToCell());
    }

    @Test
    @DisplayName("Verify piece in SickState advances halved steps (roll / 2, Rule T-12)")
    void testSickStateAdvancesHalvedSteps() {
        Cell cell10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(cell10);
        cell10.addPiece(red1);
        red1.makeSick(); // Set SickState

        // Roll 5 -> floor(5 / 2) = 2 steps -> lands on cell 12
        List<Move> legalMoves = MoveGenerator.generateLegalMoves(redPlayer, board, 5);

        assertEquals(1, legalMoves.size());
        Move move = legalMoves.get(0);
        assertEquals(2, move.getMovementSteps());
        assertSame(board.getStandardCell(12), move.getToCell());
    }
}
