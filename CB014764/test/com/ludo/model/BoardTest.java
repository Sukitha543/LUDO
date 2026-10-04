package com.ludo.model;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.enums.TeleportDestination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Board Unit Tests")
class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    @Test
    @DisplayName("Verify board track initialization and constants")
    void testTrackInitialization() {
        assertEquals(52, Board.TOTAL_STANDARD_CELLS);
        assertEquals(5, Board.HOME_STRAIGHT_CELLS_PER_COLOR);

        List<Cell> standardTrack = board.getStandardTrackCells();
        assertNotNull(standardTrack);
        assertEquals(52, standardTrack.size());

        // Check index boundaries
        assertNotNull(board.getStandardCell(0));
        assertNotNull(board.getStandardCell(51));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 52, 100 })
    @DisplayName("Verify getStandardCell throws IndexOutOfBoundsException for invalid indices")
    void testStandardCellInvalidIndices(int invalidIndex) {
        assertThrows(IndexOutOfBoundsException.class, () -> board.getStandardCell(invalidIndex));
    }

    @Test
    @DisplayName("Verify player starting 'X' indices and approach indices per rulebook")
    void testPlayerSpecialIndices() {
        // Yellow: Start 2, Approach 0
        assertEquals(2, board.getStartingIndex(PlayerColor.YELLOW));
        assertEquals(0, board.getApproachIndex(PlayerColor.YELLOW));

        // Blue: Start 15, Approach 13
        assertEquals(15, board.getStartingIndex(PlayerColor.BLUE));
        assertEquals(13, board.getApproachIndex(PlayerColor.BLUE));

        // Red: Start 28, Approach 26
        assertEquals(28, board.getStartingIndex(PlayerColor.RED));
        assertEquals(26, board.getApproachIndex(PlayerColor.RED));

        // Green: Start 41, Approach 39
        assertEquals(41, board.getStartingIndex(PlayerColor.GREEN));
        assertEquals(39, board.getApproachIndex(PlayerColor.GREEN));
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    @DisplayName("Verify starting cell and approach cell types for all colors")
    void testStartingAndApproachCellTypes(PlayerColor color) {
        Cell startCell = board.getStartingCell(color);
        assertNotNull(startCell);
        assertEquals(CellType.STARTING_X, startCell.getType());
        assertEquals(color, startCell.getColor());

        Cell approachCell = board.getApproachCell(color);
        assertNotNull(approachCell);
        assertEquals(CellType.APPROACH, approachCell.getType());
        assertEquals(color, approachCell.getColor());
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    @DisplayName("Verify Home Straights, Bases, and Homes for each color")
    void testHomeStraightsAndBases(PlayerColor color) {
        // Base cell
        Cell baseCell = board.getBaseCell(color);
        assertNotNull(baseCell);
        assertEquals(CellType.BASE, baseCell.getType());
        assertEquals(color, baseCell.getColor());

        // Home destination cell
        Cell homeCell = board.getHomeCell(color);
        assertNotNull(homeCell);
        assertEquals(CellType.HOME, homeCell.getType());
        assertEquals(color, homeCell.getColor());

        // 5 Home Straight cells
        List<Cell> homeStraight = board.getHomeStraight(color);
        assertNotNull(homeStraight);
        assertEquals(5, homeStraight.size());

        for (int i = 0; i < 5; i++) {
            Cell pathCell = board.getHomeStraightCell(color, i);
            assertEquals(CellType.HOME_PATH, pathCell.getType());
            assertEquals(color, pathCell.getColor());
            assertEquals(color.getDisplayName().toLowerCase() + "homepath" + i, pathCell.getId());
        }

        // Boundary checks for home straight index
        assertThrows(IndexOutOfBoundsException.class, () -> board.getHomeStraightCell(color, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> board.getHomeStraightCell(color, 5));
    }

    @Test
    @DisplayName("Verify Teleportation destination cells (Alpha, Beta, Gamma, Base, Starting X, Approach)")
    void testTeleportDestinations() {
        // Yellow approach is 0 -> Alpha = 9, Beta = 27, Gamma = 46
        assertEquals("9", board.getAlphaCell().getId());
        assertEquals("27", board.getBetaCell().getId());
        assertEquals("46", board.getGammaCell().getId());

        assertEquals(board.getAlphaCell(), board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED));
        assertEquals(board.getBetaCell(), board.getTeleportCell(TeleportDestination.BETA, PlayerColor.RED));
        assertEquals(board.getGammaCell(), board.getTeleportCell(TeleportDestination.GAMMA, PlayerColor.RED));
        assertEquals(board.getBaseCell(PlayerColor.RED),
                board.getTeleportCell(TeleportDestination.BASE, PlayerColor.RED));
        assertEquals(board.getStartingCell(PlayerColor.RED),
                board.getTeleportCell(TeleportDestination.STARTING_X, PlayerColor.RED));
        assertEquals(board.getApproachCell(PlayerColor.RED),
                board.getTeleportCell(TeleportDestination.APPROACH, PlayerColor.RED));
        assertNull(board.getTeleportCell(null, PlayerColor.RED));
    }

    @Test
    @DisplayName("Verify Mystery cell placement and relocation state tracking")
    void testMysteryCellManagement() {
        Cell cell10 = board.getStandardCell(10);
        Cell cell20 = board.getStandardCell(20);

        assertNull(board.getMysteryCell());

        // Spawn on cell 10
        board.setMysteryCell(cell10);
        assertEquals(cell10, board.getMysteryCell());
        assertTrue(cell10.isMysteryCell());

        // Relocate to cell 20: old cell cleared, new cell flagged
        board.setMysteryCell(cell20);
        assertEquals(cell20, board.getMysteryCell());
        assertFalse(cell10.isMysteryCell(), "Old cell should no longer be mystery");
        assertTrue(cell20.isMysteryCell());

        // Despawn
        board.setMysteryCell(null);
        assertNull(board.getMysteryCell());
        assertFalse(cell20.isMysteryCell());
    }

    @Test
    @DisplayName("Verify circular track step calculations (Clockwise and Counter-Clockwise wrapping)")
    void testCircularTrackWrapping() {
        Piece cwPiece = new Piece(PlayerColor.RED, 1);
        cwPiece.setDirection(Direction.CLOCKWISE);

        // Move 1 step from cell 51 CW -> wraps to 0
        Cell nextFrom51 = board.getNextCell(cwPiece, board.getStandardCell(51), false, false);
        assertEquals("0", nextFrom51.getId());

        Piece ccwPiece = new Piece(PlayerColor.RED, 2);
        ccwPiece.setDirection(Direction.COUNTER_CLOCKWISE);

        // Move 1 step from cell 0 CCW -> wraps to 51
        Cell nextFrom0 = board.getNextCell(ccwPiece, board.getStandardCell(0), false, false);
        assertEquals("51", nextFrom0.getId());
    }

    @Test
    @DisplayName("Verify calculateLandingCell and getPath advance correctly")
    void testLandingCellAndPathCalculation() {
        Piece piece = new Piece(PlayerColor.YELLOW, 1);
        piece.setDirection(Direction.CLOCKWISE);

        Cell startCell = board.getStandardCell(10);
        Cell landingCell = board.calculateLandingCell(piece, startCell, 4, false);
        assertEquals(board.getStandardCell(14), landingCell);

        List<Cell> path = board.getPath(piece, startCell, 4);
        assertEquals(4, path.size());
        assertEquals("11", path.get(0).getId());
        assertEquals("12", path.get(1).getId());
        assertEquals("13", path.get(2).getId());
        assertEquals("14", path.get(3).getId());
    }

    @Test
    @DisplayName("Verify Home Straight entry qualification rules (capture requirements and CCW 2nd pass rule)")
    void testHomeStraightEntryEligibility() {
        Piece redPiece = new Piece(PlayerColor.RED, 1);

        // 1. No captures -> cannot enter home straight
        assertFalse(board.canEnterHomeStraight(redPiece), "Piece with 0 captures cannot enter home straight");

        // 2. With capture + Clockwise -> can enter
        redPiece.incrementCaptureCount();
        assertTrue(board.canEnterHomeStraight(redPiece));

        // 3. With capture + Counter-Clockwise on 1st pass -> CANNOT enter
        redPiece.setDirection(Direction.COUNTER_CLOCKWISE);
        assertFalse(board.canEnterHomeStraight(redPiece), "CCW piece requires passing approach cell before entering");

        // 4. With capture + Counter-Clockwise on 2nd pass -> CAN enter
        redPiece.incrementApproachPassCount();
        assertTrue(board.canEnterHomeStraight(redPiece), "CCW piece on 2nd pass can enter home straight");
    }

    @Test
    @DisplayName("Verify blockade tracking on board for friendly pieces")
    void testBlockadeTracking() {
        Piece red1 = new Piece(PlayerColor.RED, 1);
        Piece red2 = new Piece(PlayerColor.RED, 2);
        Piece blue1 = new Piece(PlayerColor.BLUE, 1);

        Cell cell10 = board.getStandardCell(10);

        assertFalse(board.hasBlockade(PlayerColor.RED));
        assertTrue(board.getBlockades(PlayerColor.RED).isEmpty());

        // 1 friendly piece -> no blockade
        cell10.addPiece(red1);
        assertFalse(board.hasBlockade(PlayerColor.RED));

        // 1 friendly + 1 enemy piece on same cell -> is a cell block, but NOT a
        // friendly blockade
        cell10.addPiece(blue1);
        assertTrue(cell10.isBlock());
        assertFalse(board.hasBlockade(PlayerColor.RED));

        // 2 friendly pieces -> friendly blockade formed!
        cell10.addPiece(red2);
        assertTrue(board.hasBlockade(PlayerColor.RED));
        List<Cell> redBlockades = board.getBlockades(PlayerColor.RED);
        assertEquals(1, redBlockades.size());
        assertEquals(cell10, redBlockades.get(0));
    }
}
