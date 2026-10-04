package com.ludo.model;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.enums.TeleportDestination;

import java.util.*;

/**
 * Represents the Ludo game board.
 * - 52 standard cells on the outer circular track (indexed 0 to 51).
 * Numbering starts with the Yellow starting square ('X' = 0) and continues
 * clockwise.
 * - 20 color-specific cells (5 Home Straight cells per color).
 * Named according to the specification: [colour]homepath[0-4].
 * - Dedicated Approach cells, Starting 'X' cells, Bases, and Home destinations.
 */
public class Board {

    public static final int TOTAL_STANDARD_CELLS = 52;
    public static final int HOME_STRAIGHT_CELLS_PER_COLOR = 5;

    // 52 standard cells (0 to 51)
    private final Cell[] standardTrack = new Cell[TOTAL_STANDARD_CELLS];

    // 5 home straight cells per color (total 20 cells)
    private final Map<PlayerColor, List<Cell>> homeStraights = new EnumMap<>(PlayerColor.class);

    // Starting 'X' square index on standard track for each color
    private final Map<PlayerColor, Integer> startingIndices = new EnumMap<>(PlayerColor.class);

    // Approach square index on standard track for each color
    private final Map<PlayerColor, Integer> approachIndices = new EnumMap<>(PlayerColor.class);

    // Base and Home destination cells for each color
    private final Map<PlayerColor, Cell> baseCells = new EnumMap<>(PlayerColor.class);
    private final Map<PlayerColor, Cell> homeCells = new EnumMap<>(PlayerColor.class);

    public Board() {
        initializeIndices();
        initializeStandardTrack();
        initializeHomeStraights();
        initializeBasesAndHomes();
    }

    private void initializeIndices() {
        // Numbering starts with Yellow approach cell = 0 and continues clockwise.
        // Yellow starts at 2, Blue at 15, Red at 28, Green at 41 (clockwise)
        startingIndices.put(PlayerColor.YELLOW, 2);
        startingIndices.put(PlayerColor.BLUE, 15);
        startingIndices.put(PlayerColor.RED, 28);
        startingIndices.put(PlayerColor.GREEN, 41);

        // Approach cells: Yellow is 0, Blue is 13, Red is 26, Green is 39
        approachIndices.put(PlayerColor.YELLOW, 0);
        approachIndices.put(PlayerColor.BLUE, 13);
        approachIndices.put(PlayerColor.RED, 26);
        approachIndices.put(PlayerColor.GREEN, 39);
    }

    private void initializeStandardTrack() {
        // Map to identify which indices belong to starting X or approach cells
        Map<Integer, PlayerColor> startMap = new HashMap<>();
        startingIndices.forEach((color, idx) -> startMap.put(idx, color));

        Map<Integer, PlayerColor> approachMap = new HashMap<>();
        approachIndices.forEach((color, idx) -> approachMap.put(idx, color));

        for (int i = 0; i < TOTAL_STANDARD_CELLS; i++) {
            String cellId = String.valueOf(i);
            if (startMap.containsKey(i)) {
                standardTrack[i] = new Cell(cellId, CellType.STARTING_X, startMap.get(i));
            } else if (approachMap.containsKey(i)) {
                standardTrack[i] = new Cell(cellId, CellType.APPROACH, approachMap.get(i));
            } else {
                standardTrack[i] = new Cell(cellId, CellType.STANDARD, null);
            }
        }
    }

    private void initializeHomeStraights() {
        for (PlayerColor color : PlayerColor.values()) {
            List<Cell> path = new ArrayList<>(HOME_STRAIGHT_CELLS_PER_COLOR);
            String prefix = color.getDisplayName().toLowerCase() + "homepath";
            for (int i = 0; i < HOME_STRAIGHT_CELLS_PER_COLOR; i++) {
                String cellId = prefix + i;
                path.add(new Cell(cellId, CellType.HOME_PATH, color));
            }
            homeStraights.put(color, Collections.unmodifiableList(path));
        }
    }

    private void initializeBasesAndHomes() {
        for (PlayerColor color : PlayerColor.values()) {
            baseCells.put(color, new Cell(color.getDisplayName() + " Base", CellType.BASE, color));
            homeCells.put(color, new Cell(color.getDisplayName() + " Home", CellType.HOME, color));
        }
    }

    public Cell getStandardCell(int index) {
        if (index < 0 || index >= TOTAL_STANDARD_CELLS) {
            throw new IndexOutOfBoundsException("Standard cell index must be between 0 and 51. Given: " + index);
        }
        return standardTrack[index];
    }

    public int getStartingIndex(PlayerColor color) {
        return startingIndices.get(color);
    }

    public Cell getStartingCell(PlayerColor color) {
        return standardTrack[getStartingIndex(color)];
    }

    public int getApproachIndex(PlayerColor color) {
        return approachIndices.get(color);
    }

    public Cell getApproachCell(PlayerColor color) {
        return standardTrack[getApproachIndex(color)];
    }

    public List<Cell> getHomeStraight(PlayerColor color) {
        return homeStraights.get(color);
    }

    public Cell getHomeStraightCell(PlayerColor color, int index) {
        if (index < 0 || index >= HOME_STRAIGHT_CELLS_PER_COLOR) {
            throw new IndexOutOfBoundsException("Home straight index must be between 0 and 4. Given: " + index);
        }
        return homeStraights.get(color).get(index);
    }

    public Cell getBaseCell(PlayerColor color) {
        return baseCells.get(color);
    }

    public Cell getHomeCell(PlayerColor color) {
        return homeCells.get(color);
    }

    /**
     * Alpha is 9th cell from Yellow approach cell (Rule T-11).
     */
    public Cell getAlphaCell() {
        int idx = (approachIndices.get(PlayerColor.YELLOW) + 9) % TOTAL_STANDARD_CELLS;
        return standardTrack[idx];
    }

    /**
     * Beta is 27th cell from Yellow approach cell (Rule T-11).
     */
    public Cell getBetaCell() {
        int idx = (approachIndices.get(PlayerColor.YELLOW) + 27) % TOTAL_STANDARD_CELLS;
        return standardTrack[idx];
    }

    /**
     * Gamma is 46th cell from Yellow approach cell (Rule T-11).
     */
    public Cell getGammaCell() {
        int idx = (approachIndices.get(PlayerColor.YELLOW) + 46) % TOTAL_STANDARD_CELLS;
        return standardTrack[idx];
    }

    /**
     * Resolves the target Cell for a given TeleportDestination (Rule T-11 / Step
     * 13).
     */
    public Cell getTeleportCell(TeleportDestination destination, PlayerColor color) {
        if (destination == null) {
            return null;
        }
        switch (destination) {
            case ALPHA:
                return getAlphaCell();
            case BETA:
                return getBetaCell();
            case GAMMA:
                return getGammaCell();
            case BASE:
                return getBaseCell(color);
            case STARTING_X:
                return getStartingCell(color);
            case APPROACH:
                return getApproachCell(color);
            default:
                throw new IllegalArgumentException("Unknown teleport destination: " + destination);
        }
    }

    // Mystery Cell tracking (Rule T-10)
    private Cell mysteryCell;

    public Cell getMysteryCell() {
        return mysteryCell;
    }

    public void setMysteryCell(Cell mysteryCell) {
        if (this.mysteryCell != null) {
            this.mysteryCell.setMysteryCell(false);
        }
        this.mysteryCell = mysteryCell;
        if (this.mysteryCell != null) {
            this.mysteryCell.setMysteryCell(true);
        }
    }

    public List<Cell> getStandardTrackCells() {
        return Collections.unmodifiableList(Arrays.asList(standardTrack));
    }

    /**
     * Determines whether a piece is eligible to enter the Home Straight (Rule T-1).
     * - Clockwise pieces can enter when passing Approach.
     * - Counter-Clockwise pieces can ONLY enter if they have passed Approach for
     * the 2nd time
     * (approachPassCount >= 1).
     */
    public boolean canEnterHomeStraight(Piece piece) {
        if (piece.getCaptureCount() < 1) {
            return false;
        }
        if (piece.getDirection() == Direction.CLOCKWISE) {
            return true;
        } else {
            return piece.getApproachPassCount() >= 1;
        }
    }

    /**
     * Computes the next cell along the board according to piece direction and Home
     * Straight rules.
     */
    public Cell getNextCell(Piece piece, Cell fromCell, boolean recordPass, boolean startedAtApproach) {
        if (fromCell == null || fromCell.getType() == CellType.HOME) {
            return null;
        }

        // 1. In Home Straight: advances toward Home
        if (fromCell.getType() == CellType.HOME_PATH) {
            String id = fromCell.getId();
            try {
                int pathIndex = Integer.parseInt(id.replaceAll("\\D+", ""));
                if (pathIndex == HOME_STRAIGHT_CELLS_PER_COLOR - 1) {
                    return getHomeCell(piece.getColor());
                }
                return getHomeStraightCell(piece.getColor(), pathIndex + 1);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // 2. On Standard Track: check for Approach cell turn
        Cell approachCell = getApproachCell(piece.getColor());
        boolean isAtApproach = (fromCell == approachCell);

        if (isAtApproach) {
            if (startedAtApproach && canEnterHomeStraight(piece)) {
                // Allowed into Home Straight!
                return getHomeStraightCell(piece.getColor(), 0);
            } else {
                // Denied into Home Straight (1st pass of CCW, lack of captures, or didn't start
                // at approach):
                // Piece must continue along the standard track
                if (piece.getDirection() == Direction.COUNTER_CLOCKWISE && recordPass && !startedAtApproach) {
                    piece.incrementApproachPassCount();
                }
            }
        }

        // Standard circular step
        try {
            int currentIdx = Integer.parseInt(fromCell.getId());
            int nextIdx;
            if (piece.getDirection() == Direction.CLOCKWISE) {
                nextIdx = (currentIdx + 1) % TOTAL_STANDARD_CELLS;
            } else {
                nextIdx = (currentIdx - 1 + TOTAL_STANDARD_CELLS) % TOTAL_STANDARD_CELLS;
            }
            return getStandardCell(nextIdx);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Calculates the landing cell after moving N steps.
     */
    public Cell calculateLandingCell(Piece piece, Cell startCell, int steps, boolean recordPass) {
        Cell current = startCell;
        boolean startedAtApproach = (startCell == getApproachCell(piece.getColor()));
        for (int i = 0; i < steps; i++) {
            current = getNextCell(piece, current, recordPass, startedAtApproach);
            if (current == null) {
                return null; // Overshot home
            }
        }
        return current;
    }

    /**
     * Returns the sequence of cells along the trajectory when advancing 'steps'
     * from 'startCell'.
     */
    public List<Cell> getPath(Piece piece, Cell startCell, int steps) {
        List<Cell> path = new ArrayList<>(steps);
        Cell current = startCell;
        boolean startedAtApproach = (startCell == getApproachCell(piece.getColor()));
        for (int i = 0; i < steps; i++) {
            current = getNextCell(piece, current, false, startedAtApproach);
            if (current == null) {
                break;
            }
            path.add(current);
        }
        return path;
    }

    /**
     * Calculates the direction of the longest distance from home for a player from
     * the specified cell (Step 9 / Rule T-4).
     */
    public Direction getDirectionOfLongestDistanceFromHome(PlayerColor color, Cell cell) {
        int distCW = getDistanceFromHome(color, cell, Direction.CLOCKWISE);
        int distCCW = getDistanceFromHome(color, cell, Direction.COUNTER_CLOCKWISE);

        if (distCW > distCCW) {
            return Direction.CLOCKWISE;
        } else if (distCCW > distCW) {
            return Direction.COUNTER_CLOCKWISE;
        } else {
            return Direction.CLOCKWISE; // Default if equidistant
        }
    }

    /**
     * Calculates the distance (in cells) to the Approach cell in the given
     * direction.
     */
    public int getDistanceFromHome(PlayerColor color, Cell cell, Direction direction) {
        if (cell.getType() != CellType.STANDARD && cell.getType() != CellType.STARTING_X
                && cell.getType() != CellType.APPROACH) {
            return 0;
        }

        try {
            int currentIdx = Integer.parseInt(cell.getId());
            int approachIdx = getApproachIndex(color);

            if (direction == Direction.CLOCKWISE) {
                return (approachIdx - currentIdx + TOTAL_STANDARD_CELLS) % TOTAL_STANDARD_CELLS;
            } else {
                return (currentIdx - approachIdx + TOTAL_STANDARD_CELLS) % TOTAL_STANDARD_CELLS;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Calculates the next cell when moving in a specific direction.
     */
    public Cell getNextCellForDirection(Cell fromCell, Direction direction, PlayerColor color, List<Piece> blockPieces,
            boolean recordPass, boolean startedAtApproach) {
        if (fromCell == null || fromCell.getType() == CellType.HOME) {
            return null;
        }

        if (fromCell.getType() == CellType.HOME_PATH) {
            String id = fromCell.getId();
            try {
                int pathIndex = Integer.parseInt(id.replaceAll("\\D+", ""));
                if (pathIndex == HOME_STRAIGHT_CELLS_PER_COLOR - 1) {
                    return getHomeCell(color);
                }
                return getHomeStraightCell(color, pathIndex + 1);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Cell approachCell = getApproachCell(color);
        if (fromCell == approachCell) {
            boolean hasBlockCapture = false;
            if (blockPieces != null && !blockPieces.isEmpty()) {
                for (Piece p : blockPieces) {
                    if (p.getBlockCaptureCount() >= 1) {
                        hasBlockCapture = true;
                        break;
                    }
                }
            }

            if (!hasBlockCapture || !startedAtApproach) {
                // Denied due to lack of block capture or not starting at approach cell!
                // Continues on standard track.
                if (direction == Direction.COUNTER_CLOCKWISE && recordPass && blockPieces != null) {
                    for (Piece p : blockPieces) {
                        p.incrementApproachPassCount();
                    }
                }
            } else {
                if (direction == Direction.CLOCKWISE) {
                    return getHomeStraightCell(color, 0);
                } else {
                    // Counter-clockwise block
                    boolean canEnter = false;
                    if (blockPieces != null && !blockPieces.isEmpty()) {
                        // Check if the block has passed approach cell before.
                        for (Piece p : blockPieces) {
                            if (p.getApproachPassCount() >= 1) {
                                canEnter = true;
                                break;
                            }
                        }
                    }

                    if (canEnter) {
                        return getHomeStraightCell(color, 0);
                    } else {
                        if (recordPass && blockPieces != null) {
                            for (Piece p : blockPieces) {
                                p.incrementApproachPassCount();
                            }
                        }
                    }
                }
            }
        }

        try {
            int currentIdx = Integer.parseInt(fromCell.getId());
            int nextIdx;
            if (direction == Direction.CLOCKWISE) {
                nextIdx = (currentIdx + 1) % TOTAL_STANDARD_CELLS;
            } else {
                nextIdx = (currentIdx - 1 + TOTAL_STANDARD_CELLS) % TOTAL_STANDARD_CELLS;
            }
            return getStandardCell(nextIdx);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Calculates landing cell when moving 'steps' units in a specific direction.
     */
    public Cell calculateLandingCellForDirection(Cell startCell, int steps, Direction direction, PlayerColor color,
            List<Piece> blockPieces, boolean recordPass) {
        Cell current = startCell;
        boolean startedAtApproach = (startCell == getApproachCell(color));
        for (int i = 0; i < steps; i++) {
            current = getNextCellForDirection(current, direction, color, blockPieces, recordPass, startedAtApproach);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Finds all cells on the board containing a blockade (2 or more pieces) of the
     * specified player color.
     */
    public List<Cell> getBlockades(PlayerColor color) {
        List<Cell> blockades = new ArrayList<>();
        for (Cell cell : standardTrack) {
            if (cell.isBlock()) {
                long count = cell.getOccupyingPieces().stream()
                        .filter(p -> p.getColor() == color)
                        .count();
                if (count >= 2) {
                    blockades.add(cell);
                }
            }
        }
        List<Cell> hs = homeStraights.get(color);
        if (hs != null) {
            for (Cell cell : hs) {
                if (cell.isBlock()) {
                    long count = cell.getOccupyingPieces().stream()
                            .filter(p -> p.getColor() == color)
                            .count();
                    if (count >= 2) {
                        blockades.add(cell);
                    }
                }
            }
        }
        return blockades;
    }

    /**
     * Checks if the specified player has at least one blockade on the board.
     */
    public boolean hasBlockade(PlayerColor color) {
        return !getBlockades(color).isEmpty();
    }

}
