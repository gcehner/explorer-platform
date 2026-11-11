package com.gregisoft.explorer.tour.storage;

import java.nio.file.Path;

public interface GpxSourceStorage {

    String store(Path uploadedFile);

    default void delete(String sourceReference) {
        // Implementations that can remove a newly stored source should override this method.
    }
}
