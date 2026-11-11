package com.gregisoft.explorer.tour.storage;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@ApplicationScoped
public class FileSystemGpxSourceStorage implements GpxSourceStorage {

    private final Path storageRoot;

    public FileSystemGpxSourceStorage(
            @ConfigProperty(name = "explorer.gpx.storage-root") String storageRoot
    ) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override
    public String store(Path uploadedFile) {
        String sourceReference = UUID.randomUUID() + ".gpx";
        Path target = storageRoot.resolve(sourceReference);

        try {
            Files.createDirectories(storageRoot);
            Files.copy(uploadedFile, target);
            return sourceReference;
        } catch (IOException exception) {
            throw new GpxSourceStorageException("Could not store the original GPX file.", exception);
        }
    }

    @Override
    public void delete(String sourceReference) {
        if (sourceReference == null || sourceReference.isBlank()) {
            return;
        }
        Path target = storageRoot.resolve(sourceReference).normalize();
        if (!target.getParent().equals(storageRoot)) {
            throw new GpxSourceStorageException(
                    "Refusing to remove a GPX source outside the storage root.", null);
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new GpxSourceStorageException(
                    "Could not remove a GPX source after a failed import.", exception);
        }
    }
}
