package com.hotelos.roomservice.exception;

public class NoActiveStayException extends RuntimeException {
    public NoActiveStayException(String message) {
        super(message);
    }
}
