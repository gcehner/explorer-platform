package com.gregisoft.explorer.cli.garmin;

import java.util.Optional;

public interface CredentialStore {

    Optional<char[]> read();

    void store(char[] secret);

    boolean delete();
}
