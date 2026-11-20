package com.mtms.inventory.web;

import com.mtms.inventory.application.dto.CreateSupplierCommand;
import com.mtms.inventory.application.dto.SupplierDto;
import com.mtms.inventory.application.service.SupplierService;
import com.mtms.inventory.domain.SupplierStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities/{entityId}/suppliers")
@PreAuthorize("@branchAuth.hasEntityAccess(#entityId)")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<List<SupplierDto>> getSuppliers(
            @PathVariable UUID entityId,
            @RequestParam(required = false) SupplierStatus status
    ) {
        return ResponseEntity.ok(supplierService.getSuppliers(entityId, status));
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierDto> getSupplierById(
            @PathVariable UUID entityId,
            @PathVariable UUID supplierId
    ) {
        return ResponseEntity.ok(supplierService.getSupplierById(entityId, supplierId));
    }

    @PostMapping
    public ResponseEntity<SupplierDto> createSupplier(
            @PathVariable UUID entityId,
            @Valid @RequestBody CreateSupplierCommand command
    ) {
        SupplierDto created = supplierService.createSupplier(entityId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
