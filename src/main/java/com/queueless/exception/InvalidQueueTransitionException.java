package com.queueless.exception;

public class InvalidQueueTransitionException extends RuntimeException {
    public InvalidQueueTransitionException(String message) {
        super(message);
    }
}
