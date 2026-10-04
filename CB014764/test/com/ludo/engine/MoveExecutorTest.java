package com.ludo.engine;

import com.ludo.enums.CellType;
import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.enums.TeleportDestination;
import com.ludo.execptions.GameEngineException;
import com.ludo.factory.GameFactory;
import com.ludo.model.*;
import com.ludo.view.GameView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MoveExecutor Unit Tests (Rules 6, T-1, T-4, T-5, T-6, T-8, T-9)")
class MoveExecutorTest {

    private GameState gameState;
    private Board board;
    private Player redPlayer;
    private Player bluePlayer;
    private Piece red1;
    private Piece red2;
    private Piece red3;
    private Piece red4;
    private Piece blue1;
    private Piece blue2;
    private GameView dummyView;
    private TeleportHandler teleportHandler;
    private MoveExecutor executor;

    @BeforeEach
    void setUp() {
        gameState = GameFactory.createGame();
        board = gameState.getBoard();

        redPlayer = gameState.getPlayer(PlayerColor.RED);
        bluePlayer = gameState.getPlayer(PlayerColor.BLUE);

        red1 = redPlayer.getPiece(1);
        red2 = redPlayer.getPiece(2);
        red3 = redPlayer.getPiece(3);
        red4 = redPlayer.getPiece(4);

        blue1 = bluePlayer.getPiece(1);
        blue2 = bluePlayer.getPiece(2);

        dummyView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> null
        );

        teleportHandler = new TeleportHandler(gameState, dummyView);
        executor = new MoveExecutor(gameState, dummyView, teleportHandler);
    }

    // =========================================================================
    // Category 1: Construction & Defensive Input Validation
    // =========================================================================

    @Test
    @DisplayName("Verify constructor rejects null GameState")
    void testConstructorRejectsNullGameState() {
        assertThrows(IllegalArgumentException.class, () -> new MoveExecutor(null, dummyView, teleportHandler),
                "Should throw IllegalArgumentException when GameState is null");
    }

    @Test
    @DisplayName("Verify constructor rejects null GameView")
    void testConstructorRejectsNullGameView() {
        assertThrows(IllegalArgumentException.class, () -> new MoveExecutor(gameState, null, teleportHandler),
                "Should throw IllegalArgumentException when GameView is null");
    }

    @Test
    @DisplayName("Verify constructor gracefully handles null TeleportHandler with fallback")
    void testConstructorDefaultTeleportHandler() {
        MoveExecutor fallbackExecutor = new MoveExecutor(gameState, dummyView, null);
        assertNotNull(fallbackExecutor);
    }

    @Test
    @DisplayName("Verify executeMove safely returns false when passed null Move")
    void testExecuteMoveNullMoveReturnsFalse() {
        assertFalse(executor.executeMove(redPlayer, null));
    }

    @Test
    @DisplayName("Verify executeMove throws IllegalArgumentException when passed null Player")
    void testExecuteMoveNullPlayerThrowsException() {
        Move move = Move.createNormalMove(red1, board.getStandardCell(10), board.getStandardCell(14), 4, false, null, false);
        assertThrows(IllegalArgumentException.class, () -> executor.executeMove(null, move));
    }

    @Test
    @DisplayName("Verify deployPieceFromBaseToX rejects null Player or Piece")
    void testDeployPieceRejectsNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> executor.deployPieceFromBaseToX(null, red1));
        assertThrows(IllegalArgumentException.class, () -> executor.deployPieceFromBaseToX(redPlayer, null));
    }

    @Test
    @DisplayName("Verify breakBlockadeOnThreeSixes rejects null Player")
    void testBreakBlockadeRejectsNullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> executor.breakBlockadeOnThreeSixes(null));
    }

    // =========================================================================
    // Category 2: Base-to-Start Deployment & Coin Toss (Rule T-1)
    // =========================================================================

    @Test
    @DisplayName("Verify deployPieceFromBaseToX sets piece to start cell and clears base")
    void testDeployPieceFromBaseToXSetsCellAndClearsBase() {
        Cell startCell = board.getStartingCell(PlayerColor.RED);
        Cell baseCell = board.getBaseCell(PlayerColor.RED);

        assertTrue(red1.isInBase());
        assertTrue(baseCell.getOccupyingPieces().contains(red1));

        boolean wasCapture = executor.deployPieceFromBaseToX(redPlayer, red1);

        assertFalse(wasCapture);
        assertFalse(red1.isInBase());
        assertSame(startCell, red1.getCurrentCell());
        assertTrue(startCell.getOccupyingPieces().contains(red1));
        assertFalse(baseCell.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify deployPieceFromBaseToX assigns coin toss direction (Rule T-1)")
    void testDeployPieceFromBaseToXAssignsCoinTossDirection() {
        executor.deployPieceFromBaseToX(redPlayer, red1);

        assertNotNull(red1.getDirection(), "Piece must have a coin toss direction assigned");
        assertNotNull(red1.getOriginalDirection(), "Piece must have originalDirection assigned");
        assertEquals(red1.getDirection(), red1.getOriginalDirection());
        assertTrue(red1.getDirection() == Direction.CLOCKWISE || red1.getDirection() == Direction.COUNTER_CLOCKWISE);
    }

    @Test
    @DisplayName("Verify deployPieceFromBaseToX captures lone opponent piece resting on starting 'X'")
    void testDeployPieceFromBaseToXCapturesLoneOpponentOnStart() {
        Cell redStart = board.getStartingCell(PlayerColor.RED);
        blue1.setInBase(false);
        blue1.setCurrentCell(redStart);
        redStart.addPiece(blue1);

        boolean wasCapture = executor.deployPieceFromBaseToX(redPlayer, red1);

        assertTrue(wasCapture, "Deploying onto lone opponent must trigger capture");
        assertTrue(blue1.isInBase(), "Captured opponent piece must be sent back to base");
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue1.getCurrentCell());
        assertEquals(1, red1.getCaptureCount());
        assertSame(redStart, red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify deployPieceFromBaseToX forms a block when friendly piece already on starting 'X'")
    void testDeployPieceFromBaseToXFormsBlockWithFriendlyPiece() {
        Cell redStart = board.getStartingCell(PlayerColor.RED);
        red2.setInBase(false);
        red2.setCurrentCell(redStart);
        redStart.addPiece(red2);

        boolean wasCapture = executor.deployPieceFromBaseToX(redPlayer, red1);

        assertFalse(wasCapture);
        assertTrue(redStart.isBlock(), "Starting cell must form a block with friendly piece");
        assertEquals(2, redStart.getPieceCount());
    }

    // =========================================================================
    // Category 3: Standard Track Movement & Combat (Rule 6, Rule T-9)
    // =========================================================================

    @Test
    @DisplayName("Verify executeMove advances piece normally to destination")
    void testExecuteMoveNormalAdvancement() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        Move move = Move.createNormalMove(red1, c10, c14, 4, false, null, false);
        boolean wasCapture = executor.executeMove(redPlayer, move);

        assertFalse(wasCapture);
        assertSame(c14, red1.getCurrentCell());
        assertTrue(c14.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify executeMove captures opponent piece and resets it to base (Rule 6 & Rule T-9)")
    void testExecuteMoveCaptureResetsOpponentToBase() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        blue1.setInBase(false);
        blue1.setCurrentCell(c14);
        c14.addPiece(blue1);

        Move move = Move.createNormalMove(red1, c10, c14, 4, true, blue1, false);
        boolean wasCapture = executor.executeMove(redPlayer, move);

        assertTrue(wasCapture);
        assertTrue(blue1.isInBase(), "Captured blue piece must be reset to base");
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue1.getCurrentCell());
        assertEquals(1, red1.getCaptureCount());
        assertSame(c14, red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify executeMove creates a block when landing on friendly piece")
    void testExecuteMoveCreatesBlockWithFriendlyPiece() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        red2.setInBase(false);
        red2.setCurrentCell(c14);
        c14.addPiece(red2);

        Move move = Move.createNormalMove(red1, c10, c14, 4, false, null, true);
        executor.executeMove(redPlayer, move);

        assertTrue(c14.isBlock(), "Destination cell must form a block");
        assertTrue(c14.getOccupyingPieces().contains(red1));
        assertTrue(c14.getOccupyingPieces().contains(red2));
    }

    @Test
    @DisplayName("Verify piece breaking away from a block reverts both pieces to originalDirection (Rule T-5)")
    void testExecuteMoveBreaksBlockRestoresOriginalDirection() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);

        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        red1.setOriginalDirection(Direction.COUNTER_CLOCKWISE);
        red1.setDirection(Direction.CLOCKWISE); // Modified while participating in block
        red2.setOriginalDirection(Direction.CLOCKWISE);
        red2.setDirection(Direction.COUNTER_CLOCKWISE);

        // createNormalMove automatically sets breaksBlock = true because c10.isBlock() is true
        Move move = Move.createNormalMove(red1, c10, c14, 4, false, null, false);
        assertTrue(move.breaksBlock());

        executor.executeMove(redPlayer, move);

        assertEquals(Direction.COUNTER_CLOCKWISE, red1.getDirection(),
                "Moving piece must revert to its original direction");
        assertEquals(Direction.CLOCKWISE, red2.getDirection(),
                "Piece left behind in broken block must also revert to its original direction");
    }

    // =========================================================================
    // Category 4: Blockade Group Movement & Combat (Rules T-4, T-8)
    // =========================================================================

    @Test
    @DisplayName("Verify 2-piece blockade moves together to destination (Rule T-4)")
    void testExecuteMoveBlockMovement() {
        Cell c10 = board.getStandardCell(10);
        Cell c12 = board.getStandardCell(12);

        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        Move move = Move.createBlockMove(List.of(red1, red2), c10, c12, 4, 2, Direction.CLOCKWISE);
        boolean wasCapture = executor.executeMove(redPlayer, move);

        assertFalse(wasCapture);
        assertSame(c12, red1.getCurrentCell());
        assertSame(c12, red2.getCurrentCell());
        assertTrue(c12.isBlock());
        assertFalse(c10.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red2));
    }

    @Test
    @DisplayName("Verify equal-sized blockade captures opponent blockade (Rule T-8)")
    void testExecuteMoveBlockCapturesOpponentBlock() {
        Cell c10 = board.getStandardCell(10);
        Cell c12 = board.getStandardCell(12);

        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        blue1.setInBase(false);
        blue2.setInBase(false);
        blue1.setCurrentCell(c12);
        blue2.setCurrentCell(c12);
        c12.addPiece(blue1);
        c12.addPiece(blue2);

        Move move = Move.createBlockMove(List.of(red1, red2), c10, c12, 4, 2, Direction.CLOCKWISE,
                true, List.of(blue1, blue2));
        boolean wasCapture = executor.executeMove(redPlayer, move);

        assertTrue(wasCapture);
        assertTrue(blue1.isInBase(), "Captured blue 1 must be reset to base");
        assertTrue(blue2.isInBase(), "Captured blue 2 must be reset to base");
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue1.getCurrentCell());
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue2.getCurrentCell());

        assertEquals(1, red1.getCaptureCount());
        assertEquals(1, red2.getCaptureCount());
        assertEquals(1, red1.getBlockCaptureCount());
        assertEquals(1, red2.getBlockCaptureCount());

        assertSame(c12, red1.getCurrentCell());
        assertSame(c12, red2.getCurrentCell());
    }

    @Test
    @DisplayName("Verify block landing on lone opponent shares the cell peacefully without capture (Rule T-8)")
    void testExecuteMoveBlockSharesCellWithSingleOpponent() {
        Cell c10 = board.getStandardCell(10);
        Cell c12 = board.getStandardCell(12);

        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        blue1.setInBase(false);
        blue1.setCurrentCell(c12);
        c12.addPiece(blue1); // Lone opponent piece

        Move move = Move.createBlockMove(List.of(red1, red2), c10, c12, 4, 2, Direction.CLOCKWISE,
                false, Collections.emptyList());
        boolean wasCapture = executor.executeMove(redPlayer, move);

        assertFalse(wasCapture, "Blocks do NOT capture lone pieces on the track (Rule T-8)");
        assertFalse(blue1.isInBase(), "Blue piece must NOT be sent to base");
        assertSame(c12, blue1.getCurrentCell(), "Blue piece remains peacefully on cell 12");
        assertSame(c12, red1.getCurrentCell());
        assertSame(c12, red2.getCurrentCell());
        assertTrue(c12.getOccupyingPieces().contains(blue1));
        assertTrue(c12.getOccupyingPieces().contains(red1));
        assertTrue(c12.getOccupyingPieces().contains(red2));
    }

    // =========================================================================
    // Category 5: Blockade Breaking on Three Sixes (Rule T-6)
    // =========================================================================

    @Test
    @DisplayName("Verify breakBlockadeOnThreeSixes leaves one piece behind and advances the rest 6 cells (Rule T-6)")
    void testBreakBlockadeOnThreeSixesLeavesOneAndMovesRest() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        red1.setDirection(Direction.CLOCKWISE);
        red1.setOriginalDirection(Direction.CLOCKWISE);
        red2.setDirection(Direction.CLOCKWISE);
        red2.setOriginalDirection(Direction.CLOCKWISE);
        c10.addPiece(red1);
        c10.addPiece(red2);
        assertTrue(c10.isBlock());

        executor.breakBlockadeOnThreeSixes(redPlayer);

        assertFalse(c10.isBlock(), "Blockade on cell 10 must be broken");
        assertSame(c10, red1.getCurrentCell(), "Piece index 0 remains on original cell");
        assertSame(board.getStandardCell(16), red2.getCurrentCell(),
                "Other piece must advance 6 cells forward in original direction");
    }

    @Test
    @DisplayName("Verify breakBlockadeOnThreeSixes is safe no-op when player has no blockades")
    void testBreakBlockadeWithNoBlockadesIsSafeNoOp() {
        assertDoesNotThrow(() -> executor.breakBlockadeOnThreeSixes(redPlayer));
    }

    @Test
    @DisplayName("Verify broken block piece stops before opponent block when obstructed (Rule T-3)")
    void testBreakBlockadeObstructedByOpponentBlock() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);

        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        red1.setDirection(Direction.CLOCKWISE);
        red1.setOriginalDirection(Direction.CLOCKWISE);
        red2.setDirection(Direction.CLOCKWISE);
        red2.setOriginalDirection(Direction.CLOCKWISE);
        c10.addPiece(red1);
        c10.addPiece(red2);

        // Blue blockade at cell 14
        blue1.setInBase(false);
        blue2.setInBase(false);
        blue1.setCurrentCell(c14);
        blue2.setCurrentCell(c14);
        c14.addPiece(blue1);
        c14.addPiece(blue2);
        assertTrue(c14.isBlock());

        executor.breakBlockadeOnThreeSixes(redPlayer);

        // Red 2 attempts 6 steps from 10 to 16, but is obstructed at 14 -> must stop at 13
        assertSame(board.getStandardCell(13), red2.getCurrentCell(),
                "Moving piece must stop at cell 13 immediately before obstacle block");
    }

    // =========================================================================
    // Category 6: Blocked Moves & Obstruction Handling (Rule T-3)
    // =========================================================================

    @Test
    @DisplayName("Verify executeMove advances piece to cell before block on blocked move")
    void testExecuteBlockedMoveAdvancesToCellBeforeBlock() {
        Cell c10 = board.getStandardCell(10);
        Cell c12 = board.getStandardCell(12); // Stopped cell before obstacle
        Cell c14 = board.getStandardCell(14); // Intended target

        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        Move move = Move.createBlockedMove(red1, c10, c12, c14, blue1, 4, false, null, false);
        boolean wasCapture = executor.executeMove(redPlayer, move);

        assertFalse(wasCapture);
        assertSame(c12, red1.getCurrentCell(), "Piece must stop at cell before block");
        assertTrue(c12.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify piece trapped directly adjacent to obstacle cannot move forward")
    void testExecuteBlockedMoveTrappedAdjacentCannotMove() {
        Cell c11 = board.getStandardCell(11);
        Cell c12 = board.getStandardCell(12); // Obstacle cell

        red1.setInBase(false);
        red1.setCurrentCell(c11);
        c11.addPiece(red1);

        // Trapped adjacent move has toCell == fromCell
        Move move = Move.createBlockedMove(red1, c11, c11, c12, blue1, 3, false, null, false);
        boolean wasCapture = executor.executeMove(redPlayer, move);

        assertFalse(wasCapture);
        assertSame(c11, red1.getCurrentCell(), "Piece must remain in place on c11");
    }

    // =========================================================================
    // Category 7: Mystery Cell Trigger Delegation (Rule T-11)
    // =========================================================================

    @Test
    @DisplayName("Verify executeMove landing on Mystery Cell triggers handleMysteryCellLanding")
    void testExecuteMoveToMysteryCellDelegatesTeleport() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);
        c14.setMysteryCell(true);

        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        AtomicBoolean teleportCalled = new AtomicBoolean(false);
        TeleportHandler mockTeleport = new TeleportHandler(gameState, dummyView) {
            @Override
            public TeleportDestination handleMysteryCellLanding(Player player, Piece piece, TeleportDestination destination) {
                teleportCalled.set(true);
                return TeleportDestination.BASE;
            }
        };

        MoveExecutor executorWithMock = new MoveExecutor(gameState, dummyView, mockTeleport);
        Move move = Move.createNormalMove(red1, c10, c14, 4, false, null, false);
        executorWithMock.executeMove(redPlayer, move);

        assertTrue(teleportCalled.get(), "Landing on Mystery Cell must trigger TeleportHandler");
    }

    @Test
    @DisplayName("Verify executeMove for block landing on Mystery Cell triggers handleMysteryCellBlockLanding")
    void testExecuteBlockMoveToMysteryCellDelegatesTeleport() {
        Cell c10 = board.getStandardCell(10);
        Cell c12 = board.getStandardCell(12);
        c12.setMysteryCell(true);

        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        AtomicBoolean blockTeleportCalled = new AtomicBoolean(false);
        TeleportHandler mockTeleport = new TeleportHandler(gameState, dummyView) {
            @Override
            public TeleportDestination handleMysteryCellBlockLanding(Player player, List<Piece> blockPieces, TeleportDestination destination) {
                blockTeleportCalled.set(true);
                return TeleportDestination.BASE;
            }
        };

        MoveExecutor executorWithMock = new MoveExecutor(gameState, dummyView, mockTeleport);
        Move move = Move.createBlockMove(List.of(red1, red2), c10, c12, 4, 2, Direction.CLOCKWISE);
        executorWithMock.executeMove(redPlayer, move);

        assertTrue(blockTeleportCalled.get(), "Block landing on Mystery Cell must trigger block teleportation");
    }

    // =========================================================================
    // Category 8: Victory Declaration & Rollback Resilience (Rule 7)
    // =========================================================================

    @Test
    @DisplayName("Verify 4th piece reaching Home marks victory and declares winner (Rule 7)")
    void testExecuteMoveReachingHomeMarksPieceAndDeclaresWinner() {
        Cell homeCell = board.getHomeCell(PlayerColor.RED);
        Cell lastHomePath = board.getHomeStraightCell(PlayerColor.RED, 4);

        // Pieces 1, 2, 3 already finished
        red1.reachHome();
        red2.reachHome();
        red3.reachHome();

        // Piece 4 enters Home
        red4.setInBase(false);
        red4.setCurrentCell(lastHomePath);
        lastHomePath.addPiece(red4);

        Move finalMove = Move.createNormalMove(red4, lastHomePath, homeCell, 1, false, null, false);
        executor.executeMove(redPlayer, finalMove);

        assertTrue(redPlayer.hasWon(), "Red player should have won");
        assertTrue(gameState.isGameOver(), "Game should transition to game over");
        assertSame(redPlayer, gameState.getWinner(), "Winner must be set to Red player");
    }

    @Test
    @DisplayName("Verify restorePiece safely relocates piece to target cell and cleans old cell")
    void testRestorePiecePutsPieceOnTargetCell() {
        Cell c10 = board.getStandardCell(10);
        Cell c20 = board.getStandardCell(20);

        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        executor.restorePiece(red1, c20);

        assertSame(c20, red1.getCurrentCell());
        assertTrue(c20.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify rollbackPieces safely restores piece to origin cell on midway failure")
    void testRollbackPiecesRestoresPieceOnMidwayFailure() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);

        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        // Create a faulty opponent piece with null color that causes GameEngineException midway during capture
        Piece faultyOpponent = new Piece(PlayerColor.BLUE, 1) {
            @Override
            public PlayerColor getColor() {
                return null;
            }
        };

        Move badMove = Move.createNormalMove(red1, c10, c14, 4, true, faultyOpponent, false);

        assertThrows(GameEngineException.class, () -> executor.executeMove(redPlayer, badMove));

        // Verify piece was safely rolled back to origin cell c10
        assertSame(c10, red1.getCurrentCell(), "Piece must be rolled back to c10");
        assertTrue(c10.getOccupyingPieces().contains(red1));
        assertFalse(c14.getOccupyingPieces().contains(red1));
    }
}
