package com.ludo.factory;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Player;
import com.ludo.strategy.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating Players.
 * Implements the Factory Pattern to encapsulate player instantiation
 * and automatically bind each color to its appropriate AI PlayerStrategy.
 */
public class PlayerFactory {

    /**
     * Creates a single Player configured with its assignment-mandated strategy.
     *
     * @param color the color of the player to create
     * @return fully initialized Player with its specific strategy and pieces
     */
    public static Player createPlayer(PlayerColor color) {
        if (color == null) {
            throw new IllegalArgumentException("PlayerColor cannot be null");
        }

        PlayerStrategy strategy = switch (color) {
            case RED -> new RedAggressiveStrategy();
            case GREEN -> new GreenBlockerStrategy();
            case YELLOW -> new YellowWinningStrategy();
            case BLUE -> new BlueCyclicStrategy();
        };

        return new Player(color, strategy);
    }

    /**
     * Creates all four players in standard board order (Red, Green, Yellow, Blue).
     *
     * @return list containing the 4 initialized players
     */
    public static List<Player> createAllPlayers() {
        List<Player> players = new ArrayList<>(4);
        for (PlayerColor color : PlayerColor.values()) {
            players.add(createPlayer(color));
        }
        return players;
    }
}
