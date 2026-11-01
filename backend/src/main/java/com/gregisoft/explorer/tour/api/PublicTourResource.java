package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourDiscoveryService;
import com.gregisoft.explorer.tour.service.TourReadService;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/tours")
@Produces(MediaType.APPLICATION_JSON)
public class PublicTourResource {

    private final TourReadService service;
    private final TourDiscoveryService discoveryService;

    public PublicTourResource(
            TourReadService service,
            TourDiscoveryService discoveryService
    ) {
        this.service = service;
        this.discoveryService = discoveryService;
    }

    @GET
    public TourPageResponse getTours(
            @QueryParam("bbox") String bbox,
            @QueryParam("countryCode") String countryCode,
            @QueryParam("technicalDifficulty") String technicalDifficulty,
            @QueryParam("distanceMinKm") String distanceMinKm,
            @QueryParam("distanceMaxKm") String distanceMaxKm,
            @QueryParam("ascentMinMeters") String ascentMinMeters,
            @QueryParam("ascentMaxMeters") String ascentMaxMeters,
            @QueryParam("pageSize") String pageSize,
            @QueryParam("cursor") String cursor
    ) {
        return discoveryService.findTours(
                bbox,
                countryCode,
                technicalDifficulty,
                distanceMinKm,
                distanceMaxKm,
                ascentMinMeters,
                ascentMaxMeters,
                pageSize,
                cursor
        );
    }

    @GET
    @Path("/{id}")
    public TourDetailResponse getTour(@PathParam("id") long id) {
        return service.getPublishedTour(id);
    }
}
