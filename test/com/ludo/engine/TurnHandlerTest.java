package com.ludo.engine;

import com.ludo.enums.Direction;
import com.ludo.enums.PlayerColor;
import com.ludo.execptions.GameEngineException;
import com.ludo.factory.GameFactory;
import com.ludo.model.*;
import com.ludo.view.GameView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TurnHandler Unit Tests (Rules 4, T-2, T-6, T-13, T-14)")
class TurnHandlerTest {

    private GameState gameState;
    private Board board;
    private Player redPlayer;
    private Player bluePlayer;
    private Piece red1;
    private Piece red2;
    private Piece red3;
    private Piece red4;
    private Piece blue1;
    private GameView dummyView;
    private TeleportHandler teleportHandler;
    private MoveExecutor moveExecutor;
    private TurnHandler handler;

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

        dummyView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> null
        );

        teleportHandler = new TeleportHandler(gameState, dummyView);
        moveExecutor = new MoveExecutor(gameState, dummyView, teleportHandler);
        handler = new TurnHandler(gameState, dummyView, moveExecutor, teleportHandler);
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
    // Category 1: Construction & Defensive Validation
    // =========================================================================

    @Test
    @DisplayName("Verify constructor rejects null GameState")
    void testConstructorRejectsNullGameState() {
        assertThrows(IllegalArgumentException.class,
                () -> new TurnHandler(null, dummyView, moveExecutor, teleportHandler),
                "Should throw IllegalArgumentException when GameState is null");
    }

    @Test
    @DisplayName("Verify constructor rejects null GameView")
    void testConstructorRejectsNullGameView() {
        assertThrows(IllegalArgumentException.class,
                () -> new TurnHandler(gameState, null, moveExecutor, teleportHandler),
                "Should throw IllegalArgumentException when GameView is null");
    }

    @Test
    @DisplayName("Verify constructor safely defaults null MoveExecutor")
    void testConstructorGracefullyDefaultsNullMoveExecutor() {
        TurnHandler h = new TurnHandler(gameState, dummyView, null, teleportHandler);
        assertNotNull(h);
    }

    @Test
    @DisplayName("Verify constructor safely defaults null TeleportHandler")
    void testConstructorGracefullyDefaultsNullTeleportHandler() {
        TurnHandler h = new TurnHandler(gameState, dummyView, moveExecutor, null);
        assertNotNull(h);
    }

    @Test
    @DisplayName("Verify executePlayerTurn rejects null Player")
    void testExecutePlayerTurnRejectsNullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> handler.executePlayerTurn(null));
    }

    @Test
    @DisplayName("Verify executePlayerTurn rejects out-of-range dice rolls")
    void testExecutePlayerTurnRejectsInvalidDiceRoll() {
        Queue<Integer> invalidRoll = new LinkedList<>(List.of(7));
        assertThrows(GameEngineException.class, () -> handler.executePlayerTurn(redPlayer, invalidRoll));

        Queue<Integer> zeroRoll = new LinkedList<>(List.of(0));
        assertThrows(GameEngineException.class, () -> handler.executePlayerTurn(redPlayer, zeroRoll));
    }

    // =========================================================================
    // Category 2: Single Turn & Clean Termination (Rule 4)
    // =========================================================================

    @Test
    @DisplayName("Verify single roll without bonus completes turn after 1 attempt")
    void testSingleRollWithoutBonusEndsTurn() {
        Queue<Integer> rolls = new LinkedList<>(List.of(4));
        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty(), "Single roll should be fully consumed");
    }

    @Test
    @DisplayName("Verify turn concludes cleanly when player has no valid moves")
    void testPlayerWithNoLegalMovesPassesTurn() {
        // All pieces in base, roll is 2 -> 0 legal moves possible
        assertEquals(4, redPlayer.getPiecesInBase().size());
        Queue<Integer> rolls = new LinkedList<>(List.of(2));

        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty());
        assertEquals(4, redPlayer.getPiecesInBase().size(), "Pieces remain in base");
    }

    @Test
    @DisplayName("Verify single normal move advances piece and concludes turn without bonus")
    void testSingleNormalMoveAdvancesAndEndsTurn() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        Queue<Integer> rolls = new LinkedList<>(List.of(3));
        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty());
        assertSame(board.getStandardCell(13), red1.getCurrentCell(), "Piece should advance 3 cells to cell 13");
    }

    // =========================================================================
    // Category 3: Bonus Rolls on Six (Rule 4)
    // =========================================================================

    @Test
    @DisplayName("Verify rolling a 6 grants an immediate bonus roll attempt (Rule 4)")
    void testRollSixGrantsBonusAttempt() {
        AtomicBoolean bonusAnnounced = new AtomicBoolean(false);
        GameView capturingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayBonusAttempt".equals(method.getName())) {
                        bonusAnnounced.set(true);
                    }
                    return null;
                }
        );

        TurnHandler testHandler = new TurnHandler(gameState, capturingView, moveExecutor, teleportHandler);

        // Roll 6 deploys from base, then roll 3 advances 3 steps
        Queue<Integer> rolls = new LinkedList<>(List.of(6, 3));
        testHandler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty(), "Both rolls (6 + bonus attempt 3) must be consumed");
        assertTrue(bonusAnnounced.get(), "Bonus attempt notification must be displayed");
        assertEquals(3, redPlayer.getPiecesInBase().size(), "One piece deployed from base");
    }

    @Test
    @DisplayName("Verify rolling 6 twice grants two consecutive bonus attempts up to 3 attempts total")
    void testTwoConsecutiveSixesGrantsTwoBonusAttempts() {
        // Roll 6 (deploy), roll 6 (advance 6), roll 2 (advance 2)
        Queue<Integer> rolls = new LinkedList<>(List.of(6, 6, 2));
        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty(), "All 3 rolls must be consumed");
    }

    @Test
    @DisplayName("Verify maximum of 3 attempts is strictly enforced (Rule 4)")
    void testMaxThreeAttemptsEnforcedOnSixes() {
        // Even with queue containing 4 rolls, only 3 attempts are permitted
        Queue<Integer> rolls = new LinkedList<>(List.of(6, 6, 6, 5));
        handler.executePlayerTurn(redPlayer, rolls);

        assertEquals(1, rolls.size(), "4th roll must remain unconsumed as max attempts = 3");
    }

    // =========================================================================
    // Category 4: Bonus Rolls on Captures (Rule T-2)
    // =========================================================================

    @Test
    @DisplayName("Verify capturing an opponent piece grants an immediate bonus roll attempt (Rule T-2)")
    void testCaptureGrantsBonusAttempt() {
        AtomicBoolean captureBonusAnnounced = new AtomicBoolean(false);
        AtomicReference<String> reasonCaptured = new AtomicReference<>();

        GameView capturingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayBonusAttempt".equals(method.getName())) {
                        captureBonusAnnounced.set(true);
                        reasonCaptured.set((String) args[1]);
                    }
                    return null;
                }
        );

        TurnHandler testHandler = new TurnHandler(gameState, capturingView, moveExecutor, teleportHandler);

        Cell c10 = board.getStandardCell(10);
        Cell c14 = board.getStandardCell(14);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        blue1.setInBase(false);
        blue1.setCurrentCell(c14);
        c14.addPiece(blue1);

        // Roll 4 captures blue1 on cell 14, then roll 2 advances 2 steps to cell 16
        Queue<Integer> rolls = new LinkedList<>(List.of(4, 2));
        testHandler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty(), "Both rolls must be consumed");
        assertTrue(captureBonusAnnounced.get(), "Bonus attempt must be announced for capture");
        assertEquals("captured an opponent piece", reasonCaptured.get());
        assertTrue(blue1.isInBase(), "Blue piece must be sent back to base");
        assertSame(board.getStandardCell(16), red1.getCurrentCell(), "Piece should advance on bonus roll to cell 16");
    }

    @Test
    @DisplayName("Verify consecutive captures grant multiple bonus attempts up to 3 attempts total")
    void testConsecutiveCapturesGrantBonusUpToMaxThreeAttempts() {
        Cell c10 = board.getStandardCell(10);
        Cell c12 = board.getStandardCell(12);
        Cell c14 = board.getStandardCell(14);

        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        // First opponent waiting at cell 12
        blue1.setInBase(false);
        blue1.setCurrentCell(c12);
        c12.addPiece(blue1);

        // Second opponent waiting at cell 14
        Piece blue2 = bluePlayer.getPiece(2);
        blue2.setInBase(false);
        blue2.setCurrentCell(c14);
        c14.addPiece(blue2);

        // Roll 2 (captures blue1), bonus roll 2 (captures blue2), bonus roll 3 (advances to 17)
        Queue<Integer> rolls = new LinkedList<>(List.of(2, 2, 3));
        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty(), "All 3 attempts should be consumed");
        assertTrue(blue1.isInBase());
        assertTrue(blue2.isInBase());
        assertSame(board.getStandardCell(17), red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify capture on 3rd attempt does not grant a 4th attempt")
    void testNoBonusWhenCaptureOccursOnThirdAttempt() {
        // Red pieces 2, 3, 4 in home so only red1 is active on board
        placePieceInHome(red2);
        placePieceInHome(red3);
        placePieceInHome(red4);

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        // Opponent at cell 24
        Cell c24 = board.getStandardCell(24);
        blue1.setInBase(false);
        blue1.setCurrentCell(c24);
        c24.addPiece(blue1);

        // Attempt 1: roll 6 (moves red1 from 10 to 16) -> grants bonus attempt
        // Attempt 2: roll 6 (moves red1 from 16 to 22) -> grants bonus attempt
        // Attempt 3: roll 2 (moves red1 from 22 to 24, capturing blue1!) -> attempt == 3, so no 4th attempt granted
        // Queue contains roll 5 which must remain UNCONSUMED
        Queue<Integer> rolls = new LinkedList<>(List.of(6, 6, 2, 5));
        handler.executePlayerTurn(redPlayer, rolls);

        assertEquals(1, rolls.size(), "4th roll must remain unconsumed");
        assertEquals(5, rolls.peek().intValue());
        assertTrue(blue1.isInBase(), "Blue piece was captured on 3rd attempt");
        assertSame(c24, red1.getCurrentCell(), "Red piece must be on cell 24");
    }

    // =========================================================================
    // Category 5: Three-Sixes Penalty (Rule T-6, Rule T-14)
    // =========================================================================

    @Test
    @DisplayName("Verify three consecutive sixes breaks active blockade (Rule T-6)")
    void testThreeConsecutiveSixesBreaksBlockadeWhenBlockadePresent() {
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

        Queue<Integer> rolls = new LinkedList<>(List.of(6, 6, 6));
        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty());
        assertFalse(c10.isBlock(), "Active blockade must be broken on 3 consecutive sixes");
        assertSame(c10, red1.getCurrentCell(), "Piece index 0 remains on original cell");
        assertSame(board.getStandardCell(16), red2.getCurrentCell(), "Other piece moves 6 cells forward");
    }

    @Test
    @DisplayName("Verify three consecutive sixes without blockade announces penalty and terminates turn")
    void testThreeConsecutiveSixesWithoutBlockadeDisplaysPenalty() {
        AtomicBoolean penaltyAnnounced = new AtomicBoolean(false);
        GameView capturingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayThreeSixesPenalty".equals(method.getName())) {
                        penaltyAnnounced.set(true);
                    }
                    return null;
                }
        );

        TurnHandler testHandler = new TurnHandler(gameState, capturingView, moveExecutor, teleportHandler);

        // Put pieces 2, 3, 4 into Home so Red only has red1 on board and cannot form a blockade
        placePieceInHome(red2);
        placePieceInHome(red3);
        placePieceInHome(red4);

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        assertFalse(board.hasBlockade(redPlayer.getColor()));

        // Player has no blockades: 3 consecutive sixes trigger penalty display
        Queue<Integer> rolls = new LinkedList<>(List.of(6, 6, 6));
        testHandler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty(), "All 3 sixes should be processed");
        assertTrue(penaltyAnnounced.get(), "Three sixes penalty should be displayed when no blockade exists");
    }

    // =========================================================================
    // Category 6: Integration with Beta 3-Threes Rescue (Rule T-13)
    // =========================================================================

    @Test
    @DisplayName("Verify each roll triggers checkBetaConsecutiveThrees on TeleportHandler")
    void testTurnChecksBetaConsecutiveThreesOnEachRoll() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red1.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        red1.attendBriefing(); // BriefingState

        assertEquals(0, redPlayer.getConsecutiveThrees());

        // Roll 3 once during turn
        Queue<Integer> rolls = new LinkedList<>(List.of(3));
        handler.executePlayerTurn(redPlayer, rolls);

        assertEquals(1, redPlayer.getConsecutiveThrees(),
                "Consecutive threes counter should be incremented via checkBetaConsecutiveThrees");
    }

    @Test
    @DisplayName("Verify rolling 3 three times across turns releases piece from Beta to Base (Rule T-13)")
    void testBetaThreeConsecutiveThreesRescueAcrossTurns() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red1.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        red1.attendBriefing();

        // Turn 1: rolls 3
        handler.executePlayerTurn(redPlayer, new LinkedList<>(List.of(3)));
        assertEquals(1, redPlayer.getConsecutiveThrees());

        // Turn 2: rolls 3
        handler.executePlayerTurn(redPlayer, new LinkedList<>(List.of(3)));
        assertEquals(2, redPlayer.getConsecutiveThrees());

        // Turn 3: rolls 3 -> rescues piece to Base!
        handler.executePlayerTurn(redPlayer, new LinkedList<>(List.of(3)));
        assertEquals(0, redPlayer.getConsecutiveThrees(), "Counter must reset upon rescue");
        assertTrue(red1.isInBase(), "Piece must be released to Base");
        assertSame(board.getBaseCell(PlayerColor.RED), red1.getCurrentCell());
        assertFalse(betaCell.getOccupyingPieces().contains(red1));
    }

    // =========================================================================
    // Category 7: Game Over Early Exit
    // =========================================================================

    @Test
    @DisplayName("Verify turn terminates immediately when game is won, discarding remaining attempts")
    void testTurnTerminatesImmediatelyWhenGameIsWon() {
        Cell homeCell = board.getHomeCell(PlayerColor.RED);
        Cell lastHomePath = board.getHomeStraightCell(PlayerColor.RED, 4);

        // Pieces 1, 2, 3 already finished
        placePieceInHome(red1);
        placePieceInHome(red2);
        placePieceInHome(red3);

        // Piece 4 enters Home on roll 1
        red4.setInBase(false);
        board.getBaseCell(PlayerColor.RED).removePiece(red4);
        red4.setCurrentCell(lastHomePath);
        lastHomePath.addPiece(red4);

        // Rolls queue contains roll 1 (winning move) and unused roll 5
        Queue<Integer> rolls = new LinkedList<>(List.of(1, 5));
        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(gameState.isGameOver(), "Game must be game over");
        assertSame(redPlayer, gameState.getWinner());
        assertEquals(1, rolls.size(), "Remaining rolls must be discarded when game is won");
    }

    @Test
    @DisplayName("Verify turn terminates immediately when game is won on a roll of six")
    void testTurnTerminatesImmediatelyWhenGameIsWonOnRollSix() {
        Cell approachCell = board.getApproachCell(PlayerColor.RED);

        // Pieces 1, 2, 3 already finished
        placePieceInHome(red1);
        placePieceInHome(red2);
        placePieceInHome(red3);

        // Piece 4 is at Approach cell with 1 capture, ready to enter Home on roll 6 (Approach -> 5 straight cells -> Home)
        red4.setInBase(false);
        board.getBaseCell(PlayerColor.RED).removePiece(red4);
        red4.setCurrentCell(approachCell);
        red4.incrementCaptureCount();
        approachCell.addPiece(red4);

        Queue<Integer> rolls = new LinkedList<>(List.of(6, 2));
        handler.executePlayerTurn(redPlayer, rolls);

        assertTrue(gameState.isGameOver(), "Game must be game over");
        assertSame(redPlayer, gameState.getWinner());
        assertEquals(1, rolls.size(), "Bonus rolls must be aborted once game is won");
    }

    @Test
    @DisplayName("Verify rolling 6 with a capture displays capture reason instead of rolled a six")
    void testRollSixWithCaptureDisplaysCaptureBonus() {
        AtomicReference<String> reasonCaptured = new AtomicReference<>();
        GameView capturingView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> {
                    if ("displayBonusAttempt".equals(method.getName())) {
                        reasonCaptured.set((String) args[1]);
                    }
                    return null;
                }
        );

        TurnHandler testHandler = new TurnHandler(gameState, capturingView, moveExecutor, teleportHandler);

        // Place red1 on cell 10, blue1 on cell 16 (6 steps away)
        placePieceInHome(red2);
        placePieceInHome(red3);
        placePieceInHome(red4);

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        Cell c16 = board.getStandardCell(16);
        blue1.setInBase(false);
        blue1.setCurrentCell(c16);
        c16.addPiece(blue1);

        // Roll 6 captures blue1, then second roll 2 moves red1 to 18
        Queue<Integer> rolls = new LinkedList<>(List.of(6, 2));
        testHandler.executePlayerTurn(redPlayer, rolls);

        assertTrue(rolls.isEmpty());
        assertEquals("captured an opponent piece", reasonCaptured.get(),
                "Bonus reason should prioritize capture over rolled a six");
        assertTrue(blue1.isInBase());
        assertSame(board.getStandardCell(18), red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify executePlayerTurn without scripted rolls uses singleton dice")
    void testExecutePlayerTurnWithoutScriptedRolls() {
        // Player with all pieces in base will roll and pass or deploy safely
        assertDoesNotThrow(() -> handler.executePlayerTurn(redPlayer),
                "Executing player turn with real dice roll should run cleanly without exception");
    }

    @Test
    @DisplayName("Verify playMove handles null chosenMove defensively without crashing")
    void testPlayMoveHandlesNullChosenMoveDefensively() {
        // Set a strategy that returns null for chosenMove
        Player dummyPlayer = new Player(PlayerColor.RED, (p, moves, b, roll) -> null);
        Queue<Integer> rolls = new LinkedList<>(List.of(4));

        assertDoesNotThrow(() -> handler.executePlayerTurn(dummyPlayer, rolls),
                "Null chosenMove should be caught and handled gracefully");
        assertTrue(rolls.isEmpty());
    }
}
