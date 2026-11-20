package com.mtms.finance.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFinancialCategoryCommand(
        @NotBlank(message = "Mã danh mục không được để trống")
        @Size(max = 60, message = "Mã danh mục tối đa 60 ký tự")
        String categoryCode,

        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 160, message = "Tên danh mục tối đa 160 ký tự")
        String categoryName,

        String description
) {}
