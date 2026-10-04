package com.ludo.factory;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Player;
import com.ludo.strategy.BlueCyclicStrategy;
import com.ludo.strategy.GreenBlockerStrategy;
import com.ludo.strategy.RedAggressiveStrategy;
import com.ludo.strategy.YellowWinningStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerFactory Unit Tests")
class PlayerFactoryTest {

    @Test
    @DisplayName("Verify passing null PlayerColor throws IllegalArgumentException")
    void testNullColorThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PlayerFactory.createPlayer(null),
                "PlayerFactory must reject null PlayerColor");
    }

    @Test
    @DisplayName("Verify createPlayer binds RED to RedAggressiveStrategy")
    void testCreatePlayerRedStrategy() {
        Player player = PlayerFactory.createPlayer(PlayerColor.RED);
        assertNotNull(player);
        assertEquals(PlayerColor.RED, player.getColor());
        assertInstanceOf(RedAggressiveStrategy.class, player.getStrategy(),
                "Red player must be assigned RedAggressiveStrategy");
        assertEquals(4, player.getPieces().size());
    }

    @Test
    @DisplayName("Verify createPlayer binds GREEN to GreenBlockerStrategy")
    void testCreatePlayerGreenStrategy() {
        Player player = PlayerFactory.createPlayer(PlayerColor.GREEN);
        assertNotNull(player);
        assertEquals(PlayerColor.GREEN, player.getColor());
        assertInstanceOf(GreenBlockerStrategy.class, player.getStrategy(),
                "Green player must be assigned GreenBlockerStrategy");
        assertEquals(4, player.getPieces().size());
    }

    @Test
    @DisplayName("Verify createPlayer binds YELLOW to YellowWinningStrategy")
    void testCreatePlayerYellowStrategy() {
        Player player = PlayerFactory.createPlayer(PlayerColor.YELLOW);
        assertNotNull(player);
        assertEquals(PlayerColor.YELLOW, player.getColor());
        assertInstanceOf(YellowWinningStrategy.class, player.getStrategy(),
                "Yellow player must be assigned YellowWinningStrategy");
        assertEquals(4, player.getPieces().size());
    }

    @Test
    @DisplayName("Verify createPlayer binds BLUE to BlueCyclicStrategy")
    void testCreatePlayerBlueStrategy() {
        Player player = PlayerFactory.createPlayer(PlayerColor.BLUE);
        assertNotNull(player);
        assertEquals(PlayerColor.BLUE, player.getColor());
        assertInstanceOf(BlueCyclicStrategy.class, player.getStrategy(),
                "Blue player must be assigned BlueCyclicStrategy");
        assertEquals(4, player.getPieces().size());
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    @DisplayName("Verify each created player has exactly 4 pieces with matching color")
    void testPlayerPiecesInitialization(PlayerColor color) {
        Player player = PlayerFactory.createPlayer(color);
        assertEquals(4, player.getPieces().size());
        for (int i = 1; i <= 4; i++) {
            assertEquals(color, player.getPiece(i).getColor());
            assertEquals(i, player.getPiece(i).getId());
        }
    }

    @Test
    @DisplayName("Verify createAllPlayers instantiates exactly 4 distinct players for all colors")
    void testCreateAllPlayers() {
        List<Player> allPlayers = PlayerFactory.createAllPlayers();
        assertNotNull(allPlayers);
        assertEquals(4, allPlayers.size(), "Should instantiate exactly 4 players");

        assertEquals(PlayerColor.RED, allPlayers.get(0).getColor());
        assertInstanceOf(RedAggressiveStrategy.class, allPlayers.get(0).getStrategy());

        assertEquals(PlayerColor.GREEN, allPlayers.get(1).getColor());
        assertInstanceOf(GreenBlockerStrategy.class, allPlayers.get(1).getStrategy());

        assertEquals(PlayerColor.YELLOW, allPlayers.get(2).getColor());
        assertInstanceOf(YellowWinningStrategy.class, allPlayers.get(2).getStrategy());

        assertEquals(PlayerColor.BLUE, allPlayers.get(3).getColor());
        assertInstanceOf(BlueCyclicStrategy.class, allPlayers.get(3).getStrategy());
    }
}
