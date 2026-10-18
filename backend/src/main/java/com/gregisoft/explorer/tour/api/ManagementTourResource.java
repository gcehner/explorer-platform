package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourReadService;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/management/tours")
@Produces(MediaType.APPLICATION_JSON)
public class ManagementTourResource {

    private final TourReadService service;

    public ManagementTourResource(TourReadService service) {
        this.service = service;
    }

    @GET
    @Path("/{id}")
    public TourDetailResponse getTour(@PathParam("id") long id) {
        return service.getManagementTour(id);
    }
}
