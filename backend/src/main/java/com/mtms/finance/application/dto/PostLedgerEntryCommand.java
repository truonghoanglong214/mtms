package com.mtms.finance.application.dto;

import com.mtms.finance.domain.EntryType;
import com.mtms.finance.domain.SourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PostLedgerEntryCommand(
        @NotNull(message = "Tài khoản không được để trống")
        UUID accountId,

        @NotNull(message = "Danh mục không được để trống")
        UUID categoryId,

        @NotNull(message = "Loại dòng tiền không được để trống")
        EntryType entryType,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        BigDecimal amount,

        @NotNull(message = "Ngày kinh doanh không được để trống")
        LocalDate businessDate,

        String memo,

        UUID evidenceFileId,

        SourceType sourceType,

        UUID sourceId,

        String sourceLeg
) {}
