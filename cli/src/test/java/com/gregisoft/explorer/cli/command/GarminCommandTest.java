package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.ExplorerCli;
import com.gregisoft.explorer.cli.garmin.CredentialStore;
import com.gregisoft.explorer.cli.garmin.GarminAccessToken;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticationException;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticator;
import com.gregisoft.explorer.cli.garmin.GarminActivityArchive;
import com.gregisoft.explorer.cli.garmin.GarminFitFixtures;
import com.gregisoft.explorer.cli.garmin.FitToGpxConverter;
import com.gregisoft.explorer.cli.garmin.SecretReaderException;
import com.gregisoft.explorer.cli.http.ExplorerBackendClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GarminCommandTest {

    @TempDir
    Path tempDirectory;

    private HttpServer server;

    private static final String API_KEY = "garmin-secret-key";
    private static final String ACCESS_TOKEN = "garmin-secret-token";
    private static final GarminAuthenticator SUCCESSFUL_AUTHENTICATOR = key ->
            new GarminAccessToken(ACCESS_TOKEN, Instant.parse("2026-11-10T10:00:00Z"));

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

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
        assertTrue(garminHelp.out().contains("import"));
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

    @Test
    void garminImportCreatesDraftThroughExistingBackendWorkflow() throws Exception {
        byte[] fit = GarminFitFixtures.activity(tempDirectory, true, 1);
        byte[] archiveBytes = GarminFitFixtures.zip("activity.fit", fit);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/management/imports/gpx", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("\"sourceProvider\":\"GARMIN_CONNECT\""));
            assertTrue(body.contains("\"sourceActivityId\":\"123456789\""));
            byte[] response = "{\"tourId\":91,\"status\":\"DRAFT\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        ExplorerBackendClient backendClient = new ExplorerBackendClient();
        GarminImportCommand command = new GarminImportCommand(
                new FakeCredentialStore(API_KEY),
                SUCCESSFUL_AUTHENTICATOR,
                (token, activityId) -> archiveBytes,
                backendClient,
                new GarminActivityArchive(),
                new FitToGpxConverter(),
                null
        );

        Execution execution = execute(
                command,
                "https://connect.garmin.com/modern/activity/123456789",
                "--backend-url",
                "http://127.0.0.1:" + server.getAddress().getPort()
        );

        assertEquals(0, execution.exitCode());
        assertTrue(execution.out().contains("Created tour 91 with status DRAFT"));
        assertNoSecrets(execution);
    }

    @Test
    void duplicateGarminImportStopsBeforeDownload() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/management/imports/gpx", exchange -> {
            byte[] response = "{\"tourId\":45,\"status\":\"DRAFT\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        boolean[] downloaded = {false};
        ExplorerBackendClient backendClient = new ExplorerBackendClient();
        GarminImportCommand command = new GarminImportCommand(
                new FakeCredentialStore(API_KEY),
                SUCCESSFUL_AUTHENTICATOR,
                (token, activityId) -> {
                    downloaded[0] = true;
                    return new byte[0];
                },
                backendClient,
                new GarminActivityArchive(),
                new FitToGpxConverter(),
                null
        );

        Execution execution = execute(command, "123456789", "--backend-url",
                "http://127.0.0.1:" + server.getAddress().getPort());

        assertEquals(0, execution.exitCode());
        assertTrue(execution.out().contains("already imported as Explorer tour 45"));
        assertFalse(downloaded[0]);
        assertNoSecrets(execution);
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
