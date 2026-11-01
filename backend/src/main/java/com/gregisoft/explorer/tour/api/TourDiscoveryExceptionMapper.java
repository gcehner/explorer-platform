package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourDiscoveryException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TourDiscoveryExceptionMapper implements ExceptionMapper<TourDiscoveryException> {

    @Override
    public Response toResponse(TourDiscoveryException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ImportErrorResponse(exception.getMessage()))
                .build();
    }
}
