package com.gregisoft.explorer.cli.garmin;

@FunctionalInterface
public interface GarminAuthenticator {

    GarminAccessToken authenticate(char[] apiKey);
}
