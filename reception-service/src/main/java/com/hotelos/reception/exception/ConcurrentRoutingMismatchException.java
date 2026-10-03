package com.hotelos.reception.exception;

public class ConcurrentRoutingMismatchException extends RuntimeException {
    public ConcurrentRoutingMismatchException(String message) {
        super(message);
    }
}
