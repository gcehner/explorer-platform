package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.api.ElevationProfileSampleRequest;
import com.gregisoft.explorer.tour.api.GeoJsonLineStringRequest;
import com.gregisoft.explorer.tour.api.GpxImportMetadataRequest;
import com.gregisoft.explorer.tour.entity.Tour;
import com.gregisoft.explorer.tour.repository.TourRepository;
import com.gregisoft.explorer.tour.storage.GpxSourceStorage;
import com.gregisoft.explorer.tour.storage.GpxSourceStorageException;
import com.gregisoft.explorer.tour.type.PublicationStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpxImportServiceTest {

    @Test
    void validImportCreatesDraftWithNormalizedDataAndStoredSource() {
        CapturingRepository repository = new CapturingRepository();
        RecordingStorage storage = new RecordingStorage("generated.gpx");
        GpxImportService service = new GpxImportService(repository, storage);

        service.importGpx(validRequest(LocalDate.of(2025, 7, 12)), Path.of("upload.tmp"), "ride.gpx");

        Tour tour = repository.persisted;
        assertEquals(1, repository.persistCount);
        assertEquals(1, storage.callCount);
        assertEquals(PublicationStatus.DRAFT, tour.getPublicationStatus());
        assertEquals(new BigDecimal("35.60"), tour.getStatistics().getDistanceKm());
        assertEquals(1650, tour.getStatistics().getTotalAscentMeters());
        assertEquals(LocalDate.of(2025, 7, 12), tour.getTourDate());
        assertEquals(List.of(0.0, 12.5, 35.6), tour.getElevationProfile().stream()
                .map(sample -> sample.getDistanceKm().doubleValue())
                .toList());
        assertEquals(4326, tour.getRouteGeometry().getSRID());
        assertEquals("ride.gpx", tour.getGpxTrack().getOriginalFilename());
        assertEquals("generated.gpx", tour.getGpxTrack().getSourceReference());
    }

    @Test
    void tourDateMayBeAbsent() {
        CapturingRepository repository = new CapturingRepository();
        GpxImportService service = new GpxImportService(repository, new RecordingStorage("source.gpx"));

        service.importGpx(validRequest(null), Path.of("upload.tmp"), "ride.gpx");

        assertNull(repository.persisted.getTourDate());
    }

    @Test
    void invalidStatisticsAreRejectedBeforeStorage() {
        CapturingRepository repository = new CapturingRepository();
        RecordingStorage storage = new RecordingStorage("source.gpx");
        GpxImportService service = new GpxImportService(repository, storage);
        GpxImportMetadataRequest request = new GpxImportMetadataRequest(
                new BigDecimal("35.601"), 1, 1, 100, 200, null,
                validProfile(), validGeometry()
        );

        assertThrows(
                InvalidGpxImportException.class,
                () -> service.importGpx(request, Path.of("upload.tmp"), "ride.gpx")
        );
        assertEquals(0, storage.callCount);
        assertEquals(0, repository.persistCount);
    }

    @Test
    void invalidElevationProfileIsRejected() {
        GpxImportService service = new GpxImportService(
                new CapturingRepository(), new RecordingStorage("source.gpx")
        );
        GpxImportMetadataRequest request = new GpxImportMetadataRequest(
                new BigDecimal("35.60"), 1, 1, 100, 200, null,
                List.of(
                        new ElevationProfileSampleRequest(new BigDecimal("2.000"), 100),
                        new ElevationProfileSampleRequest(new BigDecimal("1.000"), 120)
                ),
                validGeometry()
        );

        assertThrows(
                InvalidGpxImportException.class,
                () -> service.importGpx(request, Path.of("upload.tmp"), "ride.gpx")
        );
    }

    @Test
    void nonLineStringAndInvalidCoordinatesAreRejected() {
        GpxImportService service = new GpxImportService(
                new CapturingRepository(), new RecordingStorage("source.gpx")
        );
        GpxImportMetadataRequest wrongType = withGeometry(
                new GeoJsonLineStringRequest("Point", List.of(List.of(BigDecimal.ZERO, BigDecimal.ZERO)))
        );
        GpxImportMetadataRequest invalidCoordinate = withGeometry(
                new GeoJsonLineStringRequest("LineString", List.of(
                        List.of(new BigDecimal("181"), BigDecimal.ZERO),
                        List.of(BigDecimal.ONE, BigDecimal.ONE)
                ))
        );

        assertThrows(
                InvalidGpxImportException.class,
                () -> service.importGpx(wrongType, Path.of("upload.tmp"), "ride.gpx")
        );
        assertThrows(
                InvalidGpxImportException.class,
                () -> service.importGpx(invalidCoordinate, Path.of("upload.tmp"), "ride.gpx")
        );
    }

    @Test
    void coordinateThatConvertsToNonFiniteDoubleIsRejectedBeforeStorage() {
        CapturingRepository repository = new CapturingRepository();
        RecordingStorage storage = new RecordingStorage("source.gpx");
        GpxImportService service = new GpxImportService(repository, storage);
        GpxImportMetadataRequest request = withGeometry(
                new GeoJsonLineStringRequest("LineString", List.of(
                        List.of(new BigDecimal("1e10000"), BigDecimal.ZERO),
                        List.of(BigDecimal.ONE, BigDecimal.ONE)
                ))
        );

        assertThrows(
                InvalidGpxImportException.class,
                () -> service.importGpx(request, Path.of("upload.tmp"), "ride.gpx")
        );
        assertEquals(0, storage.callCount);
        assertEquals(0, repository.persistCount);
    }

    @Test
    void storageFailureDoesNotPersistTour() {
        CapturingRepository repository = new CapturingRepository();
        GpxImportService service = new GpxImportService(repository, path -> {
            throw new GpxSourceStorageException("failed", new Exception("disk"));
        });

        assertThrows(
                GpxSourceStorageException.class,
                () -> service.importGpx(validRequest(null), Path.of("upload.tmp"), "ride.gpx")
        );
        assertEquals(0, repository.persistCount);
    }

    private static GpxImportMetadataRequest validRequest(LocalDate date) {
        return new GpxImportMetadataRequest(
                new BigDecimal("35.60"), 1650, 1600, 923, 2677, date,
                validProfile(), validGeometry()
        );
    }

    private static GpxImportMetadataRequest withGeometry(GeoJsonLineStringRequest geometry) {
        return new GpxImportMetadataRequest(
                new BigDecimal("35.60"), 1650, 1600, 923, 2677, null,
                validProfile(), geometry
        );
    }

    private static List<ElevationProfileSampleRequest> validProfile() {
        return List.of(
                new ElevationProfileSampleRequest(new BigDecimal("0.000"), 923),
                new ElevationProfileSampleRequest(new BigDecimal("12.500"), 2677),
                new ElevationProfileSampleRequest(new BigDecimal("35.600"), 950)
        );
    }

    private static GeoJsonLineStringRequest validGeometry() {
        return new GeoJsonLineStringRequest("LineString", List.of(
                List.of(new BigDecimal("13.5321"), new BigDecimal("46.4776")),
                List.of(new BigDecimal("13.5432"), new BigDecimal("46.4890"))
        ));
    }

    private static final class CapturingRepository extends TourRepository {
        private Tour persisted;
        private int persistCount;

        private CapturingRepository() {
            super(null);
        }

        @Override
        public void persistAndFlush(Tour tour) {
            persisted = tour;
            persistCount++;
        }
    }

    private static final class RecordingStorage implements GpxSourceStorage {
        private final String sourceReference;
        private int callCount;

        private RecordingStorage(String sourceReference) {
            this.sourceReference = sourceReference;
        }

        @Override
        public String store(Path uploadedFile) {
            callCount++;
            return sourceReference;
        }
    }
}
