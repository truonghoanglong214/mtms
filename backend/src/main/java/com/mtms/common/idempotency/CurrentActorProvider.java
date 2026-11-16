package com.mtms.common.idempotency;

import java.util.UUID;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Clean boundary interface for resolving current authenticated actor user ID without depending on identity domain.
 */
@FunctionalInterface
public interface CurrentActorProvider {

    UUID getCurrentActorId();
}
