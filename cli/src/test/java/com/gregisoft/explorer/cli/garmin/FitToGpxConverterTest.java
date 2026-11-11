package com.gregisoft.explorer.cli.garmin;

import com.gregisoft.explorer.cli.gpx.GpxTrackData;
import com.gregisoft.explorer.cli.gpx.GpxTrackReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FitToGpxConverterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void convertsFitCoordinatesElevationAndTimestampsToCompatibleGpx() throws Exception {
        Path gpx = tempDirectory.resolve("converted.gpx");

        new FitToGpxConverter().convert(
                GarminFitFixtures.activity(tempDirectory, true, 1), gpx);

        GpxTrackData track = new GpxTrackReader().read(gpx);
        assertEquals(1, track.segments().size());
        assertEquals(3, track.segments().getFirst().points().size());
        assertEquals(46.51, track.segments().getFirst().points().getFirst().latitude(), 0.000001);
        assertEquals(13.74, track.segments().getFirst().points().getFirst().longitude(), 0.000001);
        assertEquals(900.0, track.segments().getFirst().points().getFirst().elevationMeters(), 0.001);
        assertEquals(Instant.parse("2026-06-14T08:00:00Z"),
                track.segments().getFirst().points().getFirst().timestamp());
        assertTrue(Files.size(gpx) > 0);
    }

    @Test
    void rejectsCorruptFitFitWithoutGpsAndMultisport() throws Exception {
        FitToGpxConverter converter = new FitToGpxConverter();
        assertThrows(FitConversionException.class,
                () -> converter.convert(new byte[]{1, 2, 3}, tempDirectory.resolve("bad.gpx")));
        assertThrows(FitConversionException.class,
                () -> converter.convert(
                        GarminFitFixtures.activity(tempDirectory, false, 1),
                        tempDirectory.resolve("no-gps.gpx")));
        assertThrows(FitConversionException.class,
                () -> converter.convert(
                        GarminFitFixtures.activity(tempDirectory, true, 2),
                        tempDirectory.resolve("multi.gpx")));
    }
}
