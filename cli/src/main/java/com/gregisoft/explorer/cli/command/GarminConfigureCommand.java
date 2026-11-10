package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.garmin.ConsoleSecretReader;
import com.gregisoft.explorer.cli.garmin.CredentialStore;
import com.gregisoft.explorer.cli.garmin.CredentialStoreException;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticationException;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticator;
import com.gregisoft.explorer.cli.garmin.GarminPersonalApiClient;
import com.gregisoft.explorer.cli.garmin.SecretReader;
import com.gregisoft.explorer.cli.garmin.SecretReaderException;
import com.gregisoft.explorer.cli.garmin.WindowsCredentialStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.Arrays;
import java.util.concurrent.Callable;

@Command(
        name = "configure",
        description = "Validate and securely store a Garmin Personal API key.",
        mixinStandardHelpOptions = true
)
public class GarminConfigureCommand implements Callable<Integer> {

    private final SecretReader secretReader;
    private final CredentialStore credentialStore;
    private final GarminAuthenticator authenticator;

    @Spec
    private CommandSpec spec;

    public GarminConfigureCommand() {
        this(new ConsoleSecretReader(), new WindowsCredentialStore(), new GarminPersonalApiClient());
    }

    GarminConfigureCommand(
            SecretReader secretReader,
            CredentialStore credentialStore,
            GarminAuthenticator authenticator
    ) {
        this.secretReader = secretReader;
        this.credentialStore = credentialStore;
        this.authenticator = authenticator;
    }

    @Override
    public Integer call() {
        spec.commandLine().getOut().println(
                "Create a Personal API key in Garmin Connect Web under Account Settings > Personal API.");
        spec.commandLine().getOut().println(
                "The key is read without echo and is stored only after Garmin accepts it.");

        char[] enteredKey = null;
        char[] apiKey = null;
        try {
            enteredKey = secretReader.readSecret("Garmin Personal API key: ");
            apiKey = trim(enteredKey);
            requireKey(apiKey);
            authenticator.authenticate(apiKey);
            credentialStore.store(apiKey);
            spec.commandLine().getOut().println(
                    "Garmin Personal API access is configured in Windows Credential Manager.");
            return 0;
        } catch (SecretReaderException | CredentialStoreException | GarminAuthenticationException exception) {
            spec.commandLine().getErr().println("Garmin configuration failed: " + exception.getMessage());
            return 1;
        } finally {
            if (apiKey != null) {
                Arrays.fill(apiKey, '\0');
            }
            if (enteredKey != null) {
                Arrays.fill(enteredKey, '\0');
            }
        }
    }

    private static void requireKey(char[] apiKey) {
        if (apiKey == null || apiKey.length == 0 || isBlank(apiKey)) {
            throw new SecretReaderException("The Personal API key must not be empty.");
        }
    }

    private static boolean isBlank(char[] value) {
        for (char character : value) {
            if (!Character.isWhitespace(character)) {
                return false;
            }
        }
        return true;
    }

    private static char[] trim(char[] value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length;
        while (start < end && Character.isWhitespace(value[start])) {
            start++;
        }
        while (end > start && Character.isWhitespace(value[end - 1])) {
            end--;
        }
        return Arrays.copyOfRange(value, start, end);
    }
}
