package com.ludo.factory;

import com.ludo.enums.PlayerColor;
import com.ludo.model.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class for initializing the entire Ludo game environment.
 * Implements the Factory Pattern to assemble the Board, Players, Pieces,
 * Singleton Dice, and Singleton Coin into a cohesive, ready-to-play GameState.
 */
public class GameFactory {

    /**
     * Initializes and wires up all components of the game.
     * - Creates the 52-cell board and 20 home straight cells.
     * - Uses PlayerFactory to instantiate players with their respective strategies.
     * - Places all 16 pieces into their corresponding color bases.
     * - Integrates shared Singleton Dice and Singleton Coin.
     *
     * @return fully initialized GameState
     */
    public static GameState createGame() {
        // 1. Create the Board
        Board board = new Board();

        // 2. Create the Players via PlayerFactory
        List<Player> players = PlayerFactory.createAllPlayers();
        Map<PlayerColor, Player> playerMap = new EnumMap<>(PlayerColor.class);

        // 3. Place each player's pieces into their respective Base cell
        for (Player player : players) {
            playerMap.put(player.getColor(), player);
            Cell baseCell = board.getBaseCell(player.getColor());

            for (Piece piece : player.getPieces()) {
                piece.setInBase(true);
                piece.setCurrentCell(baseCell);
                baseCell.addPiece(piece);
            }
        }

        // 4. Retrieve Singleton Dice and Coin
        Dice dice = Dice.getInstance();
        Coin coin = Coin.getInstance();

        // 5. Package into GameState
        return new GameState(board, players, playerMap, dice, coin);
    }
}
