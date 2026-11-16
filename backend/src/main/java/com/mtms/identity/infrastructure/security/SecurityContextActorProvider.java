package com.mtms.identity.infrastructure.security;

import com.mtms.common.idempotency.CurrentActorProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * P0-SEC-03 / BR-SEC-03 / TC-011
 * Implementation of CurrentActorProvider extracting actor UUID from SecurityContext.
 */
@Component
public class SecurityContextActorProvider implements CurrentActorProvider {

    @Override
    public UUID getCurrentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }

        if (principal instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }
}
