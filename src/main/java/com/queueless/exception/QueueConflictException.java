package com.queueless.exception;

public class QueueConflictException extends RuntimeException {
    public QueueConflictException(String message) {
        super(message);
    }
}
