package com.ludo.state;

import com.ludo.enums.PlayerColor;
import com.ludo.model.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HomeState Unit Tests")
class HomeStateTest {

    private HomeState homeState;
    private Piece piece;

    @BeforeEach
    void setUp() {
        homeState = new HomeState();
        piece = new Piece(PlayerColor.RED, 1);
        piece.setState(homeState);
    }

    @Test
    @DisplayName("Verify state name and string representation")
    void testStateNameAndToString() {
        assertEquals("Home", homeState.getStateName());
        assertEquals("Home", homeState.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify finished piece in HomeState CANNOT move on any roll 1 through 6")
    void testCannotMoveOnAnyRoll(int roll) {
        assertFalse(homeState.canMove(piece, roll),
                "A piece in HomeState is finished and cannot move on roll " + roll);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Verify calculateMovementDistance is always 0")
    void testMovementDistanceIsZero(int roll) {
        assertEquals(0, homeState.calculateMovementDistance(roll),
                "HomeState movement distance must be 0 regardless of roll");
    }

    @Test
    @DisplayName("Verify HomeState does not expire on round end (permanent terminal state)")
    void testDoesNotExpire() {
        assertEquals(-1, homeState.getRemainingRounds());

        for (int i = 0; i < 5; i++) {
            homeState.onRoundEnd(piece);
        }

        assertEquals(-1, homeState.getRemainingRounds());
        assertSame(homeState, piece.getState());
    }
}
