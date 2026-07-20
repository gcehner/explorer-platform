package com.gregisoft.explorer.info.service;

import com.gregisoft.explorer.info.api.ApplicationInfoResponse;
import com.gregisoft.explorer.info.entity.ApplicationInfo;
import com.gregisoft.explorer.info.repository.ApplicationInfoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ApplicationInfoService {

    private final ApplicationInfoRepository repository;

    public ApplicationInfoService(ApplicationInfoRepository repository) {
        this.repository = repository;
    }

    public ApplicationInfoResponse getApplicationInfo() {
        ApplicationInfo applicationInfo = repository.findApplicationInfo()
                .orElseThrow(() ->
                        new NotFoundException("Application information was not found.")
                );

        return new ApplicationInfoResponse(
                applicationInfo.getName(),
                applicationInfo.getVersion()
        );
    }
}
