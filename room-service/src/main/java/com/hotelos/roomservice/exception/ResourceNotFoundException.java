package com.hotelos.roomservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String code;

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = "RESOURCE_NOT_FOUND";
    }

    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
