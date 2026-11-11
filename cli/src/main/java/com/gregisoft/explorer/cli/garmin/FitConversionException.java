package com.gregisoft.explorer.cli.garmin;

public class FitConversionException extends RuntimeException {

    public FitConversionException(String message) {
        super(message);
    }

    public FitConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
