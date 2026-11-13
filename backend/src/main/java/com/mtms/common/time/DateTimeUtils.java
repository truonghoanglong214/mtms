package com.mtms.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Standardizes time conventions across MTMS:
 * - Event audit timestamps: UTC (Instant)
 * - Business operational dates: Asia/Ho_Chi_Minh (LocalDate)
 */
public final class DateTimeUtils {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    private DateTimeUtils() {
    }

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static LocalDate todayInVietnam() {
        return LocalDate.now(VIETNAM_ZONE);
    }

    public static LocalDate toVietnamLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(VIETNAM_ZONE).toLocalDate();
    }

    public static Instant startOfDayInVietnamUtc(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(VIETNAM_ZONE).toInstant();
    }
}
