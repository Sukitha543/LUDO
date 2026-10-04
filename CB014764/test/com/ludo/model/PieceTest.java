package com.ludo.model;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.state.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Piece Unit Tests")
class PieceTest {

    private Piece piece;
    private Cell baseCell;
    private Cell trackCell;

    @BeforeEach
    void setUp() {
        piece = new Piece(PlayerColor.RED, 1);
        baseCell = new Cell("BASE_RED", CellType.BASE, PlayerColor.RED);
        trackCell = new Cell("10", CellType.STANDARD, null);
    }

    @Test
    @DisplayName("Verify piece default initialization properties")
    void testInitialProperties() {
        assertEquals("R1", piece.getName());
        assertEquals(1, piece.getId());
        assertEquals(PlayerColor.RED, piece.getColor());
        assertTrue(piece.isInBase(), "Piece must start in base");
        assertFalse(piece.isInHome(), "Piece must not start in home");
        assertNull(piece.getCurrentCell(), "Piece initially has no assigned cell");
        assertEquals(Direction.CLOCKWISE, piece.getDirection());
        assertEquals(Direction.CLOCKWISE, piece.getOriginalDirection());
        assertEquals(0, piece.getCaptureCount());
        assertEquals(0, piece.getBlockCaptureCount());
        assertEquals(0, piece.getApproachPassCount());
        assertInstanceOf(InBaseState.class, piece.getState(), "Initial state must be InBaseState");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("Verify piece constructor accepts valid IDs between 1 and 4")
    void testValidPieceIds(int validId) {
        assertDoesNotThrow(() -> new Piece(PlayerColor.YELLOW, validId));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 5, 10})
    @DisplayName("Verify piece constructor throws IllegalArgumentException for invalid IDs")
    void testInvalidPieceIdsThrowException(int invalidId) {
        assertThrows(IllegalArgumentException.class, () -> new Piece(PlayerColor.RED, invalidId),
                "Piece ID must be between 1 and 4");
    }

    @Test
    @DisplayName("Verify state transition when moving out of base and returning to base")
    void testBaseStateTransitions() {
        // Leave base: transition to NormalState
        piece.setInBase(false);
        assertFalse(piece.isInBase());
        assertInstanceOf(NormalState.class, piece.getState(), "Exiting base must transition to NormalState");

        // Return to base: transition back to InBaseState
        piece.setInBase(true);
        assertTrue(piece.isInBase());
        assertInstanceOf(InBaseState.class, piece.getState(), "Entering base must transition back to InBaseState");
    }

    @Test
    @DisplayName("Verify reaching home marks inHome and sets HomeState")
    void testReachHome() {
        piece.reachHome();

        assertTrue(piece.isInHome());
        assertInstanceOf(HomeState.class, piece.getState(), "Reaching home must set HomeState");
    }

    @Test
    @DisplayName("Verify applying buffs and debuffs (Energized, Sick, Briefing)")
    void testStateModifiers() {
        piece.energize();
        assertInstanceOf(EnergizedState.class, piece.getState());

        piece.makeSick();
        assertInstanceOf(SickState.class, piece.getState());

        piece.attendBriefing();
        assertInstanceOf(BriefingState.class, piece.getState());
    }

    @Test
    @DisplayName("Verify capture and pass counters increment accurately")
    void testCounterIncrements() {
        piece.incrementCaptureCount();
        piece.incrementCaptureCount();
        assertEquals(2, piece.getCaptureCount());

        piece.incrementBlockCaptureCount();
        assertEquals(1, piece.getBlockCaptureCount());

        piece.incrementApproachPassCount();
        piece.incrementApproachPassCount();
        piece.incrementApproachPassCount();
        assertEquals(3, piece.getApproachPassCount());

        piece.setApproachPassCount(10);
        assertEquals(10, piece.getApproachPassCount());
    }

    @Test
    @DisplayName("Verify piece reset restores default state and counters")
    void testReset() {
        // Mutate piece state
        piece.setInBase(false);
        piece.setCurrentCell(trackCell);
        piece.incrementCaptureCount();
        piece.incrementBlockCaptureCount();
        piece.incrementApproachPassCount();
        piece.setDirection(Direction.COUNTER_CLOCKWISE);
        piece.energize();

        // Reset with base cell
        piece.reset(baseCell);

        assertTrue(piece.isInBase());
        assertFalse(piece.isInHome());
        assertEquals(baseCell, piece.getCurrentCell());
        assertEquals(0, piece.getCaptureCount());
        assertEquals(0, piece.getBlockCaptureCount());
        assertEquals(0, piece.getApproachPassCount());
        assertEquals(Direction.CLOCKWISE, piece.getDirection());
        assertInstanceOf(InBaseState.class, piece.getState());
    }
}
