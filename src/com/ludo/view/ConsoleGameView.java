package com.ludo.view;

import com.ludo.enums.TeleportDestination;
import com.ludo.model.Cell;
import com.ludo.model.Piece;
import com.ludo.model.Player;

import java.util.List;

/**
 * Console implementation of GameView.
 * Responsible solely for formatting and printing game messages
 * according to the exact specifications in Section 3.1.
 */
public class ConsoleGameView implements GameView {

    @Override
    public void displaySimulationBanner() {
        System.out.println("\n==================================================");
        System.out.println("               GAME SIMULATION STARTED              ");
        System.out.println("==================================================");
    }

    @Override
    public void displayRoundBanner(int roundNumber) {
        System.out.println("\n==================================================");
        System.out.println("                   ROUND " + roundNumber);
        System.out.println("==================================================\n");
    }

    @Override
    public void displaySimulationTerminated(int maxRounds) {
        System.out.println("\n==================================================");
        System.out.println("SIMULATION TERMINATED: Maximum round limit of "
                + maxRounds + " reached.");
        System.out.println("==================================================\n");
    }

    @Override
    public void displayBeforeGameBegins(List<Player> players) {
        System.out.println("Before Game Begins :");
        for (Player player : players) {
            List<Piece> pieces = player.getPieces();
            String namesFormatted = formatPieceNames(pieces);
            System.out.println("The " + player.getColor().getDisplayName().toLowerCase()
                    + " player has four (04) pieces named " + namesFormatted + ".");
        }
        System.out.println();
    }

    @Override
    public void displayInitialRoll(Player player, int rollValue) {
        System.out.println("[" + player.getColor().name() + "] rolls " + rollValue);
    }

    @Override
    public void displayFirstPlayerChosen(Player firstPlayer, List<Player> roundOrder) {
        System.out.println(
                "[" + firstPlayer.getColor().name() + "] player has the highest roll and will begin the game.");

        if (roundOrder.size() == 4) {
            System.out.println("The order of a single round is ["
                    + roundOrder.get(0).getColor().name() + "], ["
                    + roundOrder.get(1).getColor().name() + "], ["
                    + roundOrder.get(2).getColor().name() + "], and ["
                    + roundOrder.get(3).getColor().name() + "].\n");
        }
    }

    @Override
    public void displayBaseToStart(Player player, Piece piece, int boardCount, int baseCount) {
        System.out.println("[" + player.getColor().getDisplayName() + "] player moves piece "
                + piece.getName() + " to the starting point.");
        System.out.println("[" + player.getColor().getDisplayName() + "] player now has "
                + boardCount + "/4 on pieces on the board and "
                + baseCount + "/4 pieces on the base.");
    }

    @Override
    public void displayCoinToss(Piece piece, com.ludo.enums.CoinSide toss, com.ludo.enums.Direction direction) {
        System.out.println("[COIN TOSS] Coin toss for " + piece.getName() + ": "
                + toss.name() + " (" + toss.getDisplayName() + ") -> Movement direction: "
                + direction.getDisplayName().toUpperCase() + ".");
    }

    @Override
    public void displayPlayerRoll(Player player, int rollValue) {
        System.out.println("[" + player.getColor().getDisplayName() + "] player rolled " + rollValue + ".");
    }

    @Override
    public void displayBonusAttempt(Player player, String reason) {
        System.out.println("[BONUS ROLL] [" + player.getColor().getDisplayName()
                + "] earns another roll attempt (" + reason + ")!");
    }

    @Override
    public void displayThreeSixesPenalty(Player player) {
        System.out.println("[" + player.getColor().getDisplayName()
                + "] rolled six (6) on the 3rd attempt! Roll is ignored and dice passes to next player.");
    }

    @Override
    public void displayPieceMoved(Player player, Piece piece, Cell fromCell, Cell toCell, int rollValue,
            com.ludo.enums.Direction direction) {
        String fromId = (fromCell == null) ? "Base" : fromCell.getId();
        String toId = (toCell == null) ? "Home" : toCell.getId();
        System.out.println("[" + player.getColor().getDisplayName() + "] moves piece "
                + piece.getName() + " from location " + fromId + " to " + toId
                + " by " + rollValue + " units in " + direction.getDisplayName() + " direction.");
    }

    @Override
    public void displayCapture(Player capturingPlayer, Piece piece, Cell square, Player opponentPlayer,
            Piece capturedPiece) {
        System.out.println("[" + capturingPlayer.getColor().getDisplayName() + "] piece "
                + piece.getName() + " lands on square " + square.getId()
                + ", captures [" + opponentPlayer.getColor().getDisplayName()
                + "] piece " + capturedPiece.getName() + ", and returns it to the base.");
        System.out.println("[" + opponentPlayer.getColor().getDisplayName() + "] player now has "
                + opponentPlayer.getPiecesOnBoard().size() + "/4 on pieces on the board and "
                + opponentPlayer.getPiecesInBase().size() + "/4 pieces on the base.");
    }

    @Override
    public void displayBlockCreated(Player player, Piece piece, Cell blockCell, List<Piece> blockPieces) {
        String otherPieces = blockPieces.stream()
                .filter(p -> p != piece)
                .map(Piece::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        System.out.println("[" + player.getColor().getDisplayName() + "] piece "
                + piece.getName() + " lands on square " + blockCell.getId()
                + ", forming a block with piece " + otherPieces + ".");
    }

    @Override
    public void displayPieceBlocked(Piece piece, Cell fromCell, Cell intendedCell, Piece blockingPiece) {
        String fromId = (fromCell == null) ? "Base" : fromCell.getId();
        String intendedId = (intendedCell == null) ? "Home" : intendedCell.getId();

        List<Piece> blockPieces = new java.util.ArrayList<>();
        Cell blockingCell = blockingPiece.getCurrentCell();
        if (blockingCell != null) {
            for (com.ludo.enums.PlayerColor c : com.ludo.enums.PlayerColor.values()) {
                List<Piece> piecesOfColor = blockingCell.getOccupyingPieces().stream()
                        .filter(p -> p.getColor() == c)
                        .toList();
                if (piecesOfColor.size() >= 2) {
                    blockPieces = piecesOfColor;
                    break;
                }
            }
        }

        if (!blockPieces.isEmpty()) {
            String blockNames = blockPieces.stream().map(Piece::getName)
                    .collect(java.util.stream.Collectors.joining(" "));
            System.out.println("[" + piece.getColor().getDisplayName() + "] piece "
                    + piece.getName() + " is blocked from moving from " + fromId
                    + " to " + intendedId + " by the block [" + blockNames + "]");
        } else {
            System.out.println("[" + piece.getColor().getDisplayName() + "] piece "
                    + piece.getName() + " is blocked from moving from " + fromId
                    + " to " + intendedId + " by [" + blockingPiece.getColor().getDisplayName()
                    + "] piece " + blockingPiece.getName() + ".");
        }
    }

    @Override
    public void displayBlockedMoveAdvance(Player player, Piece piece, Cell toCell) {
        System.out.println("[" + player.getColor().getDisplayName()
                + "] does not have other pieces in the board to move instead of the blocked piece.");
        System.out.println("Moved the piece to square " + toCell.getId()
                + " which is the cell before the block.");
    }

    @Override
    public void displayBlockedCannotMove(Player player) {
        System.out.println("[" + player.getColor().getDisplayName()
                + "] does not have other pieces in the board to move instead of the blocked piece.");
        System.out.println("Ignoring the throw and moving on to the next player.");
    }

    @Override
    public void displayBlockMoved(Player player, List<Piece> pieces, Cell fromCell, Cell toCell, int rollValue,
            int steps, com.ludo.enums.Direction direction) {
        String names = pieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        boolean isEnergized = pieces.stream().allMatch(p -> p.getState() instanceof com.ludo.state.EnergizedState);
        boolean isSick = pieces.stream().allMatch(p -> p.getState() instanceof com.ludo.state.SickState);
        String details = "roll " + rollValue + " / " + pieces.size() + " pieces";
        if (isEnergized) {
            details += " * 2 [Energized]";
        } else if (isSick) {
            details += " / 2 [Sick]";
        }
        System.out.println("[" + player.getColor().getDisplayName() + "] moves block [" + names
                + "] from location " + fromCell.getId() + " to " + toCell.getId()
                + " by " + steps + " units (" + details + ") in "
                + direction.getDisplayName() + " direction.");
    }

    @Override
    public void displayBlockadeBrokenByThreeSixes(Player player, Cell blockCell, List<Piece> movingPieces,
            Piece remainingPiece) {
        String movingNames = movingPieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        System.out.println("[" + player.getColor().getDisplayName()
                + "] rolled three (3) consecutive sixes with a blockade at square " + blockCell.getId() + "!");
        System.out.println("Breaking blockade: Piece " + remainingPiece.getName() + " remains at square "
                + blockCell.getId() + ", while piece(s) [" + movingNames
                + "] move exactly 6 cells in their original direction.");
    }

    @Override
    public void displayDicePassedToNextPlayer(Player player) {
        System.out.println("Dice passes to the next player.");
    }

    @Override
    public void displayBlockadeCaptured(Player capturingPlayer, List<Piece> capturingPieces, Cell cell,
            Player opponentPlayer, List<Piece> capturedPieces) {
        String capturingNames = capturingPieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        String capturedNames = capturedPieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        System.out.println("[" + capturingPlayer.getColor().getDisplayName() + "] block [" + capturingNames
                + "] lands on square " + cell.getId() + ", captures [" + opponentPlayer.getColor().getDisplayName()
                + "] block [" + capturedNames + "], and returns all captured pieces to the base.");
        int boardCount = capturingPlayer.getPiecesOnBoard().size();
        int baseCount = capturingPlayer.getPiecesInBase().size();
        System.out.println("[" + capturingPlayer.getColor().getDisplayName() + "] player now has "
                + boardCount + "/4 on pieces on the board and " + baseCount + "/4 pieces on the base.");
    }

    @Override
    public void displayMysteryCellSpawned(Cell cell, int rounds) {
        String roundsWord = (rounds == 4) ? "four" : String.valueOf(rounds);
        System.out.println("A mystery cell has spawned in location " + cell.getId()
                + " and will be at this location for the next " + roundsWord + " rounds.");
    }

    @Override
    public void displayMysteryCellStatus(Cell cell, int roundsRemaining) {
        System.out.println("The mystery cell is at " + cell.getId()
                + " and will be at that location for the next " + roundsRemaining + " values.");
    }

    @Override
    public void displayRoundStatus(List<Player> players, Cell mysteryCell, int mysteryRoundsRemaining) {
        for (Player player : players) {
            int boardCount = player.getPiecesOnBoard().size();
            int baseCount = player.getPiecesInBase().size();
            System.out.println("[" + player.getColor().getDisplayName() + "] player now has "
                    + boardCount + "/4 on pieces on the board and " + baseCount + "/4 pieces on the base.");
            System.out.println("============================");
            System.out.println("Location of pieces [" + player.getColor().getDisplayName().toLowerCase() + "]");
            System.out.println("============================");
            for (Piece piece : player.getPieces()) {
                String loc = piece.isInBase() ? "Base" : (piece.isInHome() ? "Home" : piece.getCurrentCell().getId());
                System.out.println("Piece " + piece.getName() + " -> " + loc + " (Captured " + piece.getCaptureCount()
                        + " pieces)");
            }
        }
        if (mysteryCell != null) {
            displayMysteryCellStatus(mysteryCell, mysteryRoundsRemaining);
        }
    }

    @Override
    public void displayMysteryCellTeleport(Player player, Piece piece, TeleportDestination destination,
            Cell destinationCell) {
        String destName = destination.getDisplayName();
        System.out.println("[" + player.getColor().getDisplayName()
                + "] player lands on a mystery cell and is teleported to " + destName + ".");
        System.out.println("[" + player.getColor().getDisplayName() + "] piece " + piece.getName() + " teleported to "
                + destName + ".");
    }

    @Override
    public void displayMysteryCellBlockTeleport(Player player, List<Piece> pieces, TeleportDestination destination,
            Cell destinationCell) {
        String destName = destination.getDisplayName();
        String names = pieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        System.out.println("[" + player.getColor().getDisplayName()
                + "] player lands on a mystery cell and is teleported to " + destName + ".");
        System.out.println(
                "[" + player.getColor().getDisplayName() + "] block [" + names + "] teleported to " + destName + ".");
    }

    @Override
    public void displayPieceAlphaEffect(Player player, Piece piece, boolean isEnergized) {
        if (isEnergized) {
            System.out.println("[" + player.getColor().getDisplayName() + "] piece "
                    + piece.getName() + " feels energized, and movement speed doubles.");
        } else {
            System.out.println("[" + player.getColor().getDisplayName() + "] piece "
                    + piece.getName() + " feels sick, and movement speed halves.");
        }
    }

    @Override
    public void displayBlockAlphaEffect(Player player, List<Piece> pieces, boolean isEnergized) {
        String names = pieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        if (isEnergized) {
            System.out.println("[" + player.getColor().getDisplayName() + "] block ["
                    + names + "] feels energized, and movement speed doubles.");
        } else {
            System.out.println("[" + player.getColor().getDisplayName() + "] block ["
                    + names + "] feels sick, and movement speed halves.");
        }
    }

    @Override
    public void displayPieceBetaEffect(Player player, Piece piece) {
        System.out.println("[" + player.getColor().getDisplayName() + "] piece "
                + piece.getName() + " attends briefing and cannot move for four rounds.");
    }

    @Override
    public void displayBlockBetaEffect(Player player, List<Piece> pieces) {
        String names = pieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        System.out.println("[" + player.getColor().getDisplayName() + "] block ["
                + names + "] attends briefing and cannot move for four rounds.");
    }

    @Override
    public void displayPieceBetaTeleportToBase(Player player, Piece piece) {
        System.out.println("[" + player.getColor().getDisplayName() + "] piece "
                + piece.getName() + " is movement-restricted and has rolled three consecutively. Teleporting piece "
                + piece.getName() + " to base.");
    }

    @Override
    public void displayBlockBetaTeleportToBase(Player player, List<Piece> pieces) {
        String names = pieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        System.out.println("[" + player.getColor().getDisplayName() + "] block ["
                + names + "] is movement-restricted and has rolled three consecutively. Teleporting block ["
                + names + "] to base.");
    }

    @Override
    public void displayPieceGammaDirectionChange(Player player, Piece piece) {
        System.out.println("The [" + player.getColor().getDisplayName() + "] piece "
                + piece.getName() + ", which was moving clockwise, has changed to moving counterclockwise.");
    }

    @Override
    public void displayBlockGammaDirectionChange(Player player, List<Piece> pieces) {
        String names = pieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        System.out.println("The [" + player.getColor().getDisplayName() + "] block ["
                + names + "], which was moving clockwise, has changed to moving counterclockwise.");
    }

    @Override
    public void displayPieceGammaTeleportToBeta(Player player, Piece piece) {
        System.out.println("The [" + player.getColor().getDisplayName() + "] piece "
                + piece.getName() + " is moving in a counterclockwise direction. Teleporting to Beta from Gamma.");
    }

    @Override
    public void displayBlockGammaTeleportToBeta(Player player, List<Piece> pieces) {
        String names = pieces.stream().map(Piece::getName).reduce((a, b) -> a + ", " + b).orElse("");
        System.out.println("The [" + player.getColor().getDisplayName() + "] block ["
                + names + "] is moving in a counterclockwise direction. Teleporting to Beta from Gamma.");
    }

    /**
     * Formats piece names as: "R1, R2, R3, and R4"
     */
    private String formatPieceNames(List<Piece> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            return "";
        }
        if (pieces.size() == 1) {
            return pieces.get(0).getName();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            if (i == pieces.size() - 1) {
                sb.append("and ").append(pieces.get(i).getName());
            } else {
                sb.append(pieces.get(i).getName()).append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public void displayGameWon(Player player) {
        System.out.println("\n**************************************************");
        System.out.println("                   GAME OVER!                     ");
        System.out.println("**************************************************");
        System.out.println("Player [" + player.getName() + "] has successfully brought all 4 pieces home!");
        System.out.println("Player [" + player.getName() + "] WINS THE GAME!");
        System.out.println("**************************************************\n");
    }

}
