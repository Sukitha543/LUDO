package com.ludo.factory;

import com.ludo.enums.CellType;
import com.ludo.enums.PlayerColor;
import com.ludo.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameFactory Unit Tests")
class GameFactoryTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = GameFactory.createGame();
    }

    @Test
    @DisplayName("Verify createGame returns non-null GameState with Board, Dice, and Coin")
    void testGameStateComponentsWired() {
        assertNotNull(gameState, "GameFactory should create a non-null GameState");
        assertNotNull(gameState.getBoard(), "Board must be initialized in GameState");
        assertSame(Dice.getInstance(), gameState.getDice(), "Shared Singleton Dice must be wired into GameState");
        assertSame(Coin.getInstance(), gameState.getCoin(), "Shared Singleton Coin must be wired into GameState");
    }

    @Test
    @DisplayName("Verify GameState contains all 4 players and populated playerMap")
    void testPlayersSetup() {
        assertEquals(4, gameState.getPlayers().size(), "GameState must contain 4 players");

        for (PlayerColor color : PlayerColor.values()) {
            Player player = gameState.getPlayer(color);
            assertNotNull(player, "Player of color " + color + " must be present in playerMap");
            assertEquals(color, player.getColor());
            assertEquals(4, player.getPieces().size());
        }
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    @DisplayName("Verify all 16 pieces start inside their player's Base cell")
    void testAllPiecesPlacedInBase(PlayerColor color) {
        Player player = gameState.getPlayer(color);
        Cell baseCell = gameState.getBoard().getBaseCell(color);

        assertNotNull(baseCell);
        assertEquals(CellType.BASE, baseCell.getType());
        assertEquals(color, baseCell.getColor());
        assertEquals(4, baseCell.getPieceCount(), "Base cell must contain all 4 pieces of color " + color);

        for (Piece piece : player.getPieces()) {
            assertTrue(piece.isInBase(), "Piece " + piece.getName() + " must be marked in base");
            assertFalse(piece.isInHome(), "Piece " + piece.getName() + " must not start in home");
            assertSame(baseCell, piece.getCurrentCell(), "Piece's currentCell must reference its base cell");
            assertTrue(baseCell.getPieces().contains(piece), "Base cell must contain piece " + piece.getName());
        }
    }

    @Test
    @DisplayName("Verify newly created game has no winner and is not over")
    void testInitialGameOverStatus() {
        assertFalse(gameState.isGameOver(), "Newly created game must not be over");
        assertNull(gameState.getWinner(), "Newly created game must have no winner");
    }
}
