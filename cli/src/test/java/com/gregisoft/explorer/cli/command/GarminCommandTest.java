package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.ExplorerCli;
import com.gregisoft.explorer.cli.garmin.CredentialStore;
import com.gregisoft.explorer.cli.garmin.GarminAccessToken;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticationException;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticator;
import com.gregisoft.explorer.cli.garmin.SecretReaderException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GarminCommandTest {

    private static final String API_KEY = "garmin-secret-key";
    private static final String ACCESS_TOKEN = "garmin-secret-token";
    private static final GarminAuthenticator SUCCESSFUL_AUTHENTICATOR = key ->
            new GarminAccessToken(ACCESS_TOKEN, Instant.parse("2026-11-10T10:00:00Z"));

    @Test
    void commandHierarchyAndHelpAreRegistered() {
        Execution rootHelp = execute(new ExplorerCli(), "--help");
        Execution garminHelp = execute(new ExplorerCli(), "garmin", "--help");

        assertEquals(0, rootHelp.exitCode());
        assertTrue(rootHelp.out().contains("garmin"));
        assertEquals(0, garminHelp.exitCode());
        assertTrue(garminHelp.out().contains("configure"));
        assertTrue(garminHelp.out().contains("status"));
        assertTrue(garminHelp.out().contains("logout"));
    }

    @Test
    void configureValidatesBeforeStoringAndDoesNotExposeSecrets() {
        FakeCredentialStore store = new FakeCredentialStore();
        GarminConfigureCommand command = new GarminConfigureCommand(
                prompt -> API_KEY.toCharArray(), store, SUCCESSFUL_AUTHENTICATOR);

        Execution execution = execute(command);

        assertEquals(0, execution.exitCode());
        assertArrayEquals(API_KEY.toCharArray(), store.storedCopy);
        assertTrue(execution.out().contains("Windows Credential Manager"));
        assertNoSecrets(execution);
    }

    @Test
    void configureDoesNotStoreInvalidKeyAndHandlesNoninteractiveInput() {
        FakeCredentialStore invalidStore = new FakeCredentialStore();
        GarminConfigureCommand invalidCommand = new GarminConfigureCommand(
                prompt -> API_KEY.toCharArray(), invalidStore,
                key -> { throw new GarminAuthenticationException("Garmin rejected the key."); });

        Execution invalid = execute(invalidCommand);
        assertEquals(1, invalid.exitCode());
        assertEquals(0, invalidStore.storeCalls);
        assertNoSecrets(invalid);

        GarminConfigureCommand noninteractiveCommand = new GarminConfigureCommand(
                prompt -> { throw new SecretReaderException("No interactive console is available."); },
                new FakeCredentialStore(), SUCCESSFUL_AUTHENTICATOR);
        Execution noninteractive = execute(noninteractiveCommand);
        assertEquals(1, noninteractive.exitCode());
        assertTrue(noninteractive.err().contains("No interactive console"));
    }

    @Test
    void statusReportsMissingSuccessfulAndFailedAuthentication() {
        Execution missing = execute(new GarminStatusCommand(
                new FakeCredentialStore(), SUCCESSFUL_AUTHENTICATOR));
        assertEquals(1, missing.exitCode());
        assertTrue(missing.out().contains("explorer garmin configure"));

        FakeCredentialStore configured = new FakeCredentialStore(API_KEY);
        Execution successful = execute(new GarminStatusCommand(configured, SUCCESSFUL_AUTHENTICATOR));
        assertEquals(0, successful.exitCode());
        assertTrue(successful.out().contains("authentication succeeds"));
        assertNoSecrets(successful);

        FakeCredentialStore rejected = new FakeCredentialStore(API_KEY);
        Execution failed = execute(new GarminStatusCommand(rejected,
                key -> { throw new GarminAuthenticationException("Authentication failed."); }));
        assertEquals(1, failed.exitCode());
        assertTrue(failed.err().contains("Authentication failed"));
        assertNoSecrets(failed);
    }

    @Test
    void logoutRemovesExistingCredentialAndIsSafeToRepeat() {
        FakeCredentialStore store = new FakeCredentialStore(API_KEY);

        Execution first = execute(new GarminLogoutCommand(store));
        Execution second = execute(new GarminLogoutCommand(store));

        assertEquals(0, first.exitCode());
        assertTrue(first.out().contains("Removed"));
        assertTrue(first.out().contains("does not revoke"));
        assertEquals(0, second.exitCode());
        assertTrue(second.out().contains("No locally stored"));
        assertNoSecrets(first);
        assertNoSecrets(second);
    }

    private static void assertNoSecrets(Execution execution) {
        String output = execution.out() + execution.err();
        assertFalse(output.contains(API_KEY));
        assertFalse(output.contains(ACCESS_TOKEN));
    }

    private static Execution execute(Object command, String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine commandLine = new CommandLine(command);
        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));
        int exitCode = commandLine.execute(args);
        return new Execution(exitCode, out.toString(), err.toString());
    }

    private static final class FakeCredentialStore implements CredentialStore {
        private char[] value;
        private char[] storedCopy;
        private int storeCalls;

        private FakeCredentialStore() {
        }

        private FakeCredentialStore(String value) {
            this.value = value.toCharArray();
        }

        @Override
        public Optional<char[]> read() {
            return value == null ? Optional.empty() : Optional.of(Arrays.copyOf(value, value.length));
        }

        @Override
        public void store(char[] secret) {
            storeCalls++;
            storedCopy = Arrays.copyOf(secret, secret.length);
            value = Arrays.copyOf(secret, secret.length);
        }

        @Override
        public boolean delete() {
            if (value == null) {
                return false;
            }
            Arrays.fill(value, '\0');
            value = null;
            return true;
        }
    }

    private record Execution(int exitCode, String out, String err) {
    }
}
