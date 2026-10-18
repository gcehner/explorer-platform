package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourUpdateException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TourUpdateExceptionMapper implements ExceptionMapper<TourUpdateException> {

    @Override
    public Response toResponse(TourUpdateException exception) {
        return Response.status(exception.getStatus())
                .entity(new ImportErrorResponse(exception.getMessage()))
                .build();
    }
}
