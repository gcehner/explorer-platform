package com.gregisoft.explorer.info.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_info")
public class ApplicationInfo {

    @Id
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    protected ApplicationInfo() {
        // Required by Jakarta Persistence.
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
