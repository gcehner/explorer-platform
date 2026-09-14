package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.InvalidGpxImportException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidGpxImportExceptionMapper
        implements ExceptionMapper<InvalidGpxImportException> {

    @Override
    public Response toResponse(InvalidGpxImportException exception) {
        return Response.status(422)
                .entity(new ImportErrorResponse(exception.getMessage()))
                .build();
    }
}
