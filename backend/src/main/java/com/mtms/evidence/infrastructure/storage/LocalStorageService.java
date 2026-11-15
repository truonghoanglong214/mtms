package com.mtms.evidence.infrastructure.storage;

import com.mtms.common.error.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * P0-SEC-01 / BR-SEC-01
 * Local filesystem implementation of private storage with strict path traversal checks.
 * Stored outside public webroot to prevent unauthorized direct HTTP access.
 */
@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootPath;

    public LocalStorageService(@Value("${mtms.storage.evidence-path:./mtms-storage/evidence}") String storagePath) {
        this.rootPath = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
                log.info("Initialized MTMS private storage directory at: {}", rootPath);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create storage directory: " + rootPath, e);
        }
    }

    private Path resolveSafePath(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Storage object key cannot be blank");
        }
        Path resolved = rootPath.resolve(objectKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new SecurityException("Potential directory traversal attempt detected in storage key: " + objectKey);
        }
        return resolved;
    }

    @Override
    public void store(String objectKey, InputStream inputStream, long byteSize, String contentType) {
        Path targetPath = resolveSafePath(objectKey);
        try {
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored evidence file: {} (size: {} bytes, type: {})", objectKey, byteSize, contentType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file with key: " + objectKey, e);
        }
    }

    @Override
    public InputStream load(String objectKey) {
        Path targetPath = resolveSafePath(objectKey);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new ResourceNotFoundException("Evidence file payload not found in storage: " + objectKey);
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file from storage: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        Path targetPath = resolveSafePath(objectKey);
        try {
            Files.deleteIfExists(targetPath);
            log.debug("Deleted evidence file from storage: {}", objectKey);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete file from storage: " + objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        Path targetPath = resolveSafePath(objectKey);
        return Files.exists(targetPath) && Files.isRegularFile(targetPath);
    }

    public Path getRootPath() {
        return rootPath;
    }
}
