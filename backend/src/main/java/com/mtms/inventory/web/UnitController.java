package com.mtms.inventory.web;

import com.mtms.inventory.application.dto.CreateUnitCommand;
import com.mtms.inventory.application.dto.UnitDto;
import com.mtms.inventory.application.service.UnitService;
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
@RequestMapping("/api/v1/units")
public class UnitController {

    private final UnitService unitService;

    public UnitController(UnitService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public ResponseEntity<List<UnitDto>> getAllUnits() {
        return ResponseEntity.ok(unitService.getAllUnits());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitDto> getUnitById(@PathVariable UUID id) {
        return ResponseEntity.ok(unitService.getUnitById(id));
    }

    @PostMapping
    @PreAuthorize("@branchAuth.isSystemAdmin()")
    public ResponseEntity<UnitDto> createUnit(@Valid @RequestBody CreateUnitCommand command) {
        UnitDto created = unitService.createUnit(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
