package com.mtms.inventory.web;

import com.mtms.inventory.application.dto.CatalogItemDto;
import com.mtms.inventory.application.dto.CreateCatalogItemCommand;
import com.mtms.inventory.application.dto.UpdateCatalogItemCommand;
import com.mtms.inventory.application.service.CatalogItemService;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entities/{entityId}/items")
@PreAuthorize("@branchAuth.hasEntityAccess(#entityId)")
public class CatalogItemController {

    private final CatalogItemService catalogItemService;

    public CatalogItemController(CatalogItemService catalogItemService) {
        this.catalogItemService = catalogItemService;
    }

    @GetMapping
    public ResponseEntity<List<CatalogItemDto>> getItems(
            @PathVariable UUID entityId,
            @RequestParam(required = false) ItemKind itemKind,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(catalogItemService.getItems(entityId, itemKind, status, search));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<CatalogItemDto> getItemById(
            @PathVariable UUID entityId,
            @PathVariable UUID itemId
    ) {
        return ResponseEntity.ok(catalogItemService.getItemById(entityId, itemId));
    }

    @PostMapping
    public ResponseEntity<CatalogItemDto> createItem(
            @PathVariable UUID entityId,
            @Valid @RequestBody CreateCatalogItemCommand command
    ) {
        CatalogItemDto created = catalogItemService.createItem(entityId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<CatalogItemDto> updateItem(
            @PathVariable UUID entityId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCatalogItemCommand command
    ) {
        CatalogItemDto updated = catalogItemService.updateItem(entityId, itemId, command);
        return ResponseEntity.ok(updated);
    }
}
