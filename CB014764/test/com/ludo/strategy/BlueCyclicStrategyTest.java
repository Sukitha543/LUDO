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

@DisplayName("BlueCyclicStrategy Unit Tests")
class BlueCyclicStrategyTest {

    private BlueCyclicStrategy strategy;
    private Board board;
    private Player bluePlayer;
    private Piece b1;
    private Piece b2;
    private Piece b3;
    private Piece b4;

    @BeforeEach
    void setUp() {
        strategy = new BlueCyclicStrategy();
        board = new Board();
        bluePlayer = new Player(PlayerColor.BLUE, strategy);
        b1 = bluePlayer.getPiece(1);
        b2 = bluePlayer.getPiece(2);
        b3 = bluePlayer.getPiece(3);
        b4 = bluePlayer.getPiece(4);
    }

    @Test
    @DisplayName("Verify null or empty legal moves safely returns null")
    void testNullOrEmptyLegalMoves() {
        assertNull(strategy.selectMove(bluePlayer, null, board, 4));
        assertNull(strategy.selectMove(bluePlayer, Collections.emptyList(), board, 4));
    }

    @Test
    @DisplayName("Verify initial piece index and setter modulo behavior")
    void testPieceIndexGetterAndSetter() {
        assertEquals(0, strategy.getCurrentPieceIndex(), "Initial index must be 0 (B1)");

        strategy.setCurrentPieceIndex(2);
        assertEquals(2, strategy.getCurrentPieceIndex());

        // Modulo 4 wrapping
        strategy.setCurrentPieceIndex(5); // 5 % 4 = 1
        assertEquals(1, strategy.getCurrentPieceIndex());
    }

    @Test
    @DisplayName("Verify strict cyclic round-robin order (B1 -> B2 -> B3 -> B4 -> B1)")
    void testStrictRoundRobinOrder() {
        Cell from = board.getStandardCell(15);
        Cell to = board.getStandardCell(18);

        Move moveB1 = Move.createNormalMove(b1, from, to, 3, false, null, false);
        Move moveB2 = Move.createNormalMove(b2, from, to, 3, false, null, false);
        Move moveB3 = Move.createNormalMove(b3, from, to, 3, false, null, false);
        Move moveB4 = Move.createNormalMove(b4, from, to, 3, false, null, false);

        List<Move> allMoves = List.of(moveB1, moveB2, moveB3, moveB4);

        // Turn 1: Starts at index 0 -> must choose B1, next index becomes 1
        Move chosen1 = strategy.selectMove(bluePlayer, allMoves, board, 3);
        assertSame(moveB1, chosen1);
        assertEquals(1, strategy.getCurrentPieceIndex());

        // Turn 2: Starts at index 1 -> must choose B2, next index becomes 2
        Move chosen2 = strategy.selectMove(bluePlayer, allMoves, board, 3);
        assertSame(moveB2, chosen2);
        assertEquals(2, strategy.getCurrentPieceIndex());

        // Turn 3: Starts at index 2 -> must choose B3, next index becomes 3
        Move chosen3 = strategy.selectMove(bluePlayer, allMoves, board, 3);
        assertSame(moveB3, chosen3);
        assertEquals(3, strategy.getCurrentPieceIndex());

        // Turn 4: Starts at index 3 -> must choose B4, wraps next index back to 0
        Move chosen4 = strategy.selectMove(bluePlayer, allMoves, board, 3);
        assertSame(moveB4, chosen4);
        assertEquals(0, strategy.getCurrentPieceIndex());

        // Turn 5: Wraps back to B1!
        Move chosen5 = strategy.selectMove(bluePlayer, allMoves, board, 3);
        assertSame(moveB1, chosen5);
        assertEquals(1, strategy.getCurrentPieceIndex());
    }

    @Test
    @DisplayName("Verify skipping unmovable pieces in the cyclic order")
    void testSkippingUnmovablePieces() {
        Cell from = board.getStandardCell(15);
        Cell to = board.getStandardCell(18);

        // B1 and B2 have no legal moves. Only B3 and B4 can move.
        Move moveB3 = Move.createNormalMove(b3, from, to, 3, false, null, false);
        Move moveB4 = Move.createNormalMove(b4, from, to, 3, false, null, false);

        List<Move> legalMoves = List.of(moveB3, moveB4);

        // Starts at index 0 (B1) -> B1 has no move, B2 has no move -> selects B3!
        assertEquals(0, strategy.getCurrentPieceIndex());
        Move chosen = strategy.selectMove(bluePlayer, legalMoves, board, 3);
        assertSame(moveB3, chosen);

        // Next turn should advance to B4 (index 3)
        assertEquals(3, strategy.getCurrentPieceIndex());
    }

    @Test
    @DisplayName("Verify Counter-Clockwise piece prioritizes landing on the Mystery Cell")
    void testCounterClockwiseSeeksMysteryCell() {
        b1.setDirection(Direction.COUNTER_CLOCKWISE);

        Cell from = board.getStandardCell(15);
        Cell regularCell = board.getStandardCell(12);
        Cell mysteryCell = board.getStandardCell(10);
        board.setMysteryCell(mysteryCell);

        Move regularMove = Move.createNormalMove(b1, from, regularCell, 3, false, null, false);
        Move mysteryMove = Move.createNormalMove(b1, from, mysteryCell, 5, false, null, false);

        List<Move> legalMoves = List.of(regularMove, mysteryMove);

        // CCW piece should prioritize landing on the mystery cell
        Move chosen = strategy.selectMove(bluePlayer, legalMoves, board, 3);
        assertSame(mysteryMove, chosen, "Counter-Clockwise Blue piece must prioritize landing on Mystery Cell");
    }

    @Test
    @DisplayName("Verify Clockwise piece prioritizes avoiding the Mystery Cell")
    void testClockwiseAvoidsMysteryCell() {
        b1.setDirection(Direction.CLOCKWISE);

        Cell from = board.getStandardCell(15);
        Cell regularCell = board.getStandardCell(18);
        Cell mysteryCell = board.getStandardCell(20);
        board.setMysteryCell(mysteryCell);

        Move mysteryMove = Move.createNormalMove(b1, from, mysteryCell, 5, false, null, false);
        Move regularMove = Move.createNormalMove(b1, from, regularCell, 3, false, null, false);

        List<Move> legalMoves = List.of(mysteryMove, regularMove);

        // CW piece should avoid landing on the mystery cell
        Move chosen = strategy.selectMove(bluePlayer, legalMoves, board, 3);
        assertSame(regularMove, chosen, "Clockwise Blue piece must prioritize avoiding the Mystery Cell");
    }

    @Test
    @DisplayName("Verify Clockwise piece lands on Mystery Cell when it is the only move")
    void testClockwiseTakesMysteryCellWhenOnlyMove() {
        b1.setDirection(Direction.CLOCKWISE);

        Cell from = board.getStandardCell(15);
        Cell mysteryCell = board.getStandardCell(18);
        board.setMysteryCell(mysteryCell);

        Move onlyMove = Move.createNormalMove(b1, from, mysteryCell, 3, false, null, false);
        List<Move> legalMoves = List.of(onlyMove);

        Move chosen = strategy.selectMove(bluePlayer, legalMoves, board, 3);
        assertSame(onlyMove, chosen, "Clockwise piece must take the move if it is the only available option");
    }

    @Test
    @DisplayName("Verify selection works safely when no mystery cell is on board")
    void testNoMysteryCellOnBoard() {
        board.setMysteryCell(null);
        Cell from = board.getStandardCell(15);
        Cell to = board.getStandardCell(18);

        Move moveB1 = Move.createNormalMove(b1, from, to, 3, false, null, false);
        List<Move> legalMoves = List.of(moveB1);

        Move chosen = strategy.selectMove(bluePlayer, legalMoves, board, 3);
        assertSame(moveB1, chosen);
    }
}
