package com.gregisoft.explorer.cli.gpx;

import java.util.List;

public record GpxTrackData(List<TrackSegmentData> segments) {
    public GpxTrackData {
        segments = List.copyOf(segments);
    }
}
