package com.gregisoft.explorer.cli.garmin;

import java.io.Console;

public final class ConsoleSecretReader implements SecretReader {

    @Override
    public char[] readSecret(String prompt) {
        Console console = System.console();
        if (console == null) {
            throw new SecretReaderException(
                    "No interactive console is available. Run this command directly from a terminal.");
        }
        char[] secret = console.readPassword("%s", prompt);
        if (secret == null) {
            throw new SecretReaderException("Personal API key input was cancelled.");
        }
        return secret;
    }
}
