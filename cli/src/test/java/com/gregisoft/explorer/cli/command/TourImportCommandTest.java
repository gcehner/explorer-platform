package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.ExplorerCli;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TourImportCommandTest {

    @TempDir
    Path tempDirectory;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void successfulImportReturnsZeroAndPrintsCreatedTour() throws Exception {
        startServer(201, "{\"tourId\":17,\"status\":\"DRAFT\"}");
        Execution execution = execute("tour", "import", validGpx().toString(),
                "--backend-url", baseUrl());

        assertEquals(0, execution.exitCode());
        assertTrue(execution.out().contains("Created tour 17 with status DRAFT."));
        assertEquals("", execution.err());
    }

    @Test
    void processingAndBackendFailuresReturnOneWithoutStackTrace() throws Exception {
        Path invalid = Files.writeString(tempDirectory.resolve("invalid.gpx"), "<gpx>");
        Execution processing = execute("tour", "import", invalid.toString());

        assertEquals(1, processing.exitCode());
        assertTrue(processing.err().startsWith("GPX import failed:"));
        assertTrue(!processing.err().contains("at com.gregisoft"));

        startServer(422, "{\"message\":\"bad normalized data\"}");
        Execution backend = execute("tour", "import", validGpx().toString(),
                "--backend-url", baseUrl());
        assertEquals(1, backend.exitCode());
        assertTrue(backend.err().contains("HTTP 422: bad normalized data"));
    }

    @Test
    void missingRequiredArgumentReturnsPicocliUsageExitCodeTwo() {
        Execution execution = execute("tour", "import");

        assertEquals(2, execution.exitCode());
        assertTrue(execution.err().contains("Missing required parameter"));
    }

    @Test
    void resolvesBackendUrlUsingApprovedPrecedence() {
        assertEquals(URI.create("http://option:8080"),
                TourImportCommand.resolveBackendUrl("http://option:8080", "http://environment:8080"));
        assertEquals(URI.create("http://environment:8080"),
                TourImportCommand.resolveBackendUrl(null, "http://environment:8080"));
        assertEquals(URI.create("http://localhost:8080"),
                TourImportCommand.resolveBackendUrl(null, null));
    }

    private Execution execute(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine commandLine = new CommandLine(new ExplorerCli());
        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));
        int exitCode = commandLine.execute(args);
        return new Execution(exitCode, out.toString(), err.toString());
    }

    private void startServer(int status, String responseBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/management/imports/gpx", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private Path validGpx() throws Exception {
        return Files.writeString(tempDirectory.resolve("valid.gpx"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><trkseg>
                    <trkpt lat="46.0" lon="14.0"><ele>100</ele><time>2025-07-12T10:00:00Z</time></trkpt>
                    <trkpt lat="46.0" lon="14.001"><ele>108</ele><time>2025-07-12T10:01:00Z</time></trkpt>
                  </trkseg></trk>
                </gpx>
                """);
    }

    private record Execution(int exitCode, String out, String err) {
    }
}
