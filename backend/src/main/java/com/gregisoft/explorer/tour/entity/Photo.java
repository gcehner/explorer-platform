package com.gregisoft.explorer.tour.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "tour_photo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tour_id", "display_order"})
)
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "image_reference", nullable = false, length = 1000)
    private String imageReference;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Photo() {
        // Required by Jakarta Persistence.
    }

    public Photo(String imageReference, String caption, int displayOrder) {
        this.imageReference = requireText(imageReference, "Image reference");
        this.caption = normalize(caption);
        setDisplayOrder(displayOrder);
    }

    void attachTo(Tour tour) {
        if (this.tour != null && this.tour != tour) {
            throw new IllegalStateException("A Photo cannot belong to more than one Tour.");
        }
        this.tour = tour;
    }

    void detachFrom(Tour tour) {
        if (this.tour == tour) {
            this.tour = null;
        }
    }

    void setDisplayOrder(int displayOrder) {
        if (displayOrder < 0) {
            throw new IllegalArgumentException("Photo display order must be zero or greater.");
        }
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Tour getTour() {
        return tour;
    }

    public String getImageReference() {
        return imageReference;
    }

    public String getCaption() {
        return caption;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
