package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.service.TourDiscoveryService;
import com.gregisoft.explorer.tour.service.TourGeometryService;
import com.gregisoft.explorer.tour.service.TourMapService;
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
    private final TourMapService mapService;
    private final TourGeometryService geometryService;

    public PublicTourResource(
            TourReadService service,
            TourDiscoveryService discoveryService,
            TourMapService mapService,
            TourGeometryService geometryService
    ) {
        this.service = service;
        this.discoveryService = discoveryService;
        this.mapService = mapService;
        this.geometryService = geometryService;
    }

    @GET
    @Path("/geometry")
    public TourGeometryResponse getGeometries(
            @QueryParam("bbox") String bbox,
            @QueryParam("zoom") String zoom,
            @QueryParam("countryCode") String countryCode,
            @QueryParam("technicalDifficulty") String technicalDifficulty,
            @QueryParam("distanceMinKm") String distanceMinKm,
            @QueryParam("distanceMaxKm") String distanceMaxKm,
            @QueryParam("ascentMinMeters") String ascentMinMeters,
            @QueryParam("ascentMaxMeters") String ascentMaxMeters
    ) {
        return geometryService.findGeometries(
                bbox,
                zoom,
                countryCode,
                technicalDifficulty,
                distanceMinKm,
                distanceMaxKm,
                ascentMinMeters,
                ascentMaxMeters
        );
    }

    @GET
    @Path("/map")
    public TourMapResponse getMapFeatures(
            @QueryParam("bbox") String bbox,
            @QueryParam("zoom") String zoom,
            @QueryParam("countryCode") String countryCode,
            @QueryParam("technicalDifficulty") String technicalDifficulty,
            @QueryParam("distanceMinKm") String distanceMinKm,
            @QueryParam("distanceMaxKm") String distanceMaxKm,
            @QueryParam("ascentMinMeters") String ascentMinMeters,
            @QueryParam("ascentMaxMeters") String ascentMaxMeters
    ) {
        return mapService.findMapFeatures(
                bbox,
                zoom,
                countryCode,
                technicalDifficulty,
                distanceMinKm,
                distanceMaxKm,
                ascentMinMeters,
                ascentMaxMeters
        );
    }

    @GET
    @Path("/latest")
    public TourLatestResponse getLatestTours(@QueryParam("limit") String limit) {
        return discoveryService.findLatestTours(limit);
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
