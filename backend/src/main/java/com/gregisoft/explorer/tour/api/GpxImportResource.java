package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.GpxImportService;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/management/imports/gpx")
@Produces(MediaType.APPLICATION_JSON)
public class GpxImportResource {

    private final GpxImportService service;

    public GpxImportResource(GpxImportService service) {
        this.service = service;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importGpx(
            @RestForm("metadata") @PartType(MediaType.APPLICATION_JSON)
            GpxImportMetadataRequest metadata,
            @RestForm("gpxFile") FileUpload gpxFile
    ) {
        if (metadata == null) {
            throw new BadRequestException("The metadata part is required.");
        }
        if (gpxFile == null) {
            throw new BadRequestException("The gpxFile part is required.");
        }
        if (gpxFile.size() == 0) {
            throw new BadRequestException("The GPX file must not be empty.");
        }

        GpxImportResponse response = service.importGpx(
                metadata,
                gpxFile.uploadedFile(),
                gpxFile.fileName()
        );
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
