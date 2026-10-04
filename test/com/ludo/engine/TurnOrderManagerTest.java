package com.ludo.engine;

import com.ludo.enums.PlayerColor;
import com.ludo.execptions.GameEngineException;
import com.ludo.factory.GameFactory;
import com.ludo.model.*;
import com.ludo.view.GameView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TurnOrderManager Unit Tests (Rule 1 & Clockwise Order)")
class TurnOrderManagerTest {

    private GameState gameState;
    private Board board;
    private Player redPlayer;
    private Player greenPlayer;
    private Player yellowPlayer;
    private Player bluePlayer;
    private GameView dummyView;
    private TeleportHandler teleportHandler;
    private MoveExecutor moveExecutor;
    private TurnOrderManager manager;

    @BeforeEach
    void setUp() {
        gameState = GameFactory.createGame();
        board = gameState.getBoard();

        redPlayer = gameState.getPlayer(PlayerColor.RED);
        greenPlayer = gameState.getPlayer(PlayerColor.GREEN);
        yellowPlayer = gameState.getPlayer(PlayerColor.YELLOW);
        bluePlayer = gameState.getPlayer(PlayerColor.BLUE);

        dummyView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> null
        );

        teleportHandler = new TeleportHandler(gameState, dummyView);
        moveExecutor = new MoveExecutor(gameState, dummyView, teleportHandler);
        manager = new TurnOrderManager(gameState, dummyView, moveExecutor);
    }

    // =========================================================================
    // Category 1: Construction & Defensive Validation
    // =========================================================================

    @Test
    @DisplayName("Verify constructor rejects null GameState")
    void testConstructorRejectsNullGameState() {
        assertThrows(IllegalArgumentException.class, () -> new TurnOrderManager(null, dummyView, moveExecutor),
                "Should throw IllegalArgumentException when GameState is null");
    }

    @Test
    @DisplayName("Verify constructor rejects null GameView")
    void testConstructorRejectsNullGameView() {
        assertThrows(IllegalArgumentException.class, () -> new TurnOrderManager(gameState, null, moveExecutor),
                "Should throw IllegalArgumentException when GameView is null");
    }

    @Test
    @DisplayName("Verify constructor safely accepts null MoveExecutor")
    void testConstructorAcceptsNullMoveExecutor() {
        TurnOrderManager nullExecutorManager = new TurnOrderManager(gameState, dummyView, null);
        assertNotNull(nullExecutorManager);
    }

    @Test
    @DisplayName("Verify initial state has empty roundOrder")
    void testConstructorInitialState() {
        assertNotNull(manager.getRoundOrder());
        assertTrue(manager.getRoundOrder().isEmpty(), "Initial roundOrder should be empty");
    }

    // =========================================================================
    // Category 2: Clockwise Order Generation
    // =========================================================================

    @Test
    @DisplayName("Verify buildClockwiseOrder starts from winning player and continues clockwise")
    void testBuildClockwiseOrderStartsFromWinner() {
        List<Player> standardOrder = List.of(redPlayer, greenPlayer, yellowPlayer, bluePlayer);

        // Yellow (index 2) begins -> [Yellow, Blue, Red, Green]
        List<Player> order = manager.buildClockwiseOrder(yellowPlayer, standardOrder);
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

        List<Player> order = manager.buildClockwiseOrder(starter, standardOrder);
        assertEquals(4, order.size());
        assertSame(starter, order.get(0));

        // Ensure all 4 unique players are present
        assertEquals(4, order.stream().distinct().count());
    }

    @Test
    @DisplayName("Verify buildClockwiseOrder rejects null starting player")
    void testBuildClockwiseOrderRejectsNullStartingPlayer() {
        List<Player> standardOrder = gameState.getPlayers();
        assertThrows(IllegalArgumentException.class, () -> manager.buildClockwiseOrder(null, standardOrder));
    }

    @Test
    @DisplayName("Verify buildClockwiseOrder rejects null or empty standardOrder list")
    void testBuildClockwiseOrderRejectsNullOrEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> manager.buildClockwiseOrder(redPlayer, null));
        assertThrows(IllegalArgumentException.class, () -> manager.buildClockwiseOrder(redPlayer, Collections.emptyList()));
    }

    @Test
    @DisplayName("Verify buildClockwiseOrder rejects player not in standardOrder list")
    void testBuildClockwiseOrderRejectsPlayerNotInList() {
        List<Player> standardOrder = gameState.getPlayers();
        Player outsider = new Player(PlayerColor.RED, null);
        assertThrows(IllegalArgumentException.class, () -> manager.buildClockwiseOrder(outsider, standardOrder));
    }

    // =========================================================================
    // Category 3: First Player Determination (Rule 1)
    // =========================================================================

    @Test
    @DisplayName("Verify determineFirstPlayer rolls until a 6 is rolled and returns that player")
    void testDetermineFirstPlayerRollsUntilSix() {
        Player firstPlayer = manager.determineFirstPlayer();

        assertNotNull(firstPlayer);
        assertTrue(gameState.getPlayers().contains(firstPlayer), "Winner must be one of the game's players");
    }

    @Test
    @DisplayName("Verify determineFirstPlayer establishes round order starting from first player")
    void testDetermineFirstPlayerEstablishesRoundOrder() {
        Player firstPlayer = manager.determineFirstPlayer();

        List<Player> roundOrder = manager.getRoundOrder();
        assertEquals(4, roundOrder.size());
        assertSame(firstPlayer, roundOrder.get(0), "roundOrder must start with the player who rolled 6");
    }

    @Test
    @DisplayName("Verify determineFirstPlayer deploys winner's first piece to starting square 'X'")
    void testDetermineFirstPlayerDeploysFirstPieceToStartSquare() {
        Player firstPlayer = manager.determineFirstPlayer();

        Cell startCell = board.getStartingCell(firstPlayer.getColor());
        assertTrue(startCell.isOccupied(), "Winner's starting cell must be occupied by deployed piece");

        Piece deployed = startCell.getOccupyingPieces().get(0);
        assertSame(firstPlayer.getColor(), deployed.getColor());
        assertFalse(deployed.isInBase(), "Deployed piece must no longer be in base");
        assertNotNull(deployed.getDirection(), "Deployed piece must have a direction from coin toss");
    }

    @Test
    @DisplayName("Verify determineFirstPlayer operates safely even when MoveExecutor is null")
    void testDetermineFirstPlayerWithNullMoveExecutorSafelyExecutes() {
        TurnOrderManager nullExecutorManager = new TurnOrderManager(gameState, dummyView, null);

        Player firstPlayer = nullExecutorManager.determineFirstPlayer();

        assertNotNull(firstPlayer);
        assertEquals(4, nullExecutorManager.getRoundOrder().size());
        assertSame(firstPlayer, nullExecutorManager.getRoundOrder().get(0));
    }

    @Test
    @DisplayName("Verify determineFirstPlayer throws GameEngineException when Dice is missing")
    void testDetermineFirstPlayerThrowsWhenDiceMissing() {
        GameState noDiceState = new GameState(board, gameState.getPlayers(), Collections.emptyMap(), null, gameState.getCoin());
        TurnOrderManager brokenManager = new TurnOrderManager(noDiceState, dummyView, moveExecutor);

        assertThrows(GameEngineException.class, brokenManager::determineFirstPlayer,
                "Should throw GameEngineException when Dice is null");
    }

    @Test
    @DisplayName("Verify determineFirstPlayer throws GameEngineException when players list is empty")
    void testDetermineFirstPlayerThrowsWhenNoPlayersAvailable() {
        GameState emptyPlayersState = new GameState(board, Collections.emptyList(), Collections.emptyMap(), gameState.getDice(), gameState.getCoin());
        TurnOrderManager brokenManager = new TurnOrderManager(emptyPlayersState, dummyView, moveExecutor);

        assertThrows(GameEngineException.class, brokenManager::determineFirstPlayer,
                "Should throw GameEngineException when player list is empty");
    }

    // =========================================================================
    // Category 4: Round Order Accessors & Mutation
    // =========================================================================

    @Test
    @DisplayName("Verify setRoundOrder updates roundOrder and getRoundOrder returns it")
    void testSetAndGetRoundOrder() {
        List<Player> customOrder = List.of(greenPlayer, bluePlayer, redPlayer, yellowPlayer);
        manager.setRoundOrder(customOrder);

        assertEquals(customOrder, manager.getRoundOrder());
        assertSame(greenPlayer, manager.getRoundOrder().get(0));
    }

    @Test
    @DisplayName("Verify setRoundOrder with null resets roundOrder to empty list")
    void testSetRoundOrderWithNullSetsEmptyList() {
        manager.setRoundOrder(List.of(redPlayer, bluePlayer));
        assertFalse(manager.getRoundOrder().isEmpty());

        manager.setRoundOrder(null);
        assertNotNull(manager.getRoundOrder());
        assertTrue(manager.getRoundOrder().isEmpty(), "Passing null should reset roundOrder to an empty list");
    }
}
