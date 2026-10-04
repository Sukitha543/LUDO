package com.ludo.engine;

import com.ludo.enums.PlayerColor;
import com.ludo.enums.TeleportDestination;
import com.ludo.factory.GameFactory;
import com.ludo.model.*;
import com.ludo.state.BriefingState;
import com.ludo.state.EnergizedState;
import com.ludo.state.SickState;
import com.ludo.view.GameView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TeleportHandler Unit Tests (Rules T-11, T-12, T-13)")
class TeleportHandlerTest {

    private GameState gameState;
    private Board board;
    private Player redPlayer;
    private Player bluePlayer;
    private Piece red1;
    private Piece red2;
    private Piece blue1;
    private Piece blue2;
    private GameView dummyView;
    private TeleportHandler handler;

    @BeforeEach
    void setUp() {
        gameState = GameFactory.createGame();
        board = gameState.getBoard();

        redPlayer = gameState.getPlayer(PlayerColor.RED);
        bluePlayer = gameState.getPlayer(PlayerColor.BLUE);

        red1 = redPlayer.getPiece(1);
        red2 = redPlayer.getPiece(2);
        blue1 = bluePlayer.getPiece(1);
        blue2 = bluePlayer.getPiece(2);

        dummyView = (GameView) Proxy.newProxyInstance(
                GameView.class.getClassLoader(),
                new Class<?>[]{GameView.class},
                (proxy, method, args) -> null
        );

        handler = new TeleportHandler(gameState, dummyView);
    }

    // =========================================================================
    // Category 1: Construction & Defensive Validation
    // =========================================================================

    @Test
    @DisplayName("Verify constructor rejects null GameState")
    void testConstructorRejectsNullGameState() {
        assertThrows(IllegalArgumentException.class, () -> new TeleportHandler(null, dummyView),
                "Should throw IllegalArgumentException when GameState is null");
    }

    @Test
    @DisplayName("Verify constructor rejects null GameView")
    void testConstructorRejectsNullGameView() {
        assertThrows(IllegalArgumentException.class, () -> new TeleportHandler(gameState, null),
                "Should throw IllegalArgumentException when GameView is null");
    }

    @Test
    @DisplayName("Verify constructor with custom Random instance operates deterministically")
    void testConstructorWithCustomRandom() {
        Random customRandom = new Random(12345L);
        TeleportHandler customHandler = new TeleportHandler(gameState, dummyView, customRandom);
        assertNotNull(customHandler);

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination chosen = customHandler.handleMysteryCellLanding(redPlayer, red1, null);
        assertNotNull(chosen);
    }

    @Test
    @DisplayName("Verify constructor with null Random safely falls back to GameRandom")
    void testConstructorWithNullRandomFallsBack() {
        TeleportHandler fallbackHandler = new TeleportHandler(gameState, dummyView, null);
        assertNotNull(fallbackHandler);

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        assertDoesNotThrow(() -> fallbackHandler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.BASE));
    }

    // =========================================================================
    // Category 2: Single Piece Mystery Cell Teleportation (Rule T-11)
    // =========================================================================

    @Test
    @DisplayName("Verify teleportation to BASE resets piece and places it in player base")
    void testTeleportToDestinationBase() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.BASE);

        assertEquals(TeleportDestination.BASE, dest);
        assertTrue(red1.isInBase(), "Piece must be flagged as inBase");
        assertSame(board.getBaseCell(PlayerColor.RED), red1.getCurrentCell());
        assertTrue(board.getBaseCell(PlayerColor.RED).getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1), "Origin cell occupancy must be cleared");
    }

    @Test
    @DisplayName("Verify teleportation to STARTING_X teleports piece to player starting cell")
    void testTeleportToDestinationStartingX() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.STARTING_X);

        assertEquals(TeleportDestination.STARTING_X, dest);
        assertSame(board.getStartingCell(PlayerColor.RED), red1.getCurrentCell());
        assertTrue(board.getStartingCell(PlayerColor.RED).getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify teleportation to APPROACH teleports piece to player approach cell")
    void testTeleportToDestinationApproach() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.APPROACH);

        assertEquals(TeleportDestination.APPROACH, dest);
        assertSame(board.getApproachCell(PlayerColor.RED), red1.getCurrentCell());
        assertTrue(board.getApproachCell(PlayerColor.RED).getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify teleportation to GAMMA teleports piece to Gamma square")
    void testTeleportToDestinationGamma() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.GAMMA);

        assertEquals(TeleportDestination.GAMMA, dest);
        Cell gammaCell = board.getTeleportCell(TeleportDestination.GAMMA, PlayerColor.RED);
        assertSame(gammaCell, red1.getCurrentCell());
        assertTrue(gammaCell.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify null destination randomly selects a valid destination")
    void testTeleportRandomSelectionWhenDestinationNull() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, null);

        assertNotNull(dest);
        assertNotSame(c10, red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify handleMysteryCellLanding rejects null player or piece")
    void testTeleportRejectsNullPlayerOrPiece() {
        assertThrows(IllegalArgumentException.class, () -> handler.handleMysteryCellLanding(null, red1, TeleportDestination.BASE));
        assertThrows(IllegalArgumentException.class, () -> handler.handleMysteryCellLanding(redPlayer, null, TeleportDestination.BASE));
    }

    // =========================================================================
    // Category 3: Alpha Cell Effect & Coin Toss (Rule T-12)
    // =========================================================================

    @Test
    @DisplayName("Verify teleportation to ALPHA with HEADS applies EnergizedState (Rule T-12)")
    void testTeleportToAlphaAppliesEnergizedOnHeads() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.ALPHA, true);

        assertEquals(TeleportDestination.ALPHA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red1.getCurrentCell());
        assertInstanceOf(EnergizedState.class, red1.getState(), "Piece must transition to EnergizedState");
    }

    @Test
    @DisplayName("Verify teleportation to ALPHA with TAILS applies SickState (Rule T-12)")
    void testTeleportToAlphaAppliesSickOnTails() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.ALPHA, false);

        assertEquals(TeleportDestination.ALPHA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red1.getCurrentCell());
        assertInstanceOf(SickState.class, red1.getState(), "Piece must transition to SickState");
    }

    @Test
    @DisplayName("Verify applyAlphaEffect rejects null piece")
    void testApplyAlphaEffectRejectsNullPiece() {
        assertThrows(IllegalArgumentException.class, () -> handler.applyAlphaEffect(redPlayer, null, true));
    }

    // =========================================================================
    // Category 4: Beta Cell Briefing Effect (Rule T-13)
    // =========================================================================

    @Test
    @DisplayName("Verify teleportation to BETA applies BriefingState (Rule T-13)")
    void testTeleportToBetaAppliesBriefingState() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        TeleportDestination dest = handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.BETA);

        assertEquals(TeleportDestination.BETA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.BETA, PlayerColor.RED), red1.getCurrentCell());
        assertInstanceOf(BriefingState.class, red1.getState(), "Piece must transition to BriefingState (frozen)");
    }

    @Test
    @DisplayName("Verify applyBetaEffect rejects null piece")
    void testApplyBetaEffectRejectsNullPiece() {
        assertThrows(IllegalArgumentException.class, () -> handler.applyBetaEffect(redPlayer, null));
    }

    // =========================================================================
    // Category 5: Beta 3-Consecutive-Threes Rescue (Rule T-13 / Step 15)
    // =========================================================================

    @Test
    @DisplayName("Verify single roll of 3 increments consecutive threes counter and returns false")
    void testBetaSingleRollThreeIncrementsCounter() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red1.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        red1.attendBriefing(); // Set BriefingState

        boolean released = handler.checkBetaConsecutiveThrees(redPlayer, 3);

        assertFalse(released, "Single roll of 3 should not trigger release");
        assertEquals(1, redPlayer.getConsecutiveThrees());
    }

    @Test
    @DisplayName("Verify two consecutive rolls of 3 increment counter to 2 and return false")
    void testBetaTwoConsecutiveThreesIncrementsCounter() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red1.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        red1.attendBriefing();

        handler.checkBetaConsecutiveThrees(redPlayer, 3);
        boolean released = handler.checkBetaConsecutiveThrees(redPlayer, 3);

        assertFalse(released);
        assertEquals(2, redPlayer.getConsecutiveThrees());
    }

    @Test
    @DisplayName("Verify three consecutive rolls of 3 teleports piece from Beta to Base (Rule T-13)")
    void testBetaThreeConsecutiveThreesTeleportsSinglePieceToBase() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red1.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        red1.attendBriefing();

        handler.checkBetaConsecutiveThrees(redPlayer, 3);
        handler.checkBetaConsecutiveThrees(redPlayer, 3);
        boolean released = handler.checkBetaConsecutiveThrees(redPlayer, 3);

        assertTrue(released, "Three consecutive 3s must trigger release to Base");
        assertEquals(0, redPlayer.getConsecutiveThrees(), "Counter should reset to 0");
        assertTrue(red1.isInBase(), "Piece must be in Base");
        assertSame(board.getBaseCell(PlayerColor.RED), red1.getCurrentCell());
        assertFalse(betaCell.getOccupyingPieces().contains(red1));
    }

    @Test
    @DisplayName("Verify three consecutive rolls of 3 teleports an entire block from Beta to Base")
    void testBetaThreeConsecutiveThreesTeleportsBlockToBase() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(betaCell);
        red2.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        betaCell.addPiece(red2);
        red1.attendBriefing();
        red2.attendBriefing();

        handler.checkBetaConsecutiveThrees(redPlayer, 3);
        handler.checkBetaConsecutiveThrees(redPlayer, 3);
        boolean released = handler.checkBetaConsecutiveThrees(redPlayer, 3);

        assertTrue(released, "Three consecutive 3s must release entire block from Beta");
        assertEquals(0, redPlayer.getConsecutiveThrees());
        assertTrue(red1.isInBase());
        assertTrue(red2.isInBase());
        assertSame(board.getBaseCell(PlayerColor.RED), red1.getCurrentCell());
        assertSame(board.getBaseCell(PlayerColor.RED), red2.getCurrentCell());
        assertFalse(betaCell.getOccupyingPieces().contains(red1));
        assertFalse(betaCell.getOccupyingPieces().contains(red2));
    }

    @Test
    @DisplayName("Verify non-three roll resets consecutive threes counter")
    void testBetaNonThreeRollResetsCounter() {
        Cell betaCell = board.getBetaCell();
        red1.setInBase(false);
        red1.setCurrentCell(betaCell);
        betaCell.addPiece(red1);
        red1.attendBriefing();

        handler.checkBetaConsecutiveThrees(redPlayer, 3);
        handler.checkBetaConsecutiveThrees(redPlayer, 3);
        assertEquals(2, redPlayer.getConsecutiveThrees());

        boolean released = handler.checkBetaConsecutiveThrees(redPlayer, 4);

        assertFalse(released);
        assertEquals(0, redPlayer.getConsecutiveThrees(), "Rolling non-3 must reset consecutive count to 0");
    }

    @Test
    @DisplayName("Verify Beta check with no pieces in BriefingState in Beta resets counter")
    void testBetaCheckWithNoPiecesInBetaResetsCounter() {
        redPlayer.incrementConsecutiveThrees();
        assertEquals(1, redPlayer.getConsecutiveThrees());

        boolean released = handler.checkBetaConsecutiveThrees(redPlayer, 3);

        assertFalse(released);
        assertEquals(0, redPlayer.getConsecutiveThrees(), "Should reset when no pieces in Beta are in BriefingState");
    }

    // =========================================================================
    // Category 6: Combat on Teleport Landing (Rule 6 & Rule T-8)
    // =========================================================================

    @Test
    @DisplayName("Verify teleporting onto a lone opponent piece triggers capture and sends it to base (Rule 6)")
    void testTeleportLandsOnLoneOpponentTriggersCapture() {
        Cell targetApproach = board.getApproachCell(PlayerColor.RED);
        blue1.setInBase(false);
        blue1.setCurrentCell(targetApproach);
        targetApproach.addPiece(blue1);

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.APPROACH);

        assertTrue(blue1.isInBase(), "Opponent piece must be returned to base");
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue1.getCurrentCell());
        assertEquals(1, red1.getCaptureCount(), "Capturing piece capture count must increment");
        assertSame(targetApproach, red1.getCurrentCell());
    }

    @Test
    @DisplayName("Verify teleporting onto a friendly piece forms a block")
    void testTeleportLandsOnFriendlyPieceFormsBlock() {
        Cell targetStart = board.getStartingCell(PlayerColor.RED);
        red2.setInBase(false);
        red2.setCurrentCell(targetStart);
        targetStart.addPiece(red2);

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red1.setCurrentCell(c10);
        c10.addPiece(red1);

        handler.handleMysteryCellLanding(redPlayer, red1, TeleportDestination.STARTING_X);

        assertSame(targetStart, red1.getCurrentCell());
        assertTrue(targetStart.isBlock(), "Target cell must form a block with friendly piece");
    }

    // =========================================================================
    // Category 7: Block Mystery Cell Teleportation & Special Effects (Step 13)
    // =========================================================================

    @Test
    @DisplayName("Verify block teleportation to BASE resets all block pieces to base")
    void testBlockTeleportToBase() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.BASE);

        assertEquals(TeleportDestination.BASE, dest);
        assertTrue(red1.isInBase());
        assertTrue(red2.isInBase());
        assertSame(board.getBaseCell(PlayerColor.RED), red1.getCurrentCell());
        assertSame(board.getBaseCell(PlayerColor.RED), red2.getCurrentCell());
        assertFalse(c10.getOccupyingPieces().contains(red1));
        assertFalse(c10.getOccupyingPieces().contains(red2));
    }

    @Test
    @DisplayName("Verify block teleportation to GAMMA moves all pieces together to Gamma cell")
    void testBlockTeleportToGamma() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.GAMMA);

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
    @DisplayName("Verify block teleportation to STARTING_X moves all pieces together to start square")
    void testBlockTeleportToStartingX() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.STARTING_X);

        assertEquals(TeleportDestination.STARTING_X, dest);
        assertSame(board.getStartingCell(PlayerColor.RED), red1.getCurrentCell());
        assertSame(board.getStartingCell(PlayerColor.RED), red2.getCurrentCell());
    }

    @Test
    @DisplayName("Verify block teleportation to APPROACH moves all pieces together to approach square")
    void testBlockTeleportToApproach() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.APPROACH);

        assertEquals(TeleportDestination.APPROACH, dest);
        assertSame(board.getApproachCell(PlayerColor.RED), red1.getCurrentCell());
        assertSame(board.getApproachCell(PlayerColor.RED), red2.getCurrentCell());
    }

    @Test
    @DisplayName("Verify block teleportation to ALPHA with HEADS applies EnergizedState to all pieces")
    void testBlockTeleportToAlphaAppliesEnergizedToAllPieces() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.ALPHA, true);

        assertEquals(TeleportDestination.ALPHA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red1.getCurrentCell());
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red2.getCurrentCell());
        assertInstanceOf(EnergizedState.class, red1.getState(), "Piece 1 must be energized");
        assertInstanceOf(EnergizedState.class, red2.getState(), "Piece 2 must be energized");
    }

    @Test
    @DisplayName("Verify block teleportation to ALPHA with TAILS applies SickState to all pieces (User Requested)")
    void testBlockTeleportToAlphaAppliesSickToAllPieces() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.ALPHA, false);

        assertEquals(TeleportDestination.ALPHA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red1.getCurrentCell());
        assertSame(board.getTeleportCell(TeleportDestination.ALPHA, PlayerColor.RED), red2.getCurrentCell());
        assertInstanceOf(SickState.class, red1.getState(), "Piece 1 must be sick");
        assertInstanceOf(SickState.class, red2.getState(), "Piece 2 must be sick");
    }

    @Test
    @DisplayName("Verify block teleportation to BETA applies BriefingState to all pieces")
    void testBlockTeleportToBetaAppliesBriefingToAllPieces() {
        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        TeleportDestination dest = handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.BETA);

        assertEquals(TeleportDestination.BETA, dest);
        assertSame(board.getTeleportCell(TeleportDestination.BETA, PlayerColor.RED), red1.getCurrentCell());
        assertSame(board.getTeleportCell(TeleportDestination.BETA, PlayerColor.RED), red2.getCurrentCell());
        assertInstanceOf(BriefingState.class, red1.getState(), "Piece 1 must attend briefing");
        assertInstanceOf(BriefingState.class, red2.getState(), "Piece 2 must attend briefing");
    }

    @Test
    @DisplayName("Verify block landing captures equal-sized opponent block and increments capture counts (Rule T-8)")
    void testBlockTeleportCapturesEqualSizedOpponentBlock() {
        Cell gammaCell = board.getTeleportCell(TeleportDestination.GAMMA, PlayerColor.RED);
        blue1.setInBase(false);
        blue2.setInBase(false);
        blue1.setCurrentCell(gammaCell);
        blue2.setCurrentCell(gammaCell);
        gammaCell.addPiece(blue1);
        gammaCell.addPiece(blue2);
        assertTrue(gammaCell.isBlock());

        Cell c10 = board.getStandardCell(10);
        red1.setInBase(false);
        red2.setInBase(false);
        red1.setCurrentCell(c10);
        red2.setCurrentCell(c10);
        c10.addPiece(red1);
        c10.addPiece(red2);

        handler.handleMysteryCellBlockLanding(redPlayer, List.of(red1, red2), TeleportDestination.GAMMA);

        assertTrue(blue1.isInBase(), "Blue piece 1 must be reset to base");
        assertTrue(blue2.isInBase(), "Blue piece 2 must be reset to base");
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue1.getCurrentCell());
        assertSame(board.getBaseCell(PlayerColor.BLUE), blue2.getCurrentCell());

        assertEquals(1, red1.getCaptureCount(), "Red piece 1 capture count must increment");
        assertEquals(1, red2.getCaptureCount(), "Red piece 2 capture count must increment");
        assertEquals(1, red1.getBlockCaptureCount(), "Red piece 1 block capture count must increment");
        assertEquals(1, red2.getBlockCaptureCount(), "Red piece 2 block capture count must increment");
    }

    @Test
    @DisplayName("Verify handleMysteryCellBlockLanding safely handles null or empty piece list")
    void testBlockTeleportRejectsNullOrEmptyBlock() {
        assertNull(handler.handleMysteryCellBlockLanding(redPlayer, null, TeleportDestination.BASE));
        assertNull(handler.handleMysteryCellBlockLanding(redPlayer, List.of(), TeleportDestination.BASE));
        assertThrows(IllegalArgumentException.class, () -> handler.handleMysteryCellBlockLanding(null, List.of(red1), TeleportDestination.BASE));
    }
}
