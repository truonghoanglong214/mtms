package com.mtms.organization.application.dto;

import com.mtms.organization.domain.BusinessEntityStatus;

import java.time.Instant;
import java.util.UUID;

public record BusinessEntityDto(
        UUID id,
        String entityCode,
        String registeredName,
        String registrationRef,
        BusinessEntityStatus status,
        Instant createdAt
) {
}
