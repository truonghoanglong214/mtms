package com.mtms.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

/**
 * P0-SEC-05 / BR-SEC-04
 * Custom health indicator providing deep diagnostics for database connectivity
 * and operational storage readiness.
 */
@Component("systemReadiness")
public class SystemReadinessHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public SystemReadinessHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        boolean isHealthy = true;

        // 1. Check database connectivity
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            long start = System.currentTimeMillis();
            stmt.execute("SELECT 1");
            long elapsed = System.currentTimeMillis() - start;
            builder.withDetail("database", "UP")
                   .withDetail("dbResponseTimeMs", elapsed);
        } catch (Exception e) {
            isHealthy = false;
            builder.withDetail("database", "DOWN")
                   .withDetail("databaseError", e.getMessage());
        }

        // 2. Check local storage disk space
        try {
            File storageDir = new File("mtms-storage");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            long usableSpaceMb = storageDir.getUsableSpace() / (1024 * 1024);
            builder.withDetail("storagePath", storageDir.getAbsolutePath())
                   .withDetail("usableDiskSpaceMb", usableSpaceMb);
            if (usableSpaceMb < 50) { // Warning if less than 50MB
                builder.withDetail("storageWarning", "Low disk space (<50MB)");
            }
        } catch (Exception e) {
            builder.withDetail("storageError", e.getMessage());
        }

        if (isHealthy) {
            return builder.up().build();
        } else {
            return builder.down().build();
        }
    }
}
