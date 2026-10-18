package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourPublicationService;
import com.gregisoft.explorer.tour.service.TourReadService;
import com.gregisoft.explorer.tour.service.TourUpdateService;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/management/tours")
@Produces(MediaType.APPLICATION_JSON)
public class ManagementTourResource {

    private final TourReadService service;
    private final TourUpdateService updateService;
    private final TourPublicationService publicationService;

    public ManagementTourResource(
            TourReadService service,
            TourUpdateService updateService,
            TourPublicationService publicationService
    ) {
        this.service = service;
        this.updateService = updateService;
        this.publicationService = publicationService;
    }

    @GET
    @Path("/{id}")
    public TourDetailResponse getTour(@PathParam("id") long id) {
        return service.getManagementTour(id);
    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public TourDetailResponse updateTour(@PathParam("id") long id, ObjectNode patch) {
        return updateService.updateDraft(id, patch);
    }

    @POST
    @Path("/{id}/publish")
    public TourDetailResponse publishTour(@PathParam("id") long id) {
        return publicationService.publish(id);
    }
}
