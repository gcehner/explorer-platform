package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.type.SurfaceType;

public record SurfaceShareResponse(
        SurfaceType surfaceType,
        int percentage
) {
}
