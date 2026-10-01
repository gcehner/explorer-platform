package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.gpx.GpxProcessingException;
import com.gregisoft.explorer.cli.gpx.GpxProcessor;
import com.gregisoft.explorer.cli.gpx.GpxTrackReader;
import com.gregisoft.explorer.cli.gpx.NormalizedGpxData;
import com.gregisoft.explorer.cli.http.BackendClientException;
import com.gregisoft.explorer.cli.http.ExplorerBackendClient;
import com.gregisoft.explorer.cli.http.GpxImportResponse;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Callable;

@Command(
        name = "import",
        description = "Create a DRAFT tour from a GPX track.",
        mixinStandardHelpOptions = true
)
public class TourImportCommand implements Callable<Integer> {

    private static final URI DEFAULT_BACKEND_URL = URI.create("http://localhost:8080");

    @Parameters(index = "0", paramLabel = "<file.gpx>", description = "Original GPX file.")
    private Path gpxFile;

    @Option(names = "--backend-url", description = "Explorer backend base URL.")
    private String backendUrl;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        try {
            validateGpxFile(gpxFile);
            URI resolvedBackendUrl = resolveBackendUrl(backendUrl, System.getenv("EXPLORER_BACKEND_URL"));
            NormalizedGpxData normalized = new GpxProcessor().process(new GpxTrackReader().read(gpxFile));
            GpxImportResponse response = new ExplorerBackendClient().importGpx(
                    resolvedBackendUrl, gpxFile, normalized);
            spec.commandLine().getOut().printf(
                    "Created tour %d with status %s.%n", response.tourId(), response.status());
            return 0;
        } catch (GpxProcessingException | BackendClientException | IllegalArgumentException exception) {
            spec.commandLine().getErr().println("GPX import failed: " + exception.getMessage());
            return 1;
        }
    }

    static URI resolveBackendUrl(String optionValue, String environmentValue) {
        String value = hasText(optionValue)
                ? optionValue
                : hasText(environmentValue) ? environmentValue : DEFAULT_BACKEND_URL.toString();
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Backend URL is invalid: " + value, exception);
        }
        if (!uri.isAbsolute()
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Backend URL must be an absolute HTTP(S) base URL.");
        }
        return uri;
    }

    private static void validateGpxFile(Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("GPX file does not exist or is not readable: " + path);
        }
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gpx")) {
            throw new IllegalArgumentException("GPX input file must have a .gpx extension.");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
