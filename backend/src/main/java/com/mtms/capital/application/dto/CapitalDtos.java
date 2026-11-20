package com.mtms.capital.application.dto;

import com.mtms.capital.domain.model.AgreementStatus;
import com.mtms.capital.domain.model.CommitmentStatus;
import com.mtms.capital.domain.model.ContributionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CapitalDtos {
    private CapitalDtos() {}

    public static final String G5_PROVISIONAL_DISCLAIMER =
            "DỮ LIỆU THIẾT LẬP THAM CHIẾU - CHƯA PHÂN PHỐI LỢI NHUẬN THỰC TẾ TRONG PHA 0 (G5 GATE)";

    // Commands / Requests
    public record CreateInvestorRequest(
            @NotBlank(message = "Tên hiển thị nhà đầu tư không được để trống")
            String displayName,
            String contactPhone,
            String contactEmail,
            String identityNumber,
            String contactNote
    ) {}

    public record CreateAgreementRequest(
            @NotNull(message = "ID nhà đầu tư không được để trống")
            UUID investorId,
            @NotBlank(message = "Số hợp đồng/thỏa thuận không được để trống")
            String agreementRef,
            @NotNull(message = "Loại hình đóng góp không được để trống")
            ContributionType contributionType,
            @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String eligibilityTerms,
            UUID evidenceFileId
    ) {}

    public record CreateCommitmentRequest(
            @NotNull(message = "ID thỏa thuận đầu tư không được để trống")
            UUID agreementId,
            @NotNull(message = "Giá trị cam kết vốn không được để trống")
            @DecimalMin(value = "0.0", message = "Giá trị cam kết không được âm")
            BigDecimal committedValue,
            @DecimalMin(value = "0.0", message = "Giá trị phi tiền mặt không được âm")
            BigDecimal recognizedNonCashValue,
            LocalDate commitmentDueDate,
            String notes
    ) {}

    public record RecordContributionRequest(
            @NotNull(message = "Ngày góp vốn không được để trống")
            LocalDate contributedOn,
            @NotNull(message = "Loại hình góp vốn không được để trống")
            ContributionType contributionType,
            @NotNull(message = "Giá trị góp vốn được công nhận không được để trống")
            @DecimalMin(value = "0.01", message = "Giá trị góp vốn phải lớn hơn 0")
            BigDecimal recognizedValue,
            UUID accountId,
            UUID assetId,
            UUID evidenceFileId,
            String notes
    ) {}

    public record CreateProfitTermRequest(
            @NotNull(message = "ID thỏa thuận đầu tư không được để trống")
            UUID agreementId,
            @NotNull(message = "Ngày bắt đầu hiệu lực không được để trống")
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            @NotNull(message = "Tỷ lệ phân chia lợi nhuận không được để trống")
            @DecimalMin(value = "0.0", message = "Tỷ lệ lợi nhuận không được nhỏ hơn 0")
            @DecimalMax(value = "1.0", message = "Tỷ lệ lợi nhuận không được vượt quá 1.0 (100%)")
            BigDecimal profitShareRatio,
            String eligibilityConditions,
            UUID evidenceFileId
    ) {}

    // DTOs
    public record InvestorDto(
            UUID id,
            UUID entityId,
            String displayName,
            String contactPhone,
            String contactEmail,
            String identityNumber,
            String contactNote,
            boolean active,
            Instant createdAt
    ) {}

    public record AgreementDto(
            UUID id,
            UUID investorId,
            String investorName,
            UUID branchId,
            String agreementRef,
            ContributionType contributionType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            AgreementStatus status,
            String eligibilityTerms,
            UUID evidenceFileId,
            Instant createdAt
    ) {}

    public record CommitmentDto(
            UUID id,
            UUID agreementId,
            UUID investorId,
            String investorName,
            String agreementRef,
            BigDecimal committedValue,
            BigDecimal recognizedNonCashValue,
            BigDecimal actualContributedValue,
            BigDecimal outstandingValue,
            LocalDate commitmentDueDate,
            CommitmentStatus status,
            String notes,
            Instant createdAt
    ) {}

    public record ContributionDto(
            UUID id,
            UUID commitmentId,
            LocalDate contributedOn,
            ContributionType contributionType,
            BigDecimal recognizedValue,
            UUID accountId,
            String accountName,
            UUID assetId,
            UUID ledgerEntryId,
            UUID evidenceFileId,
            String notes,
            UUID createdBy,
            Instant createdAt
    ) {}

    public record ProfitTermDto(
            UUID id,
            UUID agreementId,
            UUID investorId,
            String investorName,
            String agreementRef,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            BigDecimal profitShareRatio,
            BigDecimal profitSharePercent,
            String eligibilityConditions,
            UUID approvedBy,
            UUID evidenceFileId,
            Instant createdAt,
            boolean isInformationalOnly,
            String disclaimer
    ) {}

    public record InvestorCapitalBreakdownDto(
            UUID investorId,
            String investorName,
            BigDecimal totalCommittedValue,
            BigDecimal totalContributedValue,
            BigDecimal outstandingValue,
            BigDecimal capitalSharePercent,
            BigDecimal profitSharePercent,
            boolean isDifferentRatio
    ) {}

    public record CapitalSummaryDto(
            UUID branchId,
            BigDecimal totalCommittedValue,
            BigDecimal totalActualContributed,
            BigDecimal totalOutstandingValue,
            int investorCount,
            int agreementCount,
            boolean isInformationalOnly,
            String disclaimer,
            List<InvestorCapitalBreakdownDto> investorBreakdowns
    ) {}
}
