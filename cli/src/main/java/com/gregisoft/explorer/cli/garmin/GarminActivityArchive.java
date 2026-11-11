package com.gregisoft.explorer.cli.garmin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public final class GarminActivityArchive {

    static final int MAX_ENTRY_COUNT = 10;
    static final int MAX_DECOMPRESSED_BYTES = 128 * 1024 * 1024;
    static final int MAX_FIT_BYTES = 64 * 1024 * 1024;

    private final int maxEntryCount;
    private final int maxDecompressedBytes;
    private final int maxFitBytes;

    public GarminActivityArchive() {
        this(MAX_ENTRY_COUNT, MAX_DECOMPRESSED_BYTES, MAX_FIT_BYTES);
    }

    GarminActivityArchive(int maxEntryCount, int maxDecompressedBytes, int maxFitBytes) {
        this.maxEntryCount = maxEntryCount;
        this.maxDecompressedBytes = maxDecompressedBytes;
        this.maxFitBytes = maxFitBytes;
    }

    public byte[] extractSingleFit(byte[] archive) {
        if (archive == null || archive.length == 0) {
            throw new GarminActivityDownloadException("Garmin activity archive is empty.");
        }
        byte[] fit = null;
        int entries = 0;
        int totalBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries++;
                if (entries > maxEntryCount) {
                    throw invalid("contains too many entries");
                }
                validateEntryName(entry.getName());
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }
                if (!entry.getName().toLowerCase(Locale.ROOT).endsWith(".fit")) {
                    throw invalid("contains an unexpected non-FIT file");
                }
                if (fit != null) {
                    throw invalid("contains more than one FIT file");
                }
                LimitedBytes extracted = readLimited(zip, maxFitBytes, maxDecompressedBytes, totalBytes);
                fit = extracted.bytes();
                totalBytes = extracted.totalArchiveBytes();
                zip.closeEntry();
            }
        } catch (GarminActivityDownloadException exception) {
            throw exception;
        } catch (ZipException exception) {
            throw invalid("is corrupt", exception);
        } catch (IOException exception) {
            throw invalid("could not be read", exception);
        }
        if (fit == null || fit.length == 0) {
            throw invalid("does not contain one usable FIT file");
        }
        return fit;
    }

    private static LimitedBytes readLimited(
            ZipInputStream zip,
            int entryLimit,
            int totalLimit,
            int currentTotal
    ) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int entryBytes = 0;
        int totalBytes = currentTotal;
        int read;
        while ((read = zip.read(buffer)) != -1) {
            entryBytes += read;
            totalBytes += read;
            if (entryBytes > entryLimit) {
                throw invalid("contains a FIT file larger than 64 MiB");
            }
            if (totalBytes > totalLimit) {
                throw invalid("exceeds the decompressed-size safety limit");
            }
            output.write(buffer, 0, read);
        }
        return new LimitedBytes(output.toByteArray(), totalBytes);
    }

    private static void validateEntryName(String name) {
        if (name == null || name.isBlank() || name.indexOf('\0') >= 0) {
            throw invalid("contains an invalid ZIP entry name");
        }
        String portableName = name.replace('\\', '/');
        Path path;
        try {
            path = Path.of(portableName).normalize();
        } catch (RuntimeException exception) {
            throw invalid("contains an invalid ZIP entry name", exception);
        }
        if (path.isAbsolute() || portableName.startsWith("/")
                || path.startsWith("..") || portableName.matches("^[A-Za-z]:/.*")) {
            throw invalid("contains a path-traversal entry");
        }
    }

    private static GarminActivityDownloadException invalid(String reason) {
        return new GarminActivityDownloadException("Garmin activity archive " + reason + ".");
    }

    private static GarminActivityDownloadException invalid(String reason, Throwable cause) {
        return new GarminActivityDownloadException(
                "Garmin activity archive " + reason + ".", cause);
    }

    private record LimitedBytes(byte[] bytes, int totalArchiveBytes) {
    }
}
