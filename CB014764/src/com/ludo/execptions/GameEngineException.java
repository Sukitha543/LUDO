package com.ludo.execptions;

/**
 * Unchecked exception for unrecoverable or invalid game-engine states
 * (missing cells, null moves, corrupted board data, etc.).
 */
public class GameEngineException extends RuntimeException {

    public GameEngineException(String message) {
        super(message);
    }

    public GameEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}