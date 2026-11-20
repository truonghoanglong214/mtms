package com.mtms.asset.web;

import com.mtms.asset.application.AssetService;
import com.mtms.asset.application.dto.AssetDtos.AssetSummaryDto;
import com.mtms.asset.application.dto.AssetDtos.CreateEquipmentAssetRequest;
import com.mtms.asset.application.dto.AssetDtos.EquipmentAssetDto;
import com.mtms.asset.application.dto.AssetDtos.UpdateEquipmentAssetRequest;
import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<Page<EquipmentAssetDto>> getAssets(
            @PathVariable UUID branchId,
            @RequestParam(required = false) AssetCategory category,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(assetService.getAssets(branchId, category, status, search, pageable));
    }

    @GetMapping("/summary")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<AssetSummaryDto> getAssetSummary(@PathVariable UUID branchId) {
        return ResponseEntity.ok(assetService.getAssetSummary(branchId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<EquipmentAssetDto> getAssetById(
            @PathVariable UUID branchId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getAssetById(branchId, id));
    }

    @PostMapping
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<EquipmentAssetDto> createAsset(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateEquipmentAssetRequest request) {
        EquipmentAssetDto created = assetService.createAsset(branchId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<EquipmentAssetDto> updateAsset(
            @PathVariable UUID branchId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEquipmentAssetRequest request) {
        EquipmentAssetDto updated = assetService.updateAsset(branchId, id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId)")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable UUID branchId,
            @PathVariable UUID id) {
        assetService.deleteAsset(branchId, id);
        return ResponseEntity.noContent().build();
    }
}
