package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.garmin.CredentialStore;
import com.gregisoft.explorer.cli.garmin.CredentialStoreException;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticationException;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticator;
import com.gregisoft.explorer.cli.garmin.GarminPersonalApiClient;
import com.gregisoft.explorer.cli.garmin.WindowsCredentialStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "status",
        description = "Check stored Garmin credentials and authenticate with Garmin.",
        mixinStandardHelpOptions = true
)
public class GarminStatusCommand implements Callable<Integer> {

    private final CredentialStore credentialStore;
    private final GarminAuthenticator authenticator;

    @Spec
    private CommandSpec spec;

    public GarminStatusCommand() {
        this(new WindowsCredentialStore(), new GarminPersonalApiClient());
    }

    GarminStatusCommand(CredentialStore credentialStore, GarminAuthenticator authenticator) {
        this.credentialStore = credentialStore;
        this.authenticator = authenticator;
    }

    @Override
    public Integer call() {
        char[] apiKey = null;
        try {
            Optional<char[]> configuredKey = credentialStore.read();
            if (configuredKey.isEmpty()) {
                spec.commandLine().getOut().println("Garmin Personal API access is not configured.");
                spec.commandLine().getOut().println("Run: explorer garmin configure");
                return 1;
            }
            apiKey = configuredKey.get();
            authenticator.authenticate(apiKey);
            spec.commandLine().getOut().println(
                    "Garmin Personal API key is configured and authentication succeeds.");
            return 0;
        } catch (CredentialStoreException | GarminAuthenticationException exception) {
            spec.commandLine().getErr().println("Garmin status check failed: " + exception.getMessage());
            return 1;
        } finally {
            if (apiKey != null) {
                Arrays.fill(apiKey, '\0');
            }
        }
    }
}
