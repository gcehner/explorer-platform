package com.gregisoft.explorer.cli.garmin;

@FunctionalInterface
public interface SecretReader {

    char[] readSecret(String prompt);
}
