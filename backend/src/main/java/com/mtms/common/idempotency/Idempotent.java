package com.mtms.common.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Declares that a REST controller endpoint enforces idempotency via header Idempotency-Key.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Time-to-live for the idempotency record in seconds.
     * Default: 86400 (24 hours).
     */
    long ttlSeconds() default 86400L;

    /**
     * Whether the Idempotency-Key header is strictly required for this endpoint.
     * Default: false (if omitted, request proceeds without deduplication).
     */
    boolean required() default false;
}
