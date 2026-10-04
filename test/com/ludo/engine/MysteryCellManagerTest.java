package com.ludo.engine;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Board;
import com.ludo.model.Cell;
import com.ludo.model.Piece;
import com.ludo.view.GameView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MysteryCellManager Unit Tests (Rule T-10)")
class MysteryCellManagerTest {

    private Board board;
    private MysteryCellManager manager;

    @BeforeEach
    void setUp() {
        board = new Board();
        manager = new MysteryCellManager();
    }

    // =========================================================================
    // Category 1: Construction & Initial State
    // =========================================================================

    @Test
    @DisplayName("Verify initial state before simulation begins")
    void testInitialStateNotSpawned() {
        assertEquals(0, manager.getRoundsCompleted(), "Initial rounds completed should be 0");
        assertNull(manager.getActiveMysteryCell(), "Initial active mystery cell should be null");
        assertNull(manager.getPreviousMysteryCell(), "Initial previous mystery cell should be null");
        assertEquals(0, manager.getRoundsRemaining(), "Initial rounds remaining should be 0");
        assertFalse(manager.isSpawned(), "isSpawned should be false initially");
        assertNull(board.getMysteryCell(), "Board mystery cell should be null initially");
    }

    @Test
    @DisplayName("Verify constructor accepts custom Random instance")
    void testConstructorWithCustomRandom() {
        Random customRandom = new Random(12345L);
        MysteryCellManager customManager = new MysteryCellManager(customRandom);

        assertEquals(0, customManager.getRoundsCompleted());
        assertFalse(customManager.isSpawned());
    }

    @Test
    @DisplayName("Verify constructor with null Random safely falls back to GameRandom")
    void testConstructorWithNullRandomFallsBack() {
        MysteryCellManager fallbackManager = new MysteryCellManager(null);
        assertDoesNotThrow(() -> fallbackManager.spawnMysteryCell(board, null),
                "Should gracefully fall back when passed null Random");
    }

    // =========================================================================
    // Category 2: Round Tracking & Spawning Lifecycle (Rule T-10)
    // =========================================================================

    @Test
    @DisplayName("Verify mystery cell does NOT spawn at round 1")
    void testDoesNotSpawnBeforeRoundTwo() {
        manager.updateOnRoundEnd(board, null);

        assertEquals(1, manager.getRoundsCompleted(), "Rounds completed should be 1");
        assertFalse(manager.isSpawned(), "Mystery cell must not spawn after only 1 round");
        assertNull(manager.getActiveMysteryCell());
        assertNull(board.getMysteryCell());
    }

    @Test
    @DisplayName("Verify mystery cell spawns after round 2 (Rule T-10: SPAWN_AFTER_ROUNDS)")
    void testSpawnsAfterRoundTwo() {
        manager.updateOnRoundEnd(board, null); // Round 1
        manager.updateOnRoundEnd(board, null); // Round 2

        assertEquals(2, manager.getRoundsCompleted(), "Rounds completed should be 2");
        assertTrue(manager.isSpawned(), "Mystery cell must spawn after round 2");
        assertNotNull(manager.getActiveMysteryCell(), "Active mystery cell should not be null");
        assertEquals(MysteryCellManager.DURATION_ROUNDS, manager.getRoundsRemaining(),
                "Active duration should be initialized to 4 rounds");
        assertSame(manager.getActiveMysteryCell(), board.getMysteryCell(),
                "Board and Manager must reference the same active mystery cell");
        assertTrue(manager.getActiveMysteryCell().isMysteryCell(),
                "Cell property isMysteryCell() must be true");
    }

    @Test
    @DisplayName("Verify mystery cell remains active at the same cell for exactly 4 rounds")
    void testFourRoundDuration() {
        manager.updateOnRoundEnd(board, null); // Round 1
        manager.updateOnRoundEnd(board, null); // Round 2 -> Spawns

        Cell initialCell = manager.getActiveMysteryCell();
        assertNotNull(initialCell);

        // Round 3 (Duration: 3 remaining)
        manager.updateOnRoundEnd(board, null);
        assertEquals(3, manager.getRoundsRemaining());
        assertSame(initialCell, manager.getActiveMysteryCell());

        // Round 4 (Duration: 2 remaining)
        manager.updateOnRoundEnd(board, null);
        assertEquals(2, manager.getRoundsRemaining());
        assertSame(initialCell, manager.getActiveMysteryCell());

        // Round 5 (Duration: 1 remaining)
        manager.updateOnRoundEnd(board, null);
        assertEquals(1, manager.getRoundsRemaining());
        assertSame(initialCell, manager.getActiveMysteryCell());

        // Round 6 (Duration expired: despawns and relocates to new cell)
        manager.updateOnRoundEnd(board, null);
        assertEquals(MysteryCellManager.DURATION_ROUNDS, manager.getRoundsRemaining());
        assertNotNull(manager.getActiveMysteryCell());
        assertNotSame(initialCell, manager.getActiveMysteryCell(),
                "Mystery cell must relocate after 4 active rounds");
    }

    // =========================================================================
    // Category 3: Relocation & Non-Consecutive Placement Constraints
    // =========================================================================

    @Test
    @DisplayName("Verify despawn from old cell and immediate relocation to new cell after 4 rounds")
    void testDespawnsAndRelocatesAfterFourRounds() {
        manager.updateOnRoundEnd(board, null); // Round 1
        manager.updateOnRoundEnd(board, null); // Round 2 -> Spawns

        Cell firstCell = manager.getActiveMysteryCell();

        // Advance 3 rounds: 3, 4, 5 (remains active at firstCell)
        for (int r = 3; r <= 5; r++) {
            manager.updateOnRoundEnd(board, null);
            assertSame(firstCell, manager.getActiveMysteryCell());
            assertSame(firstCell, manager.getPreviousMysteryCell());
        }

        // Round 6: 4 rounds elapsed -> despawns and relocates to new cell
        manager.updateOnRoundEnd(board, null);

        Cell secondCell = manager.getActiveMysteryCell();
        assertNotSame(firstCell, secondCell, "New mystery cell must not be at the same cell as previous");
        assertFalse(firstCell.isMysteryCell(), "Old cell must no longer have mystery cell flag");
        assertTrue(secondCell.isMysteryCell(), "New cell must have mystery cell flag set");
        assertSame(secondCell, board.getMysteryCell(), "Board must reference new active mystery cell");
        assertSame(secondCell, manager.getPreviousMysteryCell(),
                "Previous cell tracker is updated to secondCell to prevent it from being picked consecutively on next spawn");
    }

    @Test
    @DisplayName("Verify mystery cell never appears in the same cell consecutively across multiple cycles")
    void testNeverConsecutiveCellOverMultipleRelocations() {
        manager.updateOnRoundEnd(board, null); // Round 1
        manager.updateOnRoundEnd(board, null); // Round 2 -> Initial spawn

        Cell current = manager.getActiveMysteryCell();

        // Run through 5 relocation cycles (each 4 rounds = 20 rounds)
        for (int cycle = 0; cycle < 5; cycle++) {
            for (int r = 0; r < 4; r++) {
                manager.updateOnRoundEnd(board, null);
            }
            Cell next = manager.getActiveMysteryCell();
            assertNotSame(current, next, "Mystery cell cannot appear in the same place consecutively (Cycle " + cycle + ")");
            current = next;
        }
    }

    @Test
    @DisplayName("Verify fallback relocation when all standard cells except previous cell are occupied")
    void testFallbackWhenAllOtherStandardCellsOccupied() {
        // Occupy cells 0 to 50 (51 cells)
        for (int i = 0; i <= 50; i++) {
            Cell cell = board.getStandardCell(i);
            Piece p = new Piece(PlayerColor.RED, 1);
            p.setInBase(false);
            cell.addPiece(p);
        }

        // Cell 51 is the only unoccupied cell
        Cell cell51 = board.getStandardCell(51);
        manager.spawnAt(cell51, 4, board, null);
        assertEquals(cell51, manager.getPreviousMysteryCell());

        // Now candidates excluding cell51 is empty, triggering the fallback path
        Cell relocated = manager.spawnMysteryCell(board, null);
        assertNotNull(relocated, "Fallback must successfully select available unoccupied cell");
        assertSame(cell51, relocated);
    }

    @Test
    @DisplayName("Verify spawnMysteryCell returns null when all 52 standard cells are occupied")
    void testAllCellsOccupiedReturnsNull() {
        for (int i = 0; i < Board.TOTAL_STANDARD_CELLS; i++) {
            Cell cell = board.getStandardCell(i);
            Piece p = new Piece(PlayerColor.BLUE, 1);
            p.setInBase(false);
            cell.addPiece(p);
        }

        Cell result = manager.spawnMysteryCell(board, null);
        assertNull(result, "When all 52 cells are occupied, spawnMysteryCell must return null");
    }

    // =========================================================================
    // Category 4: Placement Constraints & Board Synchronization
    // =========================================================================

    @Test
    @DisplayName("Verify mystery cell only spawns on standard track cells (never base or home)")
    void testOnlySpawnsOnStandardTrack() {
        List<Cell> standardCells = board.getStandardTrackCells();

        for (int i = 0; i < 20; i++) {
            manager.despawn(board);
            Cell spawned = manager.spawnMysteryCell(board, null);
            assertNotNull(spawned);
            assertTrue(standardCells.contains(spawned),
                    "Mystery cell must be on one of the 52 standard track cells");
        }
    }

    @Test
    @DisplayName("Verify mystery cell never spawns on an occupied cell")
    void testNeverSpawnsOnOccupiedCell() {
        // Occupy cells 0 to 20
        for (int i = 0; i <= 20; i++) {
            Cell cell = board.getStandardCell(i);
            Piece p = new Piece(PlayerColor.GREEN, 1);
            p.setInBase(false);
            cell.addPiece(p);
        }

        for (int i = 0; i < 30; i++) {
            manager.despawn(board);
            Cell spawned = manager.spawnMysteryCell(board, null);
            assertNotNull(spawned);
            assertFalse(spawned.isOccupied(), "Spawned mystery cell must never be occupied by a piece");
        }
    }

    @Test
    @DisplayName("Verify despawn clears manager state and board synchronization")
    void testDespawnClearsBoardAndManager() {
        Cell cell = board.getStandardCell(15);
        manager.spawnAt(cell, 4, board, null);

        assertTrue(manager.isSpawned());
        assertTrue(cell.isMysteryCell());
        assertSame(cell, board.getMysteryCell());

        manager.despawn(board);

        assertFalse(manager.isSpawned(), "isSpawned must be false after despawn");
        assertNull(manager.getActiveMysteryCell(), "Active mystery cell must be null after despawn");
        assertEquals(0, manager.getRoundsRemaining(), "Rounds remaining must be 0 after despawn");
        assertNull(board.getMysteryCell(), "Board mystery cell must be cleared");
        assertFalse(cell.isMysteryCell(), "Cell flag must be cleared");
    }

    @Test
    @DisplayName("Verify despawn when not spawned is a safe no-op")
    void testDespawnWhenNotSpawnedIsNoOp() {
        assertDoesNotThrow(() -> manager.despawn(board));
        assertNull(manager.getActiveMysteryCell());
    }

    @Test
    @DisplayName("Verify despawn with null board does not throw NullPointerException")
    void testDespawnWithNullBoardDoesNotThrow() {
        Cell cell = board.getStandardCell(10);
        manager.spawnAt(cell, 4, board, null);

        assertDoesNotThrow(() -> manager.despawn(null));
        assertNull(manager.getActiveMysteryCell());
        assertEquals(0, manager.getRoundsRemaining());
    }

    @Test
    @DisplayName("Verify spawnMysteryCell with null board returns null")
    void testSpawnMysteryCellWithNullBoardReturnsNull() {
        Cell result = manager.spawnMysteryCell(null, null);
        assertNull(result, "Calling spawnMysteryCell with null board must return null");
    }

    // =========================================================================
    // Category 5: Manual Placement (spawnAt) & Utilities
    // =========================================================================

    @Test
    @DisplayName("Verify spawnAt manually forces placement for deterministic simulation")
    void testSpawnAtManualPlacement() {
        Cell targetCell = board.getStandardCell(33);
        Cell result = manager.spawnAt(targetCell, 6, board, null);

        assertSame(targetCell, result);
        assertSame(targetCell, manager.getActiveMysteryCell());
        assertEquals(6, manager.getRoundsRemaining());
        assertSame(targetCell, board.getMysteryCell());
        assertTrue(targetCell.isMysteryCell());
        assertTrue(manager.isSpawned());
    }

    @Test
    @DisplayName("Verify spawnAt replaces existing active mystery cell and clears previous cell flag")
    void testSpawnAtReplacesExistingMysteryCell() {
        Cell cell10 = board.getStandardCell(10);
        Cell cell20 = board.getStandardCell(20);

        manager.spawnAt(cell10, 4, board, null);
        assertTrue(cell10.isMysteryCell());

        manager.spawnAt(cell20, 3, board, null);
        assertFalse(cell10.isMysteryCell(), "Previous cell must lose mystery cell status");
        assertTrue(cell20.isMysteryCell(), "New cell must have mystery cell status");
        assertSame(cell20, board.getMysteryCell());
        assertSame(cell20, manager.getActiveMysteryCell());
        assertSame(cell20, manager.getPreviousMysteryCell());
    }

    @Test
    @DisplayName("Verify setRoundsCompleted and getRoundsCompleted getter/setter")
    void testSetAndGetRoundsCompleted() {
        manager.setRoundsCompleted(12);
        assertEquals(12, manager.getRoundsCompleted());
    }

    // =========================================================================
    // Category 6: View Notifications
    // =========================================================================

    @Test
    @DisplayName("Verify view is notified when mystery cell spawns")
    void testViewNotificationOnSpawn() {
        AtomicBoolean displayCalled = new AtomicBoolean(false);
        AtomicReference<Cell> notifiedCell = new AtomicReference<>();
        AtomicInteger notifiedRounds = new AtomicInteger();

        GameView mockView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayMysteryCellSpawned".equals(method.getName())) {
                        displayCalled.set(true);
                        notifiedCell.set((Cell) args[0]);
                        notifiedRounds.set((Integer) args[1]);
                    }
                    return null;
                }
        );

        manager.updateOnRoundEnd(board, mockView); // Round 1
        assertFalse(displayCalled.get(), "View should not be called at round 1");

        manager.updateOnRoundEnd(board, mockView); // Round 2
        assertTrue(displayCalled.get(), "View should be notified at round 2");
        assertSame(manager.getActiveMysteryCell(), notifiedCell.get());
        assertEquals(MysteryCellManager.DURATION_ROUNDS, notifiedRounds.get());
    }

    @Test
    @DisplayName("Verify view is notified again when mystery cell relocates")
    void testViewNotificationOnRelocation() {
        List<Cell> spawnedHistory = new ArrayList<>();

        GameView mockView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayMysteryCellSpawned".equals(method.getName())) {
                        spawnedHistory.add((Cell) args[0]);
                    }
                    return null;
                }
        );

        // Advance through Round 1 and Round 2 (1st spawn)
        manager.updateOnRoundEnd(board, mockView);
        manager.updateOnRoundEnd(board, mockView);
        assertEquals(1, spawnedHistory.size());

        // Advance through Rounds 3, 4, 5, 6 (relocation triggers at end of Round 6)
        for (int r = 3; r <= 6; r++) {
            manager.updateOnRoundEnd(board, mockView);
        }

        assertEquals(2, spawnedHistory.size(), "View should be notified again upon relocation");
        assertNotSame(spawnedHistory.get(0), spawnedHistory.get(1),
                "Relocated cell reported to view must be different from initial cell");
    }
}
