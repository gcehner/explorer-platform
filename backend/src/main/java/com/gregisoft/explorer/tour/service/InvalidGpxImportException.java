package com.gregisoft.explorer.tour.service;

public class InvalidGpxImportException extends RuntimeException {

    public InvalidGpxImportException(String message) {
        super(message);
    }

    public InvalidGpxImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
