package com.gregisoft.explorer.cli.garmin;

import com.garmin.fit.Activity;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class GarminFitFixtures {

    private static final double DEGREES_TO_SEMICIRCLES = 2147483648.0 / 180.0;

    private GarminFitFixtures() {
    }

    public static byte[] activity(Path directory, boolean includeGps, int sessionCount) throws IOException {
        Path fit = directory.resolve("synthetic-" + System.nanoTime() + ".fit");
        FileEncoder encoder = new FileEncoder(fit.toFile(), Fit.ProtocolVersion.V2_0);
        Instant start = Instant.parse("2026-06-14T08:00:00Z");
        try {
            FileIdMesg fileId = new FileIdMesg();
            fileId.setType(com.garmin.fit.File.ACTIVITY);
            fileId.setManufacturer(1);
            fileId.setProduct(1);
            fileId.setSerialNumber(123L);
            fileId.setTimeCreated(DateTime.from(start));
            encoder.write(fileId);

            if (includeGps) {
                encoder.write(record(46.5100000, 13.7400000, 900.0f, start));
                encoder.write(record(46.5110000, 13.7420000, 915.0f, start.plusSeconds(60)));
                encoder.write(record(46.5120000, 13.7440000, 910.0f, start.plusSeconds(120)));
            } else {
                RecordMesg record = new RecordMesg();
                record.setTimestamp(DateTime.from(start));
                record.setEnhancedAltitude(900.0f);
                encoder.write(record);
            }

            for (int index = 0; index < sessionCount; index++) {
                SessionMesg session = new SessionMesg();
                session.setStartTime(DateTime.from(start));
                session.setTimestamp(DateTime.from(start.plusSeconds(120)));
                session.setTotalElapsedTime(120.0f);
                session.setTotalTimerTime(120.0f);
                session.setSport(sessionCount > 1 ? Sport.MULTISPORT : Sport.CYCLING);
                encoder.write(session);
            }

            ActivityMesg activity = new ActivityMesg();
            activity.setTimestamp(DateTime.from(start.plusSeconds(120)));
            activity.setTotalTimerTime(120.0f);
            activity.setNumSessions(sessionCount);
            activity.setType(Activity.MANUAL);
            encoder.write(activity);
        } finally {
            encoder.close();
        }
        byte[] bytes = Files.readAllBytes(fit);
        Files.delete(fit);
        return bytes;
    }

    public static byte[] zip(String entryName, byte[] content) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content);
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private static RecordMesg record(
            double latitude,
            double longitude,
            float elevation,
            Instant timestamp
    ) {
        RecordMesg record = new RecordMesg();
        record.setPositionLat((int) Math.round(latitude * DEGREES_TO_SEMICIRCLES));
        record.setPositionLong((int) Math.round(longitude * DEGREES_TO_SEMICIRCLES));
        record.setEnhancedAltitude(elevation);
        record.setTimestamp(DateTime.from(timestamp));
        return record;
    }
}
