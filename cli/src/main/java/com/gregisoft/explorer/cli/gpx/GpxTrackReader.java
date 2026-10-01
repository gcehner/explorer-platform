package com.gregisoft.explorer.cli.gpx;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Track;
import io.jenetics.jpx.WayPoint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class GpxTrackReader {

    private static final GPX.Reader STRICT_READER = GPX.Reader.of(GPX.Reader.Mode.STRICT);

    public GpxTrackData read(Path path) {
        try {
            GPX gpx = STRICT_READER.read(path);
            List<Track> tracks = gpx.tracks()
                    .filter(track -> track.segments().anyMatch(segment -> segment.points().findAny().isPresent()))
                    .toList();
            if (tracks.isEmpty()) {
                throw new GpxProcessingException("No usable GPX track was found; GPX routes are not imported.");
            }
            if (tracks.size() != 1) {
                throw new GpxProcessingException("GPX import requires exactly one non-empty track.");
            }

            List<TrackSegmentData> segments = tracks.getFirst().segments()
                    .map(segment -> new TrackSegmentData(segment.points().map(this::mapPoint).toList()))
                    .filter(segment -> !segment.points().isEmpty())
                    .toList();
            if (segments.isEmpty()) {
                throw new GpxProcessingException("The GPX track has no non-empty segments.");
            }
            for (TrackSegmentData segment : segments) {
                if (segment.points().size() < 2) {
                    throw new GpxProcessingException(
                            "Every non-empty track segment must contain at least two points."
                    );
                }
            }
            return new GpxTrackData(segments);
        } catch (GpxProcessingException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new GpxProcessingException("Could not parse GPX file: " + rootMessage(exception), exception);
        }
    }

    private TrackPoint mapPoint(WayPoint point) {
        return new TrackPoint(
                point.getLatitude().doubleValue(),
                point.getLongitude().doubleValue(),
                point.getElevation().map(Number::doubleValue).orElse(null),
                point.getTime().orElse(null)
        );
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
