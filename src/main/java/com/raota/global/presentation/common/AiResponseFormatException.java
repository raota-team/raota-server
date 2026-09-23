package com.raota.global.presentation.common;

/**
 * Signals that an upstream model response could not be converted to the structured
 * response required by an AI use case.
 */
public class AiResponseFormatException extends RuntimeException {

    public AiResponseFormatException(String message, Throwable cause) {
        super(message, cause);
    }

}
