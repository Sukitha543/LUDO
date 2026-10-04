package com.ludo.model;

import com.ludo.enums.PlayerColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameState Unit Tests")
class GameStateTest {

    private GameState gameState;
    private Board board;
    private Player redPlayer;
    private Player bluePlayer;
    private Dice dice;
    private Coin coin;

    @BeforeEach
    void setUp() {
        board = new Board();
        redPlayer = new Player(PlayerColor.RED, null);
        bluePlayer = new Player(PlayerColor.BLUE, null);
        List<Player> players = List.of(redPlayer, bluePlayer);

        Map<PlayerColor, Player> playerMap = new EnumMap<>(PlayerColor.class);
        playerMap.put(PlayerColor.RED, redPlayer);
        playerMap.put(PlayerColor.BLUE, bluePlayer);

        dice = Dice.getInstance();
        coin = Coin.getInstance();

        gameState = new GameState(board, players, playerMap, dice, coin);
    }

    @Test
    @DisplayName("Verify GameState initialization and bundle retrieval")
    void testGameStateInitialization() {
        assertSame(board, gameState.getBoard());
        assertSame(dice, gameState.getDice());
        assertSame(coin, gameState.getCoin());
        assertEquals(2, gameState.getPlayers().size());
        assertSame(redPlayer, gameState.getPlayer(PlayerColor.RED));
        assertSame(bluePlayer, gameState.getPlayer(PlayerColor.BLUE));
        assertNull(gameState.getPlayer(PlayerColor.YELLOW));
    }

    @Test
    @DisplayName("Verify players list is unmodifiable")
    void testPlayersListImmutability() {
        Player greenPlayer = new Player(PlayerColor.GREEN, null);
        assertThrows(UnsupportedOperationException.class, () -> gameState.getPlayers().add(greenPlayer),
                "Should not allow external mutation of players list");
    }

    @Test
    @DisplayName("Verify initial game is not over and has no winner")
    void testInitialGameOverState() {
        assertFalse(gameState.isGameOver(), "Game should not be over initially");
        assertNull(gameState.getWinner(), "Initial winner should be null");
    }

    @Test
    @DisplayName("Verify winner setting transitions isGameOver to true")
    void testSetWinner() {
        gameState.setWinner(redPlayer);

        assertTrue(gameState.isGameOver(), "Setting a winner should mark the game as over");
        assertSame(redPlayer, gameState.getWinner());
    }
}
