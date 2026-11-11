package com.gregisoft.explorer.cli.garmin;

@FunctionalInterface
public interface GarminActivityDownloader {

    byte[] download(String bearerToken, GarminActivityId activityId);
}
