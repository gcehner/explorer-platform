package com.gregisoft.explorer.cli.garmin;

public class GarminActivityDownloadException extends RuntimeException {

    public GarminActivityDownloadException(String message) {
        super(message);
    }

    public GarminActivityDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
