package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourMapDensityException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TourMapDensityExceptionMapper implements ExceptionMapper<TourMapDensityException> {

    @Override
    public Response toResponse(TourMapDensityException exception) {
        return Response.status(422)
                .entity(new ImportErrorResponse(exception.getMessage()))
                .build();
    }
}
