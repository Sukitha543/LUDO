package com.ludo.view;

import com.ludo.enums.TeleportDestination;
import com.ludo.model.Cell;
import com.ludo.model.Piece;
import com.ludo.model.Player;

import java.util.List;

/**
 * View interface for reporting game events.
 * Adheres to Dependency Inversion Principle (DIP) and Single Responsibility
 * Principle (SRP)
 * to decouple game logic from output formatting.
 */
public interface GameView {

    /**
     * Outputs the banner shown once when the simulation begins.
     */
    void displaySimulationBanner();

    /**
     * Outputs the banner shown at the start of each round.
     */
    void displayRoundBanner(int roundNumber);

    /**
     * Outputs a message when the simulation is forcibly terminated
     * after exceeding the maximum round safety limit.
     */
    void displaySimulationTerminated(int maxRounds);

    /**
     * Outputs initial piece information before game begins.
     * Format: "The [color] player has four (04) pieces named R1, R2, R3, and R4."
     */
    void displayBeforeGameBegins(List<Player> players);

    /**
     * Outputs each player's roll when determining who goes first.
     * Format: "[COLOR] rolls <value>"
     */
    void displayInitialRoll(Player player, int rollValue);

    /**
     * Outputs the first player chosen and the clockwise round order.
     * Format:
     * "[COLOR] player has the highest roll and will begin the game."
     * "The order of a single round is [COLOR *a], [COLOR *a + left], [COLOR *a +
     * 2left], and [COLOR *a + 3left]."
     */
    void displayFirstPlayerChosen(Player firstPlayer, List<Player> roundOrder);

    /**
     * Outputs message when a piece moves from Base to starting square 'X' (Section
     * 3.1).
     * Format:
     * "[Color X] player moves piece X[Name] to the starting point."
     * "[Color X] player now has [Number]/4 on pieces on the board and [Number]/4
     * pieces on the base."
     */
    void displayBaseToStart(Player player, Piece piece, int boardCount, int baseCount);

    /**
     * Outputs message for coin toss determining piece direction (Rule T-1).
     */
    void displayCoinToss(Piece piece, com.ludo.enums.CoinSide toss, com.ludo.enums.Direction direction);

    /**
     * Outputs roll message during gameplay (Section 3.1).
     * Format: "[Color X] player rolled [value]."
     */
    void displayPlayerRoll(Player player, int rollValue);

    /**
     * Outputs bonus roll earned message (Rule 4 / Rule T-2).
     */
    void displayBonusAttempt(Player player, String reason);

    /**
     * Outputs three consecutive sixes penalty message (Rule 4).
     */
    void displayThreeSixesPenalty(Player player);

    /**
     * Outputs piece standard movement message (Section 3.1).
     * Format: "[Color X] moves piece X from location L1 to L2 by [value] units in
     * [clockwise/counter-clockwise] direction."
     */
    void displayPieceMoved(Player player, Piece piece, Cell fromCell, Cell toCell, int rollValue,
            com.ludo.enums.Direction direction);

    /**
     * Outputs message when a piece captures an opponent piece (Section 3.1).
     * Format:
     * "[Color X] piece [Name] lands on square L1, captures [Color Y] piece [Name],
     * and returns it to the base."
     * "[Color X] player now has [Number]/4 on pieces on the board and [Number]/4
     * pieces on the base."
     */
    void displayCapture(Player capturingPlayer, Piece capturingPiece, Cell square, Player opponentPlayer,
            Piece capturedPiece);

    /**
     * Outputs message when a piece lands on friendly occupied cell to form/join a
     * block (Rule T-3).
     */
    void displayBlockCreated(Player player, Piece piece, Cell blockCell, List<Piece> blockPieces);

    /**
     * Outputs message when an opponent piece is blocked by a block (Section 3.1).
     * Format: "[Color X] piece [Name] is blocked from moving from L1 to L2 by
     * [Color Y] piece [Name]."
     */
    void displayPieceBlocked(Piece piece, Cell fromCell, Cell intendedCell, Piece blockingPiece);

    /**
     * Outputs message when a blocked piece moves up to the cell before the block
     * (Section 3.1).
     * Format:
     * "[Color X] does not have other pieces in the board to move instead of the
     * blocked piece."
     * "Moved the piece to square L3 which is the cell before the block."
     */
    void displayBlockedMoveAdvance(Player player, Piece piece, Cell toCell);

    /**
     * Outputs message when a blocked piece cannot advance and roll is ignored
     * (Section 3.1).
     * Format:
     * "[Color X] does not have other pieces in the board to move instead of the
     * blocked piece."
     * "Ignoring the throw and moving on to the next player."
     */
    void displayBlockedCannotMove(Player player);

    /**
     * Outputs message when a block moves as a unit (Rule T-4).
     */
    void displayBlockMoved(Player player, List<Piece> pieces, Cell fromCell, Cell toCell, int rollValue, int steps,
            com.ludo.enums.Direction direction);

    /**
     * Outputs message when a player rolls three consecutive 6s with a blockade,
     * breaking it (Rule T-6 / Step 10).
     */
    void displayBlockadeBrokenByThreeSixes(Player player, Cell blockCell, List<Piece> movingPieces,
            Piece remainingPiece);

    /**
     * Outputs message indicating that the dice is passed to the next player.
     */
    void displayDicePassedToNextPlayer(Player player);

    /**
     * Outputs message when a blockade captures an opponent blockade of the same
     * size (Rule T-8 / Step 11).
     */
    void displayBlockadeCaptured(Player capturingPlayer, List<Piece> capturingPieces, Cell cell, Player opponentPlayer,
            List<Piece> capturedPieces);

    /**
     * Outputs message when a mystery cell spawns on the board (Rule T-10, Section
     * 3.1).
     * Format: "A mystery cell has spawned in location L1 and will be at this
     * location for the next four rounds."
     */
    void displayMysteryCellSpawned(Cell cell, int rounds);

    /**
     * Outputs status of the mystery cell after each round (Section 3.1).
     * Format: "The mystery cell is at L1 and will be at that location for the next
     * <N> values."
     */
    void displayMysteryCellStatus(Cell cell, int roundsRemaining);

    /**
     * Outputs player piece locations and mystery cell status after each round
     * (Section 3.1).
     */
    void displayRoundStatus(List<Player> players, Cell mysteryCell, int mysteryRoundsRemaining);

    /**
     * Outputs messages when a piece lands on a mystery cell and is teleported (Rule
     * T-11, Section 3.1).
     * Format:
     * "[Color X] player lands on a mystery cell and is teleported to <location>."
     * "[Color X] piece [name] teleported to <location>."
     */
    void displayMysteryCellTeleport(Player player, Piece piece, TeleportDestination destination, Cell destinationCell);

    /**
     * Outputs messages when an entire block lands on a mystery cell and is
     * teleported together (Rule T-11, Section 3.1).
     * Format:
     * "[Color X] player lands on a mystery cell and is teleported to <location>."
     * "[Color X] block [Name1, Name2] teleported to <location>."
     */
    void displayMysteryCellBlockTeleport(Player player, List<Piece> pieces, TeleportDestination destination,
            Cell destinationCell);

    /**
     * Outputs message when a piece receives an aura effect at Alpha cell (Rule
     * T-12, Section 3.1).
     * Format:
     * "[Color X] piece [name] feels energized, and movement speed doubles." OR
     * "[Color X] piece [name] feels sick, and movement speed halves."
     */
    void displayPieceAlphaEffect(Player player, Piece piece, boolean isEnergized);

    /**
     * Outputs message when a block receives an aura effect at Alpha cell (Rule
     * T-12, Section 3.1).
     * Format:
     * "[Color X] block [name1, name2] feels energized, and movement speed doubles."
     * OR
     * "[Color X] block [name1, name2] feels sick, and movement speed halves."
     */
    void displayBlockAlphaEffect(Player player, List<Piece> pieces, boolean isEnergized);

    /**
     * Outputs message when a piece receives Beta briefing effect (Rule T-13,
     * Section 3.1).
     * Format: "[Color X] piece [name] attends briefing and cannot move for four
     * rounds."
     */
    void displayPieceBetaEffect(Player player, Piece piece);

    /**
     * Outputs message when a block receives Beta briefing effect (Rule T-13,
     * Section 3.1).
     * Format: "[Color X] block [name1, name2] attends briefing and cannot move for
     * four rounds."
     */
    void displayBlockBetaEffect(Player player, List<Piece> pieces);

    /**
     * Outputs message when a movement-restricted piece at Beta is teleported to
     * Base after rolling three 3s consecutively (Rule T-13, Section 3.1).
     * Format: "[Color X] piece [name] is movement-restricted and has rolled three
     * consecutively. Teleporting piece [name] to base."
     */
    void displayPieceBetaTeleportToBase(Player player, Piece piece);

    /**
     * Outputs message when a movement-restricted block at Beta is teleported to
     * Base after rolling three 3s consecutively (Rule T-13, Section 3.1).
     * Format: "[Color X] block [name1, name2] is movement-restricted and has rolled
     * three consecutively. Teleporting block [name1, name2] to base."
     */
    void displayBlockBetaTeleportToBase(Player player, List<Piece> pieces);

    /**
     * Outputs message when a piece moving clockwise has changed direction to
     * counterclockwise upon teleporting to Gamma (Rule T-14, Section 3.1).
     * Format: "The [Color X] piece [name], which was moving clockwise, has changed
     * to moving counterclockwise."
     */
    void displayPieceGammaDirectionChange(Player player, Piece piece);

    /**
     * Outputs message when a block moving clockwise has changed direction to
     * counterclockwise upon teleporting to Gamma (Rule T-14, Section 3.1).
     * Format: "The [Color X] block [name1, name2], which was moving clockwise, has
     * changed to moving counterclockwise."
     */
    void displayBlockGammaDirectionChange(Player player, List<Piece> pieces);

    /**
     * Outputs message when a piece moving counterclockwise is teleported to Beta
     * from Gamma (Rule T-14, Section 3.1).
     * Format: "The [Color X] piece [name] is moving in a counterclockwise
     * direction. Teleporting to Beta from Gamma."
     */
    void displayPieceGammaTeleportToBeta(Player player, Piece piece);

    /**
     * Outputs message when a block moving counterclockwise is teleported to Beta
     * from Gamma (Rule T-14, Section 3.1).
     * Format: "The [Color X] block [name1, name2] is moving in a counterclockwise
     * direction. Teleporting to Beta from Gamma."
     */
    void displayBlockGammaTeleportToBeta(Player player, List<Piece> pieces);

    /**
     * Outputs a message when a player wins the game.
     */
    void displayGameWon(Player player);
}
