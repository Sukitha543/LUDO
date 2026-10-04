package com.ludo.model;

import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.state.*;

/**
 * Represents a Ludo playing piece.
 * Each player has four pieces numbered 1 to 4 (e.g., R1, R2, R3, R4).
 * Uses the State Pattern (PieceState) to manage buffs, debuffs, base, and home
 * states dynamically.
 */
public class Piece {

    private final String name;
    private final int id;
    private final PlayerColor color;
    private boolean inBase;
    private boolean inHome;
    private Cell currentCell;
    private Direction direction;
    private Direction originalDirection;
    private int captureCount;
    private int blockCaptureCount;
    private int approachPassCount;
    private PieceState state;

    public Piece(PlayerColor color, int id) {
        if (id < 1 || id > 4) {
            throw new IllegalArgumentException("Piece ID must be between 1 and 4.");
        }
        this.color = color;
        this.id = id;
        this.name = color.getPrefix() + id;
        this.inBase = true;
        this.inHome = false;
        this.currentCell = null;
        this.direction = Direction.CLOCKWISE;
        this.originalDirection = Direction.CLOCKWISE;
        this.captureCount = 0;
        this.blockCaptureCount = 0;
        this.approachPassCount = 0;
        this.state = new InBaseState();
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public PlayerColor getColor() {
        return color;
    }

    public boolean isInBase() {
        return inBase;
    }

    public void setInBase(boolean inBase) {
        this.inBase = inBase;
        if (inBase) {
            this.state = new InBaseState();
        } else if (this.state instanceof InBaseState) {
            this.state = new NormalState();
        }
    }

    public boolean isInHome() {
        return inHome;
    }

    public void setInHome(boolean inHome) {
        this.inHome = inHome;
        if (inHome) {
            this.state = new HomeState();
        }
    }

    public Cell getCurrentCell() {
        return currentCell;
    }

    public void setCurrentCell(Cell currentCell) {
        this.currentCell = currentCell;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Direction getOriginalDirection() {
        return originalDirection;
    }

    public void setOriginalDirection(Direction originalDirection) {
        this.originalDirection = originalDirection;
    }

    public int getCaptureCount() {
        return captureCount;
    }

    public void incrementCaptureCount() {
        this.captureCount++;
    }

    public int getBlockCaptureCount() {
        return blockCaptureCount;
    }

    public void incrementBlockCaptureCount() {
        this.blockCaptureCount++;
    }

    public int getApproachPassCount() {
        return approachPassCount;
    }

    public void incrementApproachPassCount() {
        this.approachPassCount++;
    }

    public void setApproachPassCount(int approachPassCount) {
        this.approachPassCount = approachPassCount;
    }

    // State Pattern methods & delegations
    public PieceState getState() {
        return state;
    }

    public void setState(PieceState state) {
        this.state = state;
    }

    public boolean canMove(int diceRoll) {
        return state.canMove(this, diceRoll);
    }

    public int calculateMovementDistance(int diceRoll) {
        return state.calculateMovementDistance(diceRoll);
    }

    public void onRoundEnd() {
        state.onRoundEnd(this);
    }

    public void energize() {
        setState(new EnergizedState());
    }

    public void makeSick() {
        setState(new SickState());
    }

    public void attendBriefing() {
        setState(new BriefingState());
    }

    public void reachHome() {
        setInHome(true);
        setState(new HomeState());
    }

    public void reset(Cell baseCell) {
        this.inBase = true;
        this.inHome = false;
        this.currentCell = baseCell;
        this.captureCount = 0;
        this.blockCaptureCount = 0;
        this.approachPassCount = 0;
        this.direction = Direction.CLOCKWISE;
        this.originalDirection = Direction.CLOCKWISE;
        this.state = new InBaseState();
    }

    public void reset() {
        reset(null);
    }

    @Override
    public String toString() {
        return name + " [" + state.getStateName() + "]";
    }
}
