package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourGeometryDensityException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TourGeometryDensityExceptionMapper
        implements ExceptionMapper<TourGeometryDensityException> {

    private static final String ERROR_CODE = "GEOMETRY_TOO_DENSE";

    @Override
    public Response toResponse(TourGeometryDensityException exception) {
        return Response.status(422)
                .entity(new TourGeometryErrorResponse(ERROR_CODE, exception.getMessage()))
                .build();
    }
}
