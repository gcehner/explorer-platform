package com.gregisoft.explorer.cli.gpx;

import java.util.List;

public record TrackSegmentData(List<TrackPoint> points) {
    public TrackSegmentData {
        points = List.copyOf(points);
    }
}
