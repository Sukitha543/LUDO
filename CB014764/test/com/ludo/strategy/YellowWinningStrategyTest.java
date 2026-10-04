package com.ludo.strategy;

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

@DisplayName("YellowWinningStrategy Unit Tests")
class YellowWinningStrategyTest {

    private YellowWinningStrategy strategy;
    private Board board;
    private Player yellowPlayer;
    private Piece yellowPiece1;
    private Piece yellowPiece2;

    @BeforeEach
    void setUp() {
        strategy = new YellowWinningStrategy();
        board = new Board();
        yellowPlayer = new Player(PlayerColor.YELLOW, strategy);
        yellowPiece1 = yellowPlayer.getPiece(1);
        yellowPiece2 = yellowPlayer.getPiece(2);
    }

    @Test
    @DisplayName("Verify null or empty legal moves safely returns null")
    void testNullOrEmptyLegalMoves() {
        assertNull(strategy.selectMove(yellowPlayer, null, board, 6));
        assertNull(strategy.selectMove(yellowPlayer, Collections.emptyList(), board, 6));
    }

    @Test
    @DisplayName("Verify roll of 6 always deploys from base to keep an empty base")
    void testRollSixAlwaysEmptiesBase() {
        Cell startCell = board.getStartingCell(PlayerColor.YELLOW);
        Cell trackFrom = board.getStandardCell(10);
        Cell trackTo = board.getStandardCell(16);

        Piece bluePiece = new Piece(PlayerColor.BLUE, 1);
        bluePiece.setCurrentCell(trackTo);

        // Move 1: Deploy from base to start cell (piece 1 is in base)
        Move deployMove = Move.createBaseToStart(yellowPiece1, startCell);

        // Move 2: A track piece that can make a capture
        yellowPiece2.setInBase(false);
        yellowPiece2.setCurrentCell(trackFrom);
        Move trackCaptureMove = Move.createNormalMove(yellowPiece2, trackFrom, trackTo, 6, true, bluePiece, false);

        List<Move> legalMoves = List.of(trackCaptureMove, deployMove);

        // Unlike Red (which prefers the capture), Yellow ALWAYS deploys from base on a 6
        Move chosen = strategy.selectMove(yellowPlayer, legalMoves, board, 6);
        assertSame(deployMove, chosen, "Yellow must always prioritize emptying base on rolling a 6");
    }

    @Test
    @DisplayName("Verify Yellow prioritizes captures for pieces that need qualification (captureCount == 0, Rule T-7)")
    void testPrioritizesNecessaryCapture() {
        Cell fromCell1 = board.getStandardCell(20);
        Cell toCell1 = board.getStandardCell(23);
        Cell fromCell2 = board.getStandardCell(45);
        Cell toCell2 = board.getStandardCell(48);

        // Piece 1 is on track and has 0 captures: NEEDS a capture to enter home straight
        yellowPiece1.setInBase(false);
        yellowPiece1.setCurrentCell(fromCell1);
        assertEquals(0, yellowPiece1.getCaptureCount());
        Piece opponent = new Piece(PlayerColor.BLUE, 1);
        opponent.setCurrentCell(toCell1);
        Move necessaryCapture = Move.createNormalMove(yellowPiece1, fromCell1, toCell1, 3, true, opponent, false);

        // Piece 2 already has 1 capture: does not need more captures, and is much closer to home
        yellowPiece2.setInBase(false);
        yellowPiece2.setCurrentCell(fromCell2);
        yellowPiece2.incrementCaptureCount();
        assertEquals(1, yellowPiece2.getCaptureCount());
        Move nonCaptureAdvance = Move.createNormalMove(yellowPiece2, fromCell2, toCell2, 3, false, null, false);

        List<Move> legalMoves = List.of(nonCaptureAdvance, necessaryCapture);

        Move chosen = strategy.selectMove(yellowPlayer, legalMoves, board, 3);
        assertSame(necessaryCapture, chosen, "Yellow must prioritize pieces that still need a capture for Rule T-7");
    }

    @Test
    @DisplayName("Verify Yellow ignores unnecessary captures when a piece already qualified can advance closer to Home")
    void testIgnoresUnnecessaryCaptureWhenCloserPieceCanAdvance() {
        Cell fromCell1 = board.getStandardCell(20);
        Cell toCell1 = board.getStandardCell(23);
        Cell fromCell2 = board.getStandardCell(48);
        Cell toCell2 = board.getHomeStraightCell(PlayerColor.YELLOW, 0); // At home straight!

        // Both pieces are on track and already have at least 1 capture
        yellowPiece1.setInBase(false);
        yellowPiece1.setCurrentCell(fromCell1);
        yellowPiece1.incrementCaptureCount();

        yellowPiece2.setInBase(false);
        yellowPiece2.setCurrentCell(fromCell2);
        yellowPiece2.incrementCaptureCount();

        // Move 1: Piece 1 (at cell 20, far away) can capture an opponent
        Piece opponent = new Piece(PlayerColor.BLUE, 1);
        opponent.setCurrentCell(toCell1);
        Move unnecessaryCapture = Move.createNormalMove(yellowPiece1, fromCell1, toCell1, 3, true, opponent, false);

        // Move 2: Piece 2 (at cell 48, right next to Home) advances toward Home
        Move advanceCloser = Move.createNormalMove(yellowPiece2, fromCell2, toCell2, 3, false, null, false);

        List<Move> legalMoves = List.of(unnecessaryCapture, advanceCloser);

        Move chosen = strategy.selectMove(yellowPlayer, legalMoves, board, 3);
        assertSame(advanceCloser, chosen, "Yellow should advance the piece closest to Home rather than making unnecessary captures");
    }

    @Test
    @DisplayName("Verify multiple necessary captures pick the piece that is closer to Home")
    void testMultipleNecessaryCapturesPicksPieceClosestToHome() {
        // Both pieces have 0 captures and are on the board
        yellowPiece1.setInBase(false);
        yellowPiece2.setInBase(false);
        assertEquals(0, yellowPiece1.getCaptureCount());
        assertEquals(0, yellowPiece2.getCaptureCount());

        // Yellow approach is index 0.
        // Piece 1 is at cell 30 (further from approach)
        Cell cell30 = board.getStandardCell(30);
        yellowPiece1.setCurrentCell(cell30);

        // Piece 2 is at cell 50 (only 2 cells away from approach 0: closer to Home!)
        Cell cell50 = board.getStandardCell(50);
        yellowPiece2.setCurrentCell(cell50);

        Piece opp1 = new Piece(PlayerColor.BLUE, 1);
        Piece opp2 = new Piece(PlayerColor.RED, 1);

        Move capture1 = Move.createNormalMove(yellowPiece1, cell30, board.getStandardCell(33), 3, true, opp1, false);
        Move capture2 = Move.createNormalMove(yellowPiece2, cell50, board.getStandardCell(1), 3, true, opp2, false);

        List<Move> legalMoves = List.of(capture1, capture2);

        Move chosen = strategy.selectMove(yellowPlayer, legalMoves, board, 3);
        assertSame(capture2, chosen, "Among necessary captures, Yellow must pick the piece closest to Home");
    }

    @Test
    @DisplayName("Verify Yellow moves the piece closest to Home when no captures are possible")
    void testMovesPieceClosestToHomeWhenNoCaptures() {
        // Piece 1 is on cell 10 (further away)
        yellowPiece1.setInBase(false);
        Cell cell10 = board.getStandardCell(10);
        yellowPiece1.setCurrentCell(cell10);
        Move move1 = Move.createNormalMove(yellowPiece1, cell10, board.getStandardCell(14), 4, false, null, false);

        // Piece 2 is on cell 48 (much closer to approach 0)
        yellowPiece2.setInBase(false);
        Cell cell48 = board.getStandardCell(48);
        yellowPiece2.setCurrentCell(cell48);
        Move move2 = Move.createNormalMove(yellowPiece2, cell48, board.getStandardCell(0), 4, false, null, false);

        List<Move> legalMoves = List.of(move1, move2);

        Move chosen = strategy.selectMove(yellowPlayer, legalMoves, board, 4);
        assertSame(move2, chosen, "When no captures exist, Yellow must move the piece closest to its Home");
    }

    @Test
    @DisplayName("Verify getDistanceToHome accurately calculates distances for all piece positions")
    void testGetDistanceToHomeCalculations() {
        // Null piece or finished piece in home -> distance 0
        assertEquals(0, strategy.getDistanceToHome(null, board));

        Piece finishedPiece = new Piece(PlayerColor.YELLOW, 3);
        finishedPiece.reachHome();
        assertEquals(0, strategy.getDistanceToHome(finishedPiece, board));

        // Piece in base -> distance 60
        Piece basePiece = new Piece(PlayerColor.YELLOW, 4);
        basePiece.setCurrentCell(board.getBaseCell(PlayerColor.YELLOW));
        assertEquals(60, strategy.getDistanceToHome(basePiece, board));

        // Piece in home straight (e.g. yellowhomepath3 -> 5 - 3 = 2 cells to home)
        Piece homeStraightPiece = new Piece(PlayerColor.YELLOW, 1);
        homeStraightPiece.setInBase(false);
        homeStraightPiece.setCurrentCell(board.getHomeStraightCell(PlayerColor.YELLOW, 3));
        assertEquals(2, strategy.getDistanceToHome(homeStraightPiece, board));

        // Piece at Yellow approach cell (cell 0) -> 0 steps to approach + 6 = 6
        Piece approachPiece = new Piece(PlayerColor.YELLOW, 2);
        approachPiece.setInBase(false);
        approachPiece.setCurrentCell(board.getApproachCell(PlayerColor.YELLOW));
        assertEquals(6, strategy.getDistanceToHome(approachPiece, board));

        // Piece at cell 50 -> (0 - 50 + 52) % 52 = 2 steps to approach + 6 = 8
        Piece nearApproachPiece = new Piece(PlayerColor.YELLOW, 1);
        nearApproachPiece.setInBase(false);
        nearApproachPiece.setCurrentCell(board.getStandardCell(50));
        assertEquals(8, strategy.getDistanceToHome(nearApproachPiece, board));
    }
}
