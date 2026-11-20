package com.mtms.finance.application.dto;

import com.mtms.finance.domain.AccountKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFinancialAccountCommand(
        @NotBlank(message = "Tên tài khoản không được để trống")
        @Size(max = 160, message = "Tên tài khoản tối đa 160 ký tự")
        String accountName,

        @NotNull(message = "Loại tài khoản không được để trống")
        AccountKind accountKind,

        UUID bankAccountId
) {}
