package com.mtms.evidence.infrastructure.storage;

import java.io.InputStream;

/**
 * P0-SEC-01 / BR-SEC-01
 * Abstraction for private blob/file storage (Local filesystem, S3, MinIO, etc.).
 */
public interface StorageService {

    void store(String objectKey, InputStream inputStream, long byteSize, String contentType);

    InputStream load(String objectKey);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
