package com.ludo.model;

import com.ludo.enums.PlayerColor;
import com.ludo.strategy.PlayerStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Player Unit Tests")
class PlayerTest {

    private Player player;
    private PlayerStrategy dummyStrategy;

    @BeforeEach
    void setUp() {
        dummyStrategy = (p, legalMoves, board, diceRoll) -> legalMoves.isEmpty() ? null : legalMoves.get(0);
        player = new Player(PlayerColor.RED, dummyStrategy);
    }

    @Test
    @DisplayName("Verify player initialization properties and initial piece collection")
    void testPlayerInitialization() {
        assertEquals(PlayerColor.RED, player.getColor());
        assertEquals("Red Player", player.getName());
        assertEquals(dummyStrategy, player.getStrategy());
        assertEquals(0, player.getConsecutiveThrees());

        List<Piece> pieces = player.getPieces();
        assertNotNull(pieces);
        assertEquals(4, pieces.size(), "Player must be initialized with exactly 4 pieces");

        // Verify each piece belongs to this player with ids 1..4
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, pieces.get(i).getId());
            assertEquals(PlayerColor.RED, pieces.get(i).getColor());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("Verify getPiece returns correct piece for valid IDs 1 through 4")
    void testGetPieceValidIds(int id) {
        Piece piece = player.getPiece(id);
        assertNotNull(piece);
        assertEquals(id, piece.getId());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 5, 10})
    @DisplayName("Verify getPiece throws IllegalArgumentException for out-of-range IDs")
    void testGetPieceInvalidIdsThrowException(int invalidId) {
        assertThrows(IllegalArgumentException.class, () -> player.getPiece(invalidId));
    }

    @Test
    @DisplayName("Verify getPieces list is unmodifiable")
    void testPiecesListEncapsulation() {
        List<Piece> pieces = player.getPieces();
        assertThrows(UnsupportedOperationException.class, () -> pieces.add(new Piece(PlayerColor.RED, 1)),
                "Pieces list should not be modifiable externally");
    }

    @Test
    @DisplayName("Verify piece location filters (inBase, onBoard, inHome)")
    void testPieceLocationFilters() {
        // Initially all 4 pieces are in base
        assertEquals(4, player.getPiecesInBase().size());
        assertTrue(player.getPiecesOnBoard().isEmpty());
        assertTrue(player.getPiecesInHome().isEmpty());
        assertTrue(player.hasPiecesInBase());

        // Move 2 pieces out of base onto the board
        player.getPiece(1).setInBase(false);
        player.getPiece(2).setInBase(false);

        assertEquals(2, player.getPiecesInBase().size());
        assertEquals(2, player.getPiecesOnBoard().size());
        assertTrue(player.hasPiecesInBase());

        // Move 1 piece into Home
        player.getPiece(1).reachHome();

        assertEquals(2, player.getPiecesInBase().size());
        assertEquals(1, player.getPiecesOnBoard().size());
        assertEquals(1, player.getPiecesInHome().size());

        // Move remaining 2 pieces out of base
        player.getPiece(3).setInBase(false);
        player.getPiece(4).setInBase(false);
        assertFalse(player.hasPiecesInBase(), "Player should have no pieces in base");
    }

    @Test
    @DisplayName("Verify winning condition (hasWon returns true ONLY when all 4 pieces reach home)")
    void testWinningCondition() {
        assertFalse(player.hasWon(), "Player cannot win with 0 pieces in home");

        // Reach home with 1, 2, and 3 pieces -> still not won
        player.getPiece(1).reachHome();
        assertFalse(player.hasWon());

        player.getPiece(2).reachHome();
        assertFalse(player.hasWon());

        player.getPiece(3).reachHome();
        assertFalse(player.hasWon(), "Player cannot win with only 3 pieces in home");

        // Reach home with the 4th piece -> WON!
        player.getPiece(4).reachHome();
        assertTrue(player.hasWon(), "Player should win when all 4 pieces reach home");
    }

    @Test
    @DisplayName("Verify consecutive threes counter management")
    void testConsecutiveThrees() {
        assertEquals(0, player.getConsecutiveThrees());

        player.incrementConsecutiveThrees();
        player.incrementConsecutiveThrees();
        assertEquals(2, player.getConsecutiveThrees());

        player.resetConsecutiveThrees();
        assertEquals(0, player.getConsecutiveThrees());
    }

    @Test
    @DisplayName("Verify chooseMove handles null/empty moves and delegates to strategy")
    void testChooseMove() {
        // Null or empty moves should return null safely
        assertNull(player.chooseMove(null, null, 6));
        assertNull(player.chooseMove(Collections.emptyList(), null, 6));

        // When strategy is null, should return null safely
        player.setStrategy(null);
        assertNull(player.chooseMove(Collections.emptyList(), null, 6));
    }
}
