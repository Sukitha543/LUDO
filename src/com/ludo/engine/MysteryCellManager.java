package com.ludo.engine;

import com.ludo.model.Board;
import com.ludo.model.Cell;
import com.ludo.view.GameView;

import java.util.List;
import java.util.Random;

public class MysteryCellManager {

    public static final int SPAWN_AFTER_ROUNDS = 2;
    public static final int DURATION_ROUNDS = 4;

    private int roundsCompleted;
    private Cell activeMysteryCell;
    private Cell previousMysteryCell;
    private int roundsRemaining;
    private final Random random;

    public MysteryCellManager() {
        this(com.ludo.util.GameRandom.getInstance());
    }

    public MysteryCellManager(Random random) {
        this.roundsCompleted = 0;
        this.activeMysteryCell = null;
        this.previousMysteryCell = null;
        this.roundsRemaining = 0;
        this.random = (random != null) ? random : com.ludo.util.GameRandom.getInstance();
    }

    public void updateOnRoundEnd(Board board, GameView view) {
        roundsCompleted++;

        if (activeMysteryCell == null) {
            // Check if 2 rounds have passed
            if (roundsCompleted >= SPAWN_AFTER_ROUNDS) {
                spawnMysteryCell(board, view);
            }
        } else {
            // Already active on the board: decrement duration
            roundsRemaining--;
            if (roundsRemaining <= 0) {
                // 4 rounds elapsed: despawn and immediately relocate to another empty cell
                despawn(board);
                spawnMysteryCell(board, view);
            }
        }
    }

    public Cell spawnMysteryCell(Board board, GameView view) {
        if (board == null) {
            return null;
        }

        List<Cell> candidates = board.getStandardTrackCells().stream()
                .filter(cell -> !cell.isOccupied())
                .filter(cell -> cell != previousMysteryCell)
                .toList();

        if (candidates.isEmpty()) {
            // Fallback: any unoccupied standard cell
            candidates = board.getStandardTrackCells().stream()
                    .filter(cell -> !cell.isOccupied())
                    .toList();
        }

        if (candidates.isEmpty()) {
            return null;
        }

        Cell chosenCell = candidates.get(random.nextInt(candidates.size()));
        return placeMysteryCell(chosenCell, DURATION_ROUNDS, board, view);
    }

    public Cell spawnAt(Cell cell, int rounds, Board board, GameView view) {
        return placeMysteryCell(cell, rounds, board, view);
    }

    private Cell placeMysteryCell(Cell cell, int rounds, Board board, GameView view) {
        if (activeMysteryCell != null && board != null) {
            board.setMysteryCell(null);
        }

        this.previousMysteryCell = cell;
        this.activeMysteryCell = cell;
        this.roundsRemaining = rounds;

        if (board != null) {
            board.setMysteryCell(cell);
        }

        if (view != null && cell != null) {
            view.displayMysteryCellSpawned(cell, rounds);
        }

        return cell;
    }

    public void despawn(Board board) {
        if (activeMysteryCell != null) {
            if (board != null) {
                board.setMysteryCell(null);
            }
            this.activeMysteryCell = null;
            this.roundsRemaining = 0;
        }
    }

    public int getRoundsCompleted() {
        return roundsCompleted;
    }

    public void setRoundsCompleted(int roundsCompleted) {
        this.roundsCompleted = roundsCompleted;
    }

    public Cell getActiveMysteryCell() {
        return activeMysteryCell;
    }

    public Cell getPreviousMysteryCell() {
        return previousMysteryCell;
    }

    public int getRoundsRemaining() {
        return roundsRemaining;
    }

    public boolean isSpawned() {
        return activeMysteryCell != null;
    }
}
