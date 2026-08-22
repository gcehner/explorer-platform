package com.gregisoft.explorer.tour.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "gpx_track")
public class GpxTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false, unique = true)
    private Tour tour;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "source_reference", nullable = false, length = 1000)
    private String sourceReference;

    protected GpxTrack() {
        // Required by Jakarta Persistence.
    }

    public GpxTrack(String originalFilename, String sourceReference) {
        this.originalFilename = requireText(originalFilename, "Original filename");
        this.sourceReference = requireText(sourceReference, "Source reference");
    }

    void attachTo(Tour tour) {
        if (this.tour != null && this.tour != tour) {
            throw new IllegalStateException("A GPX track cannot belong to more than one Tour.");
        }
        this.tour = tour;
    }

    public Long getId() {
        return id;
    }

    public Tour getTour() {
        return tour;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}
