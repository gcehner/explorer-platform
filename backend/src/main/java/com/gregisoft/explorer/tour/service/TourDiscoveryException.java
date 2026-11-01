package com.gregisoft.explorer.tour.service;

public class TourDiscoveryException extends RuntimeException {

    public TourDiscoveryException(String message) {
        super(message);
    }

    public TourDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
