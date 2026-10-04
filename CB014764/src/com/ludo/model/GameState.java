package com.ludo.model;

import com.ludo.enums.PlayerColor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete initial or active state of the Ludo game.
 * Bundles the board, players, dice, and coin together.
 */
public class GameState {

    private final Board board;
    private final List<Player> players;
    private final Map<PlayerColor, Player> playerMap;
    private final Dice dice;
    private final Coin coin;
    private Player winner = null;

    public GameState(Board board, List<Player> players, Map<PlayerColor, Player> playerMap, Dice dice, Coin coin) {
        this.board = board;
        this.players = Collections.unmodifiableList(players);
        this.playerMap = Collections.unmodifiableMap(playerMap);
        this.dice = dice;
        this.coin = coin;
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayer(PlayerColor color) {
        return playerMap.get(color);
    }

    public Dice getDice() {
        return dice;
    }

    public Coin getCoin() {
        return coin;
    }

    public boolean isGameOver() {
        return winner != null;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }
}
