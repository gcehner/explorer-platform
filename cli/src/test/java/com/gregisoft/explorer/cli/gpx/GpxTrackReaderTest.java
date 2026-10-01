package com.gregisoft.explorer.cli.gpx;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpxTrackReaderTest {

    @TempDir
    Path tempDirectory;

    private final GpxTrackReader reader = new GpxTrackReader();

    @Test
    void readsOneTrackAndIgnoresEmptySegments() throws IOException {
        Path file = write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><trkseg></trkseg><trkseg>
                    <trkpt lat="46.0" lon="14.0"><ele>100</ele></trkpt>
                    <trkpt lat="46.0" lon="14.001"><ele>101</ele></trkpt>
                  </trkseg></trk>
                </gpx>
                """);

        GpxTrackData result = reader.read(file);

        assertEquals(1, result.segments().size());
        assertEquals(2, result.segments().getFirst().points().size());
    }

    @Test
    void rejectsRoutesInsteadOfUsingThemAsTracks() throws IOException {
        Path file = write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
                  <rte><rtept lat="46.0" lon="14.0"/><rtept lat="46.0" lon="14.001"/></rte>
                </gpx>
                """);

        assertThrows(GpxProcessingException.class, () -> reader.read(file));
    }

    @Test
    void rejectsMultipleNonEmptyTracksAndSinglePointSegments() throws IOException {
        Path multiple = write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><trkseg><trkpt lat="46" lon="14"/><trkpt lat="46" lon="14.1"/></trkseg></trk>
                  <trk><trkseg><trkpt lat="47" lon="15"/><trkpt lat="47" lon="15.1"/></trkseg></trk>
                </gpx>
                """);
        assertThrows(GpxProcessingException.class, () -> reader.read(multiple));

        Path singlePoint = write("""
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
                  <trk><trkseg><trkpt lat="46" lon="14"/></trkseg></trk>
                </gpx>
                """);
        assertThrows(GpxProcessingException.class, () -> reader.read(singlePoint));
    }

    @Test
    void rejectsMalformedGpx() throws IOException {
        assertThrows(GpxProcessingException.class, () -> reader.read(write("<gpx>")));
    }

    private Path write(String content) throws IOException {
        Path file = tempDirectory.resolve("track-" + System.nanoTime() + ".gpx");
        return Files.writeString(file, content);
    }
}
