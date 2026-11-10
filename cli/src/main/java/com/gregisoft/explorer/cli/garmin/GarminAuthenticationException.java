package com.gregisoft.explorer.cli.garmin;

public class GarminAuthenticationException extends RuntimeException {

    public GarminAuthenticationException(String message) {
        super(message);
    }

    public GarminAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
