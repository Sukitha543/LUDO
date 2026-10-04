package com.ludo.strategy;

import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.model.Board;
import com.ludo.model.Cell;
import com.ludo.model.Move;
import com.ludo.model.Piece;
import com.ludo.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GreenBlockerStrategy Unit Tests")
class GreenBlockerStrategyTest {

    private GreenBlockerStrategy strategy;
    private Board board;
    private Player greenPlayer;
    private Piece greenPiece1;
    private Piece greenPiece2;

    @BeforeEach
    void setUp() {
        strategy = new GreenBlockerStrategy();
        board = new Board();
        greenPlayer = new Player(PlayerColor.GREEN, strategy);
        greenPiece1 = greenPlayer.getPiece(1);
        greenPiece2 = greenPlayer.getPiece(2);
    }

    @Test
    @DisplayName("Verify null or empty legal moves safely returns null")
    void testNullOrEmptyLegalMoves() {
        assertNull(strategy.selectMove(greenPlayer, null, board, 6));
        assertNull(strategy.selectMove(greenPlayer, Collections.emptyList(), board, 6));
    }

    @Test
    @DisplayName("Verify roll of 6 prioritizes creating a block over deploying from base")
    void testRollSixPrefersBlockCreationOverBaseDeployment() {
        Cell startCell = board.getStartingCell(PlayerColor.GREEN);
        Cell trackFrom = board.getStandardCell(10);
        Cell trackTo = board.getStandardCell(16);

        // Move 1: Deploy from base to starting cell
        Move deployMove = Move.createBaseToStart(greenPiece1, startCell);

        // Move 2: Track piece moves 6 cells and creates a blockade
        greenPiece2.setInBase(false);
        greenPiece2.setCurrentCell(trackFrom);
        Move blockCreatingMove = Move.createNormalMove(greenPiece2, trackFrom, trackTo, 6, false, null, true);

        List<Move> legalMoves = List.of(deployMove, blockCreatingMove);

        // Green prefers creating a block over deploying from base on roll of 6!
        Move chosen = strategy.selectMove(greenPlayer, legalMoves, board, 6);
        assertSame(blockCreatingMove, chosen, "Green must prioritize creating a block over deploying from base on roll of 6");
    }

    @Test
    @DisplayName("Verify roll of 6 deploys from base when no block-creating move is possible")
    void testRollSixDeploysWhenNoBlockCreated() {
        Cell startCell = board.getStartingCell(PlayerColor.GREEN);
        Cell trackFrom = board.getStandardCell(10);
        Cell trackTo = board.getStandardCell(16);

        // Move 1: Deploy from base
        Move deployMove = Move.createBaseToStart(greenPiece1, startCell);

        // Move 2: Regular move on track (does not create block)
        greenPiece2.setInBase(false);
        greenPiece2.setCurrentCell(trackFrom);
        Move regularMove = Move.createNormalMove(greenPiece2, trackFrom, trackTo, 6, false, null, false);

        List<Move> legalMoves = List.of(regularMove, deployMove);

        Move chosen = strategy.selectMove(greenPlayer, legalMoves, board, 6);
        assertSame(deployMove, chosen, "Green must deploy from base on roll of 6 when no block can be formed");
    }

    @Test
    @DisplayName("Verify Green prioritizes necessary captures for Home Straight qualification (captureCount == 0, Rule T-7)")
    void testPrioritizesNecessaryCapture() {
        Cell fromCell = board.getStandardCell(20);
        Cell toCell = board.getStandardCell(23);

        // Piece 1 has 0 captures: needs a capture
        greenPiece1.setInBase(false);
        greenPiece1.setCurrentCell(fromCell);
        assertEquals(0, greenPiece1.getCaptureCount());

        Piece opponent = new Piece(PlayerColor.RED, 1);
        opponent.setCurrentCell(toCell);

        Move necessaryCapture = Move.createNormalMove(greenPiece1, fromCell, toCell, 3, true, opponent, false);
        Move regularMove = Move.createNormalMove(greenPiece2, fromCell, toCell, 3, false, null, false);

        List<Move> legalMoves = List.of(regularMove, necessaryCapture);

        Move chosen = strategy.selectMove(greenPlayer, legalMoves, board, 3);
        assertSame(necessaryCapture, chosen, "Green must prioritize necessary captures to qualify for the Home Straight");
    }

    @Test
    @DisplayName("Verify Green prioritizes creating blocks on any roll")
    void testCreatesBlockOnAnyRoll() {
        // Both pieces already qualified
        greenPiece1.setInBase(false);
        greenPiece1.incrementCaptureCount();
        greenPiece2.setInBase(false);
        greenPiece2.incrementCaptureCount();

        Cell from1 = board.getStandardCell(10);
        Cell to1 = board.getStandardCell(13);
        Cell from2 = board.getStandardCell(20);
        Cell to2 = board.getStandardCell(23);

        Move blockCreatingMove = Move.createNormalMove(greenPiece1, from1, to1, 3, false, null, true);
        Move nonBlockMove = Move.createNormalMove(greenPiece2, from2, to2, 3, false, null, false);

        List<Move> legalMoves = List.of(nonBlockMove, blockCreatingMove);

        Move chosen = strategy.selectMove(greenPlayer, legalMoves, board, 3);
        assertSame(blockCreatingMove, chosen, "Green must prioritize creating a block on any roll");
    }

    @Test
    @DisplayName("Verify Green advances block as a unit (Rule T-4) before breaking")
    void testAdvancesBlockAsUnitBeforeBreaking() {
        greenPiece1.setInBase(false);
        greenPiece1.incrementCaptureCount();
        greenPiece2.setInBase(false);
        greenPiece2.incrementCaptureCount();

        Cell fromCell = board.getStandardCell(10);
        Cell toCell = board.getStandardCell(12);

        // Move 1: Advancing block as a unit (Rule T-4 block move)
        List<Piece> blockPieces = List.of(greenPiece1, greenPiece2);
        Move blockMove = Move.createBlockMove(blockPieces, fromCell, toCell, 4, 2, Direction.CLOCKWISE);

        // Move 2: Moving one piece away and breaking the block
        Move breakingMove = Move.createNormalMove(greenPiece1, fromCell, toCell, 2, false, null, false);

        List<Move> legalMoves = List.of(breakingMove, blockMove);

        Move chosen = strategy.selectMove(greenPlayer, legalMoves, board, 2);
        assertSame(blockMove, chosen, "Green must prioritize advancing a block as a unit rather than breaking it");
    }

    @Test
    @DisplayName("Verify Green protects blocks and breaks only as an absolute last resort")
    void testBreaksBlockOnlyAsLastResort() {
        greenPiece1.setInBase(false);
        greenPiece1.incrementCaptureCount();
        greenPiece2.setInBase(false);
        greenPiece2.incrementCaptureCount();

        Cell blockCell = board.getStandardCell(10);
        Cell singlePieceCell = board.getStandardCell(25);

        // Move 1: Non-breaking move of a single independent piece
        Move nonBreakingMove = Move.createNormalMove(greenPiece2, singlePieceCell, board.getStandardCell(28), 3, false, null, false);

        // Move 2: Breaking the block
        blockCell.addPiece(greenPiece1);
        blockCell.addPiece(new Piece(PlayerColor.GREEN, 3));
        Move breakingMove = Move.createNormalMove(greenPiece1, blockCell, board.getStandardCell(13), 3, false, null, false);
        assertTrue(breakingMove.breaksBlock());

        // Choice between non-breaking move and breaking move -> Green protects block!
        List<Move> legalMoves = List.of(breakingMove, nonBreakingMove);
        Move chosen = strategy.selectMove(greenPlayer, legalMoves, board, 3);
        assertSame(nonBreakingMove, chosen, "Green must protect established blocks by moving other pieces first");

        // Last resort: ONLY breaking move is available -> Green takes it
        List<Move> onlyBreakingMoves = List.of(breakingMove);
        Move lastResort = strategy.selectMove(greenPlayer, onlyBreakingMoves, board, 3);
        assertSame(breakingMove, lastResort, "Green should break block when it is the strictly only available move");
    }

    @Test
    @DisplayName("Verify getDistanceToHome calculations for Green pieces")
    void testGetDistanceToHomeCalculations() {
        assertEquals(0, strategy.getDistanceToHome(null, board));

        Piece finishedPiece = new Piece(PlayerColor.GREEN, 3);
        finishedPiece.reachHome();
        assertEquals(0, strategy.getDistanceToHome(finishedPiece, board));

        // Piece in base -> 60
        Piece basePiece = new Piece(PlayerColor.GREEN, 4);
        basePiece.setCurrentCell(board.getBaseCell(PlayerColor.GREEN));
        assertEquals(60, strategy.getDistanceToHome(basePiece, board));

        // Piece in home straight (e.g. greenhomepath2 -> 5 - 2 = 3)
        Piece homeStraightPiece = new Piece(PlayerColor.GREEN, 1);
        homeStraightPiece.setInBase(false);
        homeStraightPiece.setCurrentCell(board.getHomeStraightCell(PlayerColor.GREEN, 2));
        assertEquals(3, strategy.getDistanceToHome(homeStraightPiece, board));

        // Piece at Green approach cell (cell 39) -> 6
        Piece approachPiece = new Piece(PlayerColor.GREEN, 2);
        approachPiece.setInBase(false);
        approachPiece.setCurrentCell(board.getApproachCell(PlayerColor.GREEN));
        assertEquals(6, strategy.getDistanceToHome(approachPiece, board));

        // Piece at cell 35 -> (39 - 35 + 52) % 52 = 4 + 6 = 10
        Piece trackPiece = new Piece(PlayerColor.GREEN, 1);
        trackPiece.setInBase(false);
        trackPiece.setCurrentCell(board.getStandardCell(35));
        assertEquals(10, strategy.getDistanceToHome(trackPiece, board));
    }
}
