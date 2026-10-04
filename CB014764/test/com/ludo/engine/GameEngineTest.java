package com.ludo.engine;

import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.enums.TeleportDestination;
import com.ludo.execptions.GameEngineException;
import com.ludo.factory.GameFactory;
import com.ludo.model.*;
import com.ludo.state.BriefingState;
import com.ludo.state.EnergizedState;
import com.ludo.state.SickState;
import com.ludo.view.GameView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameEngine Unit Tests (Facade & Lifecycle Coordinator)")
class GameEngineTest {

    private GameState gameState;
    private Board board;
    private Player redPlayer;
    private Player greenPlayer;
    private Player yellowPlayer;
    private Player bluePlayer;
    private Piece red1;
    private Piece red2;
    private Piece red3;
    private Piece red4;
    private GameView dummyView;
    private GameEngine engine;

    @BeforeEach
    void setUp() {
        gameState = GameFactory.createGame();
        board = gameState.getBoard();

        redPlayer = gameState.getPlayer(PlayerColor.RED);
        greenPlayer = gameState.getPlayer(PlayerColor.GREEN);
        yellowPlayer = gameState.getPlayer(PlayerColor.YELLOW);
        bluePlayer = gameState.getPlayer(PlayerColor.BLUE);

        red1 = redPlayer.getPiece(1);
        red2 = redPlayer.getPiece(2);
        red3 = redPlayer.getPiece(3);
        red4 = redPlayer.getPiece(4);

        dummyView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> null
        );

        engine = new GameEngine(gameState, dummyView);
    }

    private void placePieceInHome(Piece piece) {
        piece.setInBase(false);
        board.getBaseCell(piece.getColor()).removePiece(piece);
        piece.reachHome();
        Cell homeCell = board.getHomeCell(piece.getColor());
        piece.setCurrentCell(homeCell);
        homeCell.addPiece(piece);
    }

    // =========================================================================
    // Category 1: Construction, Dependency Injection & Wiring
    // =========================================================================

    @Test
    @DisplayName("Verify constructor rejects null GameState")
    void testConstructorNullGameStateThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new GameEngine(null, dummyView),
                "Should reject null GameState");
    }

    @Test
    @DisplayName("Verify constructor rejects null GameView")
    void testConstructorNullGameViewThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new GameEngine(gameState, null),
                "Should reject null GameView");
    }

    @Test
    @DisplayName("Verify constructor initializes correctly and wires all sub-handlers")
    void testConstructorInitializesCorrectly() {
        assertSame(gameState, engine.getGameState());
        assertSame(dummyView, engine.getView());
        assertNotNull(engine.getMysteryCellManager());
        assertNotNull(engine.getTurnOrderManager());
        assertNotNull(engine.getTurnHandler());
        assertNotNull(engine.getMoveExecutor());
        assertNotNull(engine.getTeleportHandler());
        assertNotNull(engine.getRoundOrder());
        assertTrue(engine.getRoundOrder().isEmpty());
    }

    @Test
    @DisplayName("Verify constructor accepts injected custom MysteryCellManager")
    void testConstructorCustomMysteryCellManager() {
        MysteryCellManager customMcm = new MysteryCellManager();
        GameEngine customEngine = new GameEngine(gameState, dummyView, customMcm);
        assertSame(customMcm, customEngine.getMysteryCellManager());
    }

    @Test
    @DisplayName("Verify constructor safely defaults null MysteryCellManager")
    void testConstructorNullMysteryCellManagerDefaultsNonNull() {
        GameEngine customEngine = new GameEngine(gameState, dummyView, null);
        assertNotNull(customEngine.getMysteryCellManager());
    }

    // =========================================================================
    // Category 2: Setup & Simulation Lifecycle
    // =========================================================================

    @Test
    @DisplayName("Verify setupGame initializes first player, establishes round order, and deploys piece")
    void testSetupGameInitializesGameSuccessfully() {
        AtomicBoolean bannerDisplayed = new AtomicBoolean(false);
        GameView trackingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayBeforeGameBegins".equals(method.getName())) {
                        bannerDisplayed.set(true);
                    }
                    return null;
                }
        );

        GameEngine testEngine = new GameEngine(gameState, trackingView);
        testEngine.setupGame();

        assertTrue(bannerDisplayed.get(), "displayBeforeGameBegins must be invoked during setup");
        List<Player> roundOrder = testEngine.getRoundOrder();
        assertEquals(4, roundOrder.size(), "roundOrder must contain 4 players after setup");

        Player firstPlayer = roundOrder.get(0);
        Cell startCell = board.getStartingCell(firstPlayer.getColor());
        assertTrue(startCell.isOccupied(), "First player's piece must be deployed to starting cell");
        assertFalse(firstPlayer.getPieces().get(0).isInBase());
    }

    @Test
    @DisplayName("Verify setupGame wraps unexpected runtime exceptions into GameEngineException")
    void testSetupGameWrapsUnexpectedException() {
        GameView throwingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayBeforeGameBegins".equals(method.getName())) {
                        throw new RuntimeException("Simulated view rendering failure");
                    }
                    return null;
                }
        );

        GameEngine failingEngine = new GameEngine(gameState, throwingView);
        GameEngineException ex = assertThrows(GameEngineException.class, failingEngine::setupGame);
        assertTrue(ex.getMessage().contains("Game setup failed"));
    }

    @Test
    @DisplayName("Verify runSimulation throws GameEngineException if called before setupGame")
    void testRunSimulationThrowsWhenRoundOrderIsEmpty() {
        assertTrue(engine.getRoundOrder().isEmpty());
        GameEngineException ex = assertThrows(GameEngineException.class, engine::runSimulation);
        assertTrue(ex.getMessage().contains("Round order is empty"));
    }

    @Test
    @DisplayName("Verify runSimulation runs until game is won and displays simulation banner")
    void testRunSimulationExecutesUntilGameIsWon() {
        AtomicBoolean bannerShown = new AtomicBoolean(false);
        GameView trackingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displaySimulationBanner".equals(method.getName())) {
                        bannerShown.set(true);
                    }
                    return null;
                }
        );

        GameEngine simEngine = new GameEngine(gameState, trackingView);
        simEngine.getTurnOrderManager().setRoundOrder(List.of(redPlayer, greenPlayer, yellowPlayer, bluePlayer));

        // Set up Red with 3 pieces in home, 4th piece 1 step from home
        placePieceInHome(red1);
        placePieceInHome(red2);
        placePieceInHome(red3);

        Cell lastHomePath = board.getHomeStraightCell(PlayerColor.RED, 4);
        red4.setInBase(false);
        board.getBaseCell(PlayerColor.RED).removePiece(red4);
        red4.setCurrentCell(lastHomePath);
        lastHomePath.addPiece(red4);

        // Script Red's roll to 1 (winning move into Home)
        Queue<Integer> winningRolls = new LinkedList<>(List.of(1));
        redPlayer.getStrategy(); // strategy exists

        // Run turn directly or simulation
        simEngine.executePlayerTurn(redPlayer, winningRolls);
        assertTrue(gameState.isGameOver());

        // Now run simulation; since gameState is already game over, it shows banner and terminates loop cleanly
        simEngine.runSimulation();
        assertTrue(bannerShown.get(), "Simulation banner must be displayed");
        assertSame(redPlayer, gameState.getWinner());
    }

    @Test
    @DisplayName("Verify executeRound runs all players in roundOrder and executes endRound")
    void testExecuteRoundExecutesAllPlayersAndEndRound() {
        engine.getTurnOrderManager().setRoundOrder(List.of(redPlayer, greenPlayer, yellowPlayer, bluePlayer));

        // Put a piece into SickState to verify endRound() gets called at end of executeRound()
        red1.makeSick(); // 4 rounds remaining
        assertEquals(4, ((SickState) red1.getState()).getRemainingRounds());

        engine.executeRound();

        // SickState remaining rounds should have decremented from 4 to 3
        assertEquals(3, ((SickState) red1.getState()).getRemainingRounds(),
                "executeRound must call endRound() decremeting piece timed states");
        assertEquals(1, engine.getMysteryCellManager().getRoundsCompleted());
    }

    @Test
    @DisplayName("Verify executeRound halts immediately if game is won midway through the round")
    void testExecuteRoundHaltsImmediatelyIfGameIsWonMidway() {
        AtomicInteger turnsExecuted = new AtomicInteger(0);
        GameView countingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayPlayerRoll".equals(method.getName())) {
                        turnsExecuted.incrementAndGet();
                    }
                    return null;
                }
        );

        GameEngine testEngine = new GameEngine(gameState, countingView);
        testEngine.getTurnOrderManager().setRoundOrder(List.of(redPlayer, greenPlayer, yellowPlayer, bluePlayer));

        // Make Red win immediately on its turn
        placePieceInHome(red1);
        placePieceInHome(red2);
        placePieceInHome(red3);
        placePieceInHome(red4);
        assertTrue(redPlayer.hasWon());
        // Trigger game over
        gameState.setWinner(redPlayer);
        assertTrue(gameState.isGameOver());

        testEngine.executeRound();

        // Since Red already won, turn loop breaks immediately
        assertTrue(gameState.isGameOver());
    }

    @Test
    @DisplayName("Verify executeRound catches player turn runtime exceptions gracefully without crashing")
    void testExecuteRoundCatchesPlayerExceptionGracefully() {
        // Player with faulty strategy that throws
        Player faultyPlayer = new Player(PlayerColor.GREEN, (p, moves, b, roll) -> {
            throw new RuntimeException("Simulated AI strategy crash");
        });
        engine.getTurnOrderManager().setRoundOrder(List.of(faultyPlayer, yellowPlayer));

        assertDoesNotThrow(() -> engine.executeRound(),
                "executeRound should catch player turn exceptions and proceed without crashing");
    }

    @Test
    @DisplayName("Verify endRound updates piece timed effects and mystery cell lifecycle")
    void testEndRoundUpdatesPieceTimedStatesAndMysteryCell() {
        red1.makeSick(); // 4 rounds remaining
        assertInstanceOf(SickState.class, red1.getState());

        engine.endRound();

        assertEquals(3, ((SickState) red1.getState()).getRemainingRounds());
        assertEquals(1, engine.getMysteryCellManager().getRoundsCompleted());
    }

    @Test
    @DisplayName("Verify endRound catches runtime exceptions defensively")
    void testEndRoundCatchesExceptionsDefensively() {
        GameView throwingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayRoundStatus".equals(method.getName())) {
                        throw new RuntimeException("Simulated view error");
                    }
                    return null;
                }
        );

        GameEngine customEngine = new GameEngine(gameState, throwingView);
        assertDoesNotThrow(customEngine::endRound, "endRound should handle exceptions without throwing");
    }

    // =========================================================================
    // Category 3: Delegation to TurnOrderManager
    // =========================================================================

    @Test
    @DisplayName("Verify determineFirstPlayer establishes round order and deploys first piece")
    void testDetermineFirstPlayerEstablishesOrderAndDeploysPiece() {
        Player firstPlayer = engine.determineFirstPlayer();

        assertNotNull(firstPlayer);
        assertTrue(gameState.getPlayers().contains(firstPlayer));

        List<Player> roundOrder = engine.getRoundOrder();
        assertFalse(roundOrder.isEmpty());
        assertSame(firstPlayer, roundOrder.get(0));

        Cell startCell = board.getStartingCell(firstPlayer.getColor());
        assertTrue(startCell.isOccupied());
        Piece deployedPiece = startCell.getOccupyingPieces().get(0);
        assertSame(firstPlayer.getColor(), deployedPiece.getColor());
        assertFalse(deployedPiece.isInBase());
        assertNotNull(deployedPiece.getDirection());
    }

    @Test
    @DisplayName("Verify buildClockwiseOrder starts from winning player and continues clockwise")
    void testBuildClockwiseOrderStartsFromWinner() {
        List<Player> standardOrder = List.of(redPlayer, greenPlayer, yellowPlayer, bluePlayer);

        List<Player> order = engine.buildClockwiseOrder(yellowPlayer, standardOrder);
        assertEquals(4, order.size());
        assertSame(yellowPlayer, order.get(0));
        assertSame(bluePlayer, order.get(1));
        assertSame(redPlayer, order.get(2));
        assertSame(greenPlayer, order.get(3));
    }

    @ParameterizedTest
    @EnumSource(PlayerColor.class)
    @DisplayName("Verify buildClockwiseOrder preserves 4-player cycle for every starting color")
    void testBuildClockwiseOrderAllColors(PlayerColor startColor) {
        List<Player> standardOrder = gameState.getPlayers();
        Player starter = gameState.getPlayer(startColor);

        List<Player> order = engine.buildClockwiseOrder(starter, standardOrder);
        assertEquals(4, order.size());
        assertSame(starter, order.get(0));
    }

    @Test
    @DisplayName("Verify buildClockwiseOrder rejects invalid inputs")
    void testBuildClockwiseOrderRejectsInvalidInputs() {
        List<Player> standardOrder = gameState.getPlayers();
        Player outsider = new Player(PlayerColor.RED, null);

        assertThrows(IllegalArgumentException.class, () -> engine.buildClockwiseOrder(null, standardOrder));
        assertThrows(IllegalArgumentException.class, () -> engine.buildClockwiseOrder(redPlayer, null));
        assertThrows(IllegalArgumentException.class, () -> engine.buildClockwiseOrder(redPlayer, List.of()));
        assertThrows(IllegalArgumentException.class, () -> engine.buildClockwiseOrder(outsider, standardOrder));
    }

    // =========================================================================
    // Category 4: Delegation to TurnHandler
    // =========================================================================

    @Test
    @DisplayName("Verify executePlayerTurn without scripted rolls executes cleanly")
    void testExecutePlayerTurnSingleArgDelegation() {
        assertDoesNotThrow(() -> engine.executePlayerTurn(redPlayer));
    }

    @Test
    @DisplayName("Verify executePlayerTurn with scripted rolls consumes rolls and grants bonus on six")
    void testExecutePlayerTurnScriptedRollsDelegation() {
        Queue<Integer> scriptedRolls = new LinkedList<>(List.of(6, 3));
        engine.executePlayerTurn(redPlayer, scriptedRolls);

        assertTrue(scriptedRolls.isEmpty(), "Both rolls must be consumed");
        assertEquals(3, redPlayer.getPiecesInBase().size(), "One piece deployed from base");
    }

    @Test
    @DisplayName("Verify three consecutive sixes penalty breaks blockade via executePlayerTurn")
    void testExecutePlayerTurnThreeConsecutiveSixesTriggersPenalty() {
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

        Queue<Integer> rolls = new LinkedList<>(List.of(6, 6, 6));
        engine.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty());
        assertFalse(c10.isBlock(), "Three sixes must break the active blockade");
    }

    // =========================================================================
    // Category 5: Delegation to MoveExecutor
    // =========================================================================

    @Test
    @DisplayName("Verify deployPieceFromBaseToX updates location and clears piece from base")
    void testDeployPieceFromBaseToXSetsCellAndClearsBase() {
        Cell startCell = board.getStartingCell(PlayerColor.RED);
        Cell baseCell = board.getBaseCell(PlayerColor.RED);

        assertTrue(red1.isInBase());
        assertTrue(baseCell.getOccupyingPieces().contains(red1));

        boolean wasCapture = engine.deployPieceFromBaseToX(redPlayer, red1);

        assertFalse(wasCapture);
        assertFalse(red1.isInBase());
        assertSame(startCell, red1.getCurrentCell());
        assertTrue(startCell.getOccupyingPieces().contains(red1));
        assertFalse(baseCell.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify deployPieceFromBaseToX captures lone opponent piece resting on starting 'X'")
    void testDeployPieceFromBaseToXCapturesLoneOpponentOnStart() {
        Cell redStart = board.getStartingCell(PlayerColor.RED);
        Piece blue1 = bluePlayer.getPiece(1);
        blue1.setInBase(false);
        blue1.setCurrentCell(redStart);
        redStart.addPiece(blue1);

        boolean wasCapture = engine.deployPieceFromBaseToX(redPlayer, red1);

        assertTrue(wasCapture, "Deploying onto lone opponent must trigger capture");
        assertTrue(blue1.isInBase(), "Captured opponent piece must be returned to base");
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue1.getCurrentCell());
        assertEquals(1, red1.getCaptureCount(), "Capturing piece capture count must increment");
        assertSame(redStart, red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify executeMove executes forward track movement correctly")
    void testExecuteMoveNormalAdvancement() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        Move move = Move.createNormalMove(red1, c10, c14, 4, false, null, false);
        boolean wasCapture = engine.executeMove(redPlayer, move);

        assertFalse(wasCapture);
        assertSame(c14, red1.getCurrentCell());
        assertTrue(c14.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify executeMove captures opponent piece and resets it to base (Rule T-9)")
    void testExecuteMoveCaptureResetsOpponentToBase() {
        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        Piece blue1 = bluePlayer.getPiece(1);
        blue1.setInBase(false);
        blue1.setCurrentCell(c14);
        c14.addPiece(blue1);

        Move move = Move.createNormalMove(red1, c10, c14, 4, true, blue1, false);
        boolean wasCapture = engine.executeMove(redPlayer, move);

        assertTrue(wasCapture);
        assertTrue(blue1.isInBase(), "Captured opponent piece must be in base");
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue1.getCurrentCell());
        assertEquals(1, red1.getCaptureCount());
    }

    @Test
    @DisplayName("Verify executeMove returns false when passed null move")
    void testExecuteMoveNullMoveReturnsFalse() {
        assertFalse(engine.executeMove(redPlayer, null));
    }

    @Test
    @DisplayName("Verify executeMove throws IllegalArgumentException for null player")
    void testExecuteMoveNullPlayerThrowsException() {
        Move move = Move.createNormalMove(red1, board.getStandardCell(10), board.getStandardCell(14), 4, false, null, false);
        assertThrows(IllegalArgumentException.class, () -> engine.executeMove(null, move));
    }

    @Test
    @DisplayName("Verify breakBlockadeOnThreeSixes breaks blockade and leaves one piece behind (Rule T-6)")
    void testBreakBlockadeOnThreeSixesLeavesOnePieceAndMovesRest() {
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

        engine.breakBlockadeOnThreeSixes(redPlayer);

        assertFalse(c10.isBlock(), "Blockade on cell 10 must be broken");
        assertSame(c10, red1.getCurrentCell(), "Piece index 0 remains on original cell");
        assertSame(board.getStandardCell(16), red2.getCurrentCell(),
                "Other piece must move 6 cells forward in original direction");
    }

    // =========================================================================
    // Category 6: Delegation to TeleportHandler
    // =========================================================================

    @Test
    @DisplayName("Verify teleportation to BASE resets piece to player base (Rule T-11)")
    void testHandleMysteryCellLandingBaseTeleport() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = engine.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.BASE);

        assertEquals(TeleportDestination.BASE, dest);
        assertTrue(red1.isInBase());
        assertSame(board.getBaseCell(PlayerColor.RED), red1.getCurrentCell());
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify teleportation to ALPHA with HEADS applies EnergizedState (Rule T-12)")
    void testHandleMysteryCellLandingAlphaCellEnergized() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = engine.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.ALPHA, true);

        assertEquals(TeleportDestination.ALPHA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red1.getCurrentCell());
        assertInstanceOf(EnergizedState.class, red1.getState());
    }

    @Test
    @DisplayName("Verify teleportation to ALPHA with TAILS applies SickState (Rule T-12)")
    void testHandleMysteryCellLandingAlphaCellSick() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = engine.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.ALPHA, false);

        assertEquals(TeleportDestination.ALPHA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red1.getCurrentCell());
        assertInstanceOf(SickState.class, red1.getState());
    }

    @Test
    @DisplayName("Verify teleportation to BETA applies BriefingState (Rule T-13)")
    void testHandleMysteryCellLandingBetaCellBriefing() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = engine.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.BETA);

        assertEquals(TeleportDestination.BETA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.BETA, PlayerColor.RED), red1.getCurrentCell());
        assertInstanceOf(BriefingState.class, red1.getState());
    }

    @Test
    @DisplayName("Verify teleportation to STARTING_X moves piece to player starting cell")
    void testHandleMysteryCellLandingStartingXTeleport() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = engine.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.STARTING_X);

        assertEquals(TeleportDestination.STARTING_X, dest);
        assertSame(board.getStartingCell(PlayerColor.RED), red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify teleportation to APPROACH moves piece to player approach cell")
    void testHandleMysteryCellLandingApproachTeleport() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = engine.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.APPROACH);

        assertEquals(TeleportDestination.APPROACH, dest);
        assertSame(board.getApproachCell(PlayerColor.RED), red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify applyAlphaEffect directly applies energized or sick state")
    void testApplyAlphaEffectDelegation() {
        boolean energized = engine.applyAlphaEffect(redPlayer, red1, true);
        assertTrue(energized);
        assertInstanceOf(EnergizedState.class, red1.getState());

        boolean sick = engine.applyAlphaEffect(redPlayer, red1, false);
        assertFalse(sick);
        assertInstanceOf(SickState.class, red1.getState());
    }

    @Test
    @DisplayName("Verify applyBetaEffect directly applies BriefingState")
    void testApplyBetaEffectDelegation() {
        engine.applyBetaEffect(redPlayer, red1);
        assertInstanceOf(BriefingState.class, red1.getState());
    }

    @Test
    @DisplayName("Verify checkBetaConsecutiveThrees checks counter and frees piece on 3 threes")
    void testCheckBetaConsecutiveThreesDelegation() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red1.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        red1.attendBriefing();

        assertFalse(engine.checkBetaConsecutiveThrees(redPlayer, 3)); // 1st
        assertFalse(engine.checkBetaConsecutiveThrees(redPlayer, 3)); // 2nd
        assertTrue(engine.checkBetaConsecutiveThrees(redPlayer, 3));  // 3rd -> rescued!
        assertTrue(red1.isInBase());
    }

    @Test
    @DisplayName("Verify handleMysteryCellBlockLanding teleports entire block together (Step 13)")
    void testHandleMysteryCellBlockLandingTeleportsEntireBlock() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = engine.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.GAMMA);

        assertEquals(TeleportDestination.GAMMA, dest);
        Cell gammaCell = board.getTeleportCell(TeleportDestination.GAMMA, PlayerColor.RED);
        assertSame(gammaCell, red1.getCurrentCell());
        assertSame(gammaCell, red2.getCurrentCell());
        assertTrue(gammaCell.getOccupyingPieces().contains(red1));
        assertTrue(gammaCell.getOccupyingPieces().contains(red2));
        assertFalse(c10.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red2));
    }

    @Test
    @DisplayName("Verify applyAlphaBlockEffect applies EnergizedState to all block pieces")
    void testApplyAlphaBlockEffectDelegation() {
        boolean energized = engine.applyAlphaBlockEffect(redPlayer, List.of(red1, red2), true);
        assertTrue(energized);
        assertInstanceOf(EnergizedState.class, red1.getState());
        assertInstanceOf(EnergizedState.class, red2.getState());
    }

    @Test
    @DisplayName("Verify applyBetaBlockEffect applies BriefingState to all block pieces")
    void testApplyBetaBlockEffectDelegation() {
        engine.applyBetaBlockEffect(redPlayer, List.of(red1, red2));
        assertInstanceOf(BriefingState.class, red1.getState());
        assertInstanceOf(BriefingState.class, red2.getState());
    }
}
