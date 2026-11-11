package com.gregisoft.explorer.cli.garmin;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GarminActivityArchiveTest {

    @Test
    void extractsExactlyOneFitFile() throws Exception {
        byte[] fit = {1, 2, 3};
        assertArrayEquals(fit, new GarminActivityArchive().extractSingleFit(
                GarminFitFixtures.zip("activity.fit", fit)));
    }

    @Test
    void rejectsTraversalUnexpectedFilesAndInvalidZip() throws Exception {
        GarminActivityArchive archive = new GarminActivityArchive();
        assertThrows(GarminActivityDownloadException.class,
                () -> archive.extractSingleFit(GarminFitFixtures.zip("../activity.fit", new byte[]{1})));
        assertThrows(GarminActivityDownloadException.class,
                () -> archive.extractSingleFit(GarminFitFixtures.zip("activity.txt", new byte[]{1})));
        assertThrows(GarminActivityDownloadException.class,
                () -> archive.extractSingleFit(new byte[]{1, 2, 3}));
    }

    @Test
    void rejectsOversizedDecompressedFit() throws Exception {
        byte[] compressible = new byte[65];
        Arrays.fill(compressible, (byte) 1);
        GarminActivityArchive archive = new GarminActivityArchive(10, 128, 64);

        assertThrows(GarminActivityDownloadException.class,
                () -> archive.extractSingleFit(GarminFitFixtures.zip("activity.fit", compressible)));
    }

    @Test
    void rejectsMultipleFitFiles() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("one.fit"));
            zip.write(1);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("two.fit"));
            zip.write(2);
            zip.closeEntry();
        }
        assertThrows(GarminActivityDownloadException.class,
                () -> new GarminActivityArchive().extractSingleFit(output.toByteArray()));
    }
}
