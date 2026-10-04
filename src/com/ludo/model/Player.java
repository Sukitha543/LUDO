package com.ludo.model;

import com.ludo.enums.PlayerColor;
import com.ludo.strategy.PlayerStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a player in the Ludo game.
 * Each player has a specific color, 4 pieces, and a PlayerStrategy (Strategy
 * Pattern)
 * that defines their tactical decision making.
 */
public class Player {

    private final PlayerColor color;
    private final String name;
    private final List<Piece> pieces;
    private PlayerStrategy strategy;
    private int consecutiveThrees = 0;

    public Player(PlayerColor color, PlayerStrategy strategy) {
        this.color = color;
        this.name = color.getDisplayName() + " Player";
        this.strategy = strategy;
        this.pieces = new ArrayList<>(4);
        for (int i = 1; i <= 4; i++) {
            pieces.add(new Piece(color, i));
        }
    }

    public PlayerColor getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public List<Piece> getPieces() {
        return Collections.unmodifiableList(pieces);
    }

    public Piece getPiece(int id) {
        if (id < 1 || id > pieces.size()) {
            throw new IllegalArgumentException("Piece ID must be between 1 and " + pieces.size());
        }
        return pieces.get(id - 1);
    }

    public PlayerStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(PlayerStrategy strategy) {
        this.strategy = strategy;
    }

    public List<Piece> getPiecesInBase() {
        return pieces.stream().filter(Piece::isInBase).toList();
    }

    public List<Piece> getPiecesOnBoard() {
        return pieces.stream().filter(p -> !p.isInBase() && !p.isInHome()).toList();
    }

    public List<Piece> getPiecesInHome() {
        return pieces.stream().filter(Piece::isInHome).toList();
    }

    public boolean hasPiecesInBase() {
        return pieces.stream().anyMatch(Piece::isInBase);
    }

    public Move chooseMove(List<Move> legalMoves, Board board, int diceRoll) {
        if (strategy == null || legalMoves == null || legalMoves.isEmpty()) {
            return null;
        }
        return strategy.selectMove(this, legalMoves, board, diceRoll);
    }

    public int getConsecutiveThrees() {
        return consecutiveThrees;
    }

    public void incrementConsecutiveThrees() {
        this.consecutiveThrees++;
    }

    public void resetConsecutiveThrees() {
        this.consecutiveThrees = 0;
    }

    public boolean hasWon() {
        return getPiecesInHome().size() == 4;
    }

    @Override
    public String toString() {
        return name;
    }
}
