package com.mtms.revenue.application.dto;

import com.mtms.revenue.domain.OperationStatus;
import jakarta.validation.constraints.NotNull;

public record SetBusinessDayCommand(
        @NotNull(message = "operationStatus must not be null")
        OperationStatus operationStatus,
        String closeReason
) {
}
