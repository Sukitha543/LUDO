package com.ludo.execptions;

/**
 * Unchecked exception for errors encountered during move generation
 * (null inputs, invalid board state, corrupted piece data, etc.).
 */
public class MoveGeneratorException extends RuntimeException {

    public MoveGeneratorException(String message) {
        super(message);
    }

    public MoveGeneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
