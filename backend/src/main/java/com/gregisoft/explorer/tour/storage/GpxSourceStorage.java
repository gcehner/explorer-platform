package com.gregisoft.explorer.tour.storage;

import java.nio.file.Path;

public interface GpxSourceStorage {

    String store(Path uploadedFile);
}
