package com.hotelos.reception.exception;

public class ServiceUnavailableException extends RuntimeException {
    private final String code;

    public ServiceUnavailableException(String message) {
        super(message);
        this.code = "SERVICE_UNAVAILABLE";
    }

    public ServiceUnavailableException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceUnavailableException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
