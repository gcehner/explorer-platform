package com.gregisoft.explorer.info.api;

import com.gregisoft.explorer.info.service.ApplicationInfoService;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/info")
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationInfoResource {

    private final ApplicationInfoService service;

    public ApplicationInfoResource(ApplicationInfoService service) {
        this.service = service;
    }

    @GET
    public ApplicationInfoResponse getApplicationInfo() {
        return service.getApplicationInfo();
    }
}
