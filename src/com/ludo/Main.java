package com.ludo;

import com.ludo.engine.GameEngine;
import com.ludo.execptions.GameEngineException;
import com.ludo.factory.GameFactory;
import com.ludo.model.GameState;
import com.ludo.view.ConsoleGameView;
import com.ludo.view.GameView;

public class Main {
        public static void main(String[] args) {
                // UNCOMMENT THIS LINE TO TURN ON TESTING MODE (100% predictable)
                // com.ludo.util.GameRandom.setSeed(12345);

                try {
                        GameState gameState = GameFactory.createGame();
                        GameView view = new ConsoleGameView();
                        GameEngine engine = new GameEngine(gameState, view);
                        engine.setupGame();
                        engine.runSimulation();

                } catch (GameEngineException e) {
                        System.err.println("[ERROR] Game setup or simulation failed: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                        System.err.println("[ERROR] Invalid game configuration: " + e.getMessage());
                } catch (RuntimeException e) {
                        System.err.println("[ERROR] Unexpected error, game cannot continue: " + e.getMessage());
                }
        }
}
