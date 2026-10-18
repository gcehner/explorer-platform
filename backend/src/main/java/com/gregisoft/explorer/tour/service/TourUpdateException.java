package com.gregisoft.explorer.tour.service;

public class TourUpdateException extends RuntimeException {

    private final int status;

    private TourUpdateException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static TourUpdateException badRequest(String message) {
        return new TourUpdateException(400, message);
    }

    public static TourUpdateException conflict(String message) {
        return new TourUpdateException(409, message);
    }

    public static TourUpdateException unprocessable(String message) {
        return new TourUpdateException(422, message);
    }

    public int getStatus() {
        return status;
    }
}
