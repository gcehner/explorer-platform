package com.gregisoft.explorer.cli.importing;

import com.gregisoft.explorer.cli.gpx.GpxProcessor;
import com.gregisoft.explorer.cli.gpx.GpxTrackReader;
import com.gregisoft.explorer.cli.gpx.NormalizedGpxData;
import com.gregisoft.explorer.cli.http.ExplorerBackendClient;
import com.gregisoft.explorer.cli.http.GpxImportOutcome;

import java.net.URI;
import java.nio.file.Path;

public final class TourImportWorkflow {

    private final GpxTrackReader trackReader;
    private final GpxProcessor processor;
    private final ExplorerBackendClient backendClient;

    public TourImportWorkflow() {
        this(new GpxTrackReader(), new GpxProcessor(), new ExplorerBackendClient());
    }

    public TourImportWorkflow(ExplorerBackendClient backendClient) {
        this(new GpxTrackReader(), new GpxProcessor(), backendClient);
    }

    TourImportWorkflow(
            GpxTrackReader trackReader,
            GpxProcessor processor,
            ExplorerBackendClient backendClient
    ) {
        this.trackReader = trackReader;
        this.processor = processor;
        this.backendClient = backendClient;
    }

    public GpxImportOutcome importGpx(
            URI backendUrl,
            Path gpxFile,
            String sourceProvider,
            String sourceActivityId
    ) {
        NormalizedGpxData normalized = processor.process(trackReader.read(gpxFile));
        return backendClient.importGpx(
                backendUrl, gpxFile, normalized, sourceProvider, sourceActivityId);
    }
}
