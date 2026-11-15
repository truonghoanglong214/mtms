package com.mtms.evidence.application.dto;

import java.io.InputStream;

/**
 * Encapsulates the binary stream and headers for serving evidence content.
 */
public record FilePayload(
        InputStream inputStream,
        String originalFilename,
        String contentType,
        long byteSize
) {
}
