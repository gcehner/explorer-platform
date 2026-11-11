package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.garmin.CredentialStore;
import com.gregisoft.explorer.cli.garmin.CredentialStoreException;
import com.gregisoft.explorer.cli.garmin.FitConversionException;
import com.gregisoft.explorer.cli.garmin.FitToGpxConverter;
import com.gregisoft.explorer.cli.garmin.GarminAccessToken;
import com.gregisoft.explorer.cli.garmin.GarminActivityArchive;
import com.gregisoft.explorer.cli.garmin.GarminActivityDownloadClient;
import com.gregisoft.explorer.cli.garmin.GarminActivityDownloadException;
import com.gregisoft.explorer.cli.garmin.GarminActivityDownloader;
import com.gregisoft.explorer.cli.garmin.GarminActivityId;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticationException;
import com.gregisoft.explorer.cli.garmin.GarminAuthenticator;
import com.gregisoft.explorer.cli.garmin.GarminPersonalApiClient;
import com.gregisoft.explorer.cli.garmin.WindowsCredentialStore;
import com.gregisoft.explorer.cli.gpx.GpxProcessingException;
import com.gregisoft.explorer.cli.http.BackendClientException;
import com.gregisoft.explorer.cli.http.ExplorerBackendClient;
import com.gregisoft.explorer.cli.http.GpxImportOutcome;
import com.gregisoft.explorer.cli.http.GpxImportResponse;
import com.gregisoft.explorer.cli.importing.TourImportWorkflow;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "import",
        description = "Import one Garmin Connect activity as an Explorer DRAFT tour.",
        mixinStandardHelpOptions = true
)
public class GarminImportCommand implements Callable<Integer> {

    private static final String SOURCE_PROVIDER = "GARMIN_CONNECT";

    @Parameters(index = "0", paramLabel = "<activity-id-or-url>",
            description = "Positive Garmin activity ID or Garmin Connect activity URL.")
    private String activityInput;

    @Option(names = "--backend-url", description = "Explorer backend base URL.")
    private String backendUrl;

    @Spec
    private CommandSpec spec;

    private final CredentialStore credentialStore;
    private final GarminAuthenticator authenticator;
    private final GarminActivityDownloader downloader;
    private final ExplorerBackendClient backendClient;
    private final GarminActivityArchive archive;
    private final FitToGpxConverter converter;
    private final TourImportWorkflow importWorkflow;

    public GarminImportCommand() {
        this(new WindowsCredentialStore(), new GarminPersonalApiClient(),
                new GarminActivityDownloadClient(), new ExplorerBackendClient(),
                new GarminActivityArchive(), new FitToGpxConverter(), null);
    }

    GarminImportCommand(
            CredentialStore credentialStore,
            GarminAuthenticator authenticator,
            GarminActivityDownloader downloader,
            ExplorerBackendClient backendClient,
            GarminActivityArchive archive,
            FitToGpxConverter converter,
            TourImportWorkflow importWorkflow
    ) {
        this.credentialStore = credentialStore;
        this.authenticator = authenticator;
        this.downloader = downloader;
        this.backendClient = backendClient;
        this.archive = archive;
        this.converter = converter;
        this.importWorkflow = importWorkflow == null
                ? new TourImportWorkflow(backendClient) : importWorkflow;
    }

    @Override
    public Integer call() {
        char[] apiKey = null;
        Path temporaryGpx = null;
        try {
            GarminActivityId activityId = GarminActivityId.parse(activityInput);
            URI resolvedBackendUrl = TourImportCommand.resolveBackendUrl(
                    backendUrl, System.getenv("EXPLORER_BACKEND_URL"));

            Optional<char[]> configuredKey = credentialStore.read();
            if (configuredKey.isEmpty()) {
                throw new GarminAuthenticationException(
                        "Garmin Personal API access is not configured. Run: explorer garmin configure");
            }
            apiKey = configuredKey.get();
            GarminAccessToken token = authenticator.authenticate(apiKey);

            Optional<GpxImportResponse> existing = backendClient.findImportBySource(
                    resolvedBackendUrl, SOURCE_PROVIDER, activityId.value());
            if (existing.isPresent()) {
                printExisting(activityId, existing.get());
                return 0;
            }

            byte[] downloadedArchive = downloader.download(token.value(), activityId);
            byte[] fit = archive.extractSingleFit(downloadedArchive);
            temporaryGpx = Files.createTempFile(
                    "explorer-garmin-" + activityId.value() + "-", ".gpx");
            converter.convert(fit, temporaryGpx);

            GpxImportOutcome outcome = importWorkflow.importGpx(
                    resolvedBackendUrl,
                    temporaryGpx,
                    SOURCE_PROVIDER,
                    activityId.value()
            );
            if (outcome.created()) {
                spec.commandLine().getOut().printf(
                        "Created tour %d with status %s from Garmin activity %s.%n",
                        outcome.response().tourId(), outcome.response().status(), activityId.value());
            } else {
                printExisting(activityId, outcome.response());
            }
            return 0;
        } catch (IllegalArgumentException | CredentialStoreException
                | GarminAuthenticationException | GarminActivityDownloadException
                | FitConversionException | GpxProcessingException | BackendClientException
                | IOException exception) {
            spec.commandLine().getErr().println("Garmin import failed: " + exception.getMessage());
            return 1;
        } finally {
            if (apiKey != null) {
                Arrays.fill(apiKey, '\0');
            }
            if (temporaryGpx != null) {
                try {
                    Files.deleteIfExists(temporaryGpx);
                } catch (IOException exception) {
                    spec.commandLine().getErr().println(
                            "Warning: could not remove temporary GPX file: " + temporaryGpx);
                }
            }
        }
    }

    private void printExisting(GarminActivityId activityId, GpxImportResponse existing) {
        spec.commandLine().getOut().printf(
                "Garmin activity %s was already imported as Explorer tour %d with status %s.%n",
                activityId.value(), existing.tourId(), existing.status());
    }
}
