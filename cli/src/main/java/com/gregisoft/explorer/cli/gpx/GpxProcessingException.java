package com.gregisoft.explorer.cli.gpx;

public class GpxProcessingException extends RuntimeException {

    public GpxProcessingException(String message) {
        super(message);
    }

    public GpxProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
