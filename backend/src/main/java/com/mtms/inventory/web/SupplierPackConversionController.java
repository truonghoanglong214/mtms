package com.mtms.inventory.web;

import com.mtms.inventory.application.dto.CreateSupplierPackConversionCommand;
import com.mtms.inventory.application.dto.PackConversionCalculationRequest;
import com.mtms.inventory.application.dto.PackConversionCalculationResponse;
import com.mtms.inventory.application.dto.SupplierPackConversionDto;
import com.mtms.inventory.application.service.SupplierPackConversionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities/{entityId}")
@PreAuthorize("@branchAuth.hasEntityAccess(#entityId)")
public class SupplierPackConversionController {

    private final SupplierPackConversionService conversionService;

    public SupplierPackConversionController(SupplierPackConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/items/{itemId}/pack-conversions")
    public ResponseEntity<List<SupplierPackConversionDto>> getPackConversionsForItem(
            @PathVariable UUID entityId,
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(conversionService.getPackConversionsForItem(entityId, itemId));
    }

    @PostMapping("/items/{itemId}/pack-conversions")
    public ResponseEntity<SupplierPackConversionDto> createPackConversion(
            @PathVariable UUID entityId,
            @PathVariable UUID itemId,
            @Valid @RequestBody CreateSupplierPackConversionCommand command
    ) {
        SupplierPackConversionDto created = conversionService.createPackConversion(entityId, itemId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/pack-conversions/calculate")
    public ResponseEntity<PackConversionCalculationResponse> calculatePackConversion(
            @PathVariable UUID entityId,
            @Valid @RequestBody PackConversionCalculationRequest request
    ) {
        PackConversionCalculationResponse response = conversionService.calculatePackConversion(entityId, request);
        return ResponseEntity.ok(response);
    }
}
