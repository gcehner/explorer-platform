package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.api.GpxImportResponse;

public record GpxImportResult(GpxImportResponse response, boolean created) {
}
