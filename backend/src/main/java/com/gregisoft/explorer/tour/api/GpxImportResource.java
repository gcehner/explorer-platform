package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.GpxImportService;
import com.gregisoft.explorer.tour.service.GpxImportResult;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

        GpxImportResult result = service.importGpx(
                metadata,
                gpxFile.uploadedFile(),
                gpxFile.fileName()
        );
        return Response.status(result.created() ? Response.Status.CREATED : Response.Status.OK)
                .entity(result.response())
                .build();
    }

    @GET
    @Path("/sources/{provider}/{activityId}")
    public Response findBySource(
            @PathParam("provider") String provider,
            @PathParam("activityId") String activityId
    ) {
        return service.findBySource(provider, activityId)
                .map(response -> Response.ok(response).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
