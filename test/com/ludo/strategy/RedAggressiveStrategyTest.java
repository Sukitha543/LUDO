package com.ludo.strategy;

//import com.ludo.enums.CellType;
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

@DisplayName("RedAggressiveStrategy Unit Tests")
class RedAggressiveStrategyTest {

    private RedAggressiveStrategy strategy;
    private Board board;
    private Player redPlayer;
    private Piece redPiece1;
    private Piece redPiece2;

    @BeforeEach
    void setUp() {
        strategy = new RedAggressiveStrategy();
        board = new Board();
        redPlayer = new Player(PlayerColor.RED, strategy);
        redPiece1 = redPlayer.getPiece(1);
        redPiece2 = redPlayer.getPiece(2);
    }

    @Test
    @DisplayName("Verify null or empty legal moves safely returns null")
    void testNullOrEmptyLegalMoves() {
        assertNull(strategy.selectMove(redPlayer, null, board, 4));
        assertNull(strategy.selectMove(redPlayer, Collections.emptyList(), board, 4));
    }

    @Test
    @DisplayName("Verify capture is prioritized over regular forward moves")
    void testCapturePrioritizedOverNormalMove() {
        Cell fromCell = board.getStandardCell(10);
        Cell normalTo = board.getStandardCell(13);
        Cell captureTo = board.getStandardCell(13);

        Piece bluePiece = new Piece(PlayerColor.BLUE, 1);
        bluePiece.setCurrentCell(captureTo);

        Move normalMove = Move.createNormalMove(redPiece1, fromCell, normalTo, 3, false, null, false);
        Move captureMove = Move.createNormalMove(redPiece2, fromCell, captureTo, 3, true, bluePiece, false);

        List<Move> legalMoves = List.of(normalMove, captureMove);

        Move chosen = strategy.selectMove(redPlayer, legalMoves, board, 3);
        assertSame(captureMove, chosen, "Red must choose the capture move over the normal move");
    }

    @Test
    @DisplayName("Verify roll of 6 prioritizes track capture over deploying from base")
    void testRollSixPrefersCaptureOverBaseDeployment() {
        Cell startCell = board.getStartingCell(PlayerColor.RED);
        Cell trackFrom = board.getStandardCell(20);
        Cell trackTo = board.getStandardCell(26);

        Piece bluePiece = new Piece(PlayerColor.BLUE, 1);
        bluePiece.setCurrentCell(trackTo);

        // Move 1: Deploy from base to start 'X'
        Move deployMove = Move.createBaseToStart(redPiece1, startCell);

        // Move 2: Existing piece on track captures an opponent with a roll of 6
        Move trackCaptureMove = Move.createNormalMove(redPiece2, trackFrom, trackTo, 6, true, bluePiece, false);

        List<Move> legalMoves = List.of(deployMove, trackCaptureMove);

        Move chosen = strategy.selectMove(redPlayer, legalMoves, board, 6);
        assertSame(trackCaptureMove, chosen, "On roll of 6, Red must capture rather than deploy from base");
    }

    @Test
    @DisplayName("Verify roll of 6 deploys from base when no track capture is possible")
    void testRollSixDeploysWhenNoCapturePossible() {
        Cell startCell = board.getStartingCell(PlayerColor.RED);
        Cell trackFrom = board.getStandardCell(20);
        Cell trackTo = board.getStandardCell(26);

        // Move 1: Deploy from base
        Move deployMove = Move.createBaseToStart(redPiece1, startCell);

        // Move 2: Regular move on track (no capture)
        Move regularTrackMove = Move.createNormalMove(redPiece2, trackFrom, trackTo, 6, false, null, false);

        List<Move> legalMoves = List.of(deployMove, regularTrackMove);

        Move chosen = strategy.selectMove(redPlayer, legalMoves, board, 6);
        assertSame(deployMove, chosen, "On roll of 6 with no capture, Red must deploy a piece from base to 'X'");
    }

    @Test
    @DisplayName("Verify multiple captures prioritize the opponent piece closest to its home")
    void testMultipleCapturesPicksOpponentClosestToHome() {
        Cell fromCell = board.getStandardCell(10);
        Cell toCell1 = board.getStandardCell(12);
        Cell toCell2 = board.getStandardCell(14);

        // Blue approach index is 13.
        // Piece A (Blue) on cell 12: only 1 step away from approach (closest to home!)
        Piece bluePieceCloseToHome = new Piece(PlayerColor.BLUE, 1);
        bluePieceCloseToHome.setCurrentCell(toCell1);
        bluePieceCloseToHome.setDirection(Direction.CLOCKWISE);

        // Yellow approach index is 0.
        // Piece B (Yellow) on cell 14: 38 steps away from its approach (far from home!)
        Piece yellowPieceFarFromHome = new Piece(PlayerColor.YELLOW, 1);
        yellowPieceFarFromHome.setCurrentCell(toCell2);
        yellowPieceFarFromHome.setDirection(Direction.CLOCKWISE);

        Move captureBlue = Move.createNormalMove(redPiece1, fromCell, toCell1, 2, true, bluePieceCloseToHome, false);
        Move captureYellow = Move.createNormalMove(redPiece2, fromCell, toCell2, 4, true, yellowPieceFarFromHome,
                false);

        List<Move> legalMoves = List.of(captureYellow, captureBlue);

        Move chosen = strategy.selectMove(redPlayer, legalMoves, board, 2);
        assertSame(captureBlue, chosen, "Red must prioritize capturing the opponent that is closest to its home");
    }

    @Test
    @DisplayName("Verify Red avoids creating blocks unless unavoidable")
    void testAvoidsCreatingBlocks() {
        Cell fromCell = board.getStandardCell(10);
        Cell toCell = board.getStandardCell(13);

        Move blockCreatingMove = Move.createNormalMove(redPiece1, fromCell, toCell, 3, false, null, true);
        Move nonBlockMove = Move.createNormalMove(redPiece2, fromCell, toCell, 3, false, null, false);

        List<Move> legalMoves = List.of(blockCreatingMove, nonBlockMove);

        Move chosen = strategy.selectMove(redPlayer, legalMoves, board, 3);
        assertSame(nonBlockMove, chosen, "Red must avoid creating blocks if a non-blocking move is available");
    }

    @Test
    @DisplayName("Verify fallback when creating a block is completely unavoidable")
    void testFallbackWhenBlockUnavoidable() {
        Cell fromCell = board.getStandardCell(10);
        Cell toCell = board.getStandardCell(13);

        Move unavoidableBlockMove = Move.createNormalMove(redPiece1, fromCell, toCell, 3, false, null, true);
        List<Move> legalMoves = List.of(unavoidableBlockMove);

        Move chosen = strategy.selectMove(redPlayer, legalMoves, board, 3);
        assertSame(unavoidableBlockMove, chosen, "Red must take the block-creating move when it is the only option");
    }

    @Test
    @DisplayName("Verify getOpponentDistanceToHome handles base, home straight, and standard track positions")
    void testGetOpponentDistanceToHomeCalculations() {
        // Null piece guard
        assertEquals(Integer.MAX_VALUE, strategy.getOpponentDistanceToHome(null, board));

        // Piece in base (furthest away = 58)
        Piece basePiece = new Piece(PlayerColor.BLUE, 1);
        basePiece.setCurrentCell(board.getBaseCell(PlayerColor.BLUE));
        assertEquals(58, strategy.getOpponentDistanceToHome(basePiece, board));

        // Piece in home straight (e.g. bluehomepath3 -> 5 - 3 = 2 cells to home)
        Piece homeStraightPiece = new Piece(PlayerColor.BLUE, 2);
        homeStraightPiece.setCurrentCell(board.getHomeStraightCell(PlayerColor.BLUE, 3));
        assertEquals(2, strategy.getOpponentDistanceToHome(homeStraightPiece, board));

        // Piece on standard track at Blue approach cell (cell 13) -> 0 steps to
        // approach + 6 = 6
        Piece approachPiece = new Piece(PlayerColor.BLUE, 3);
        approachPiece.setCurrentCell(board.getApproachCell(PlayerColor.BLUE));
        approachPiece.setDirection(Direction.CLOCKWISE);
        assertEquals(6, strategy.getOpponentDistanceToHome(approachPiece, board));
    }
}
