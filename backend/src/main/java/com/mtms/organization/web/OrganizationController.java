package com.mtms.organization.web;

import com.mtms.common.api.ApiResponse;
import com.mtms.organization.application.OrganizationService;
import com.mtms.organization.application.dto.BranchDto;
import com.mtms.organization.application.dto.BusinessEntityDto;
import com.mtms.organization.application.dto.CreateBranchCommand;
import com.mtms.organization.application.dto.CreateBusinessEntityCommand;
import com.mtms.organization.domain.BranchStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping("/entities")
    public ResponseEntity<ApiResponse<BusinessEntityDto>> createEntity(
            @Valid @RequestBody CreateBusinessEntityCommand command) {
        BusinessEntityDto created = organizationService.createBusinessEntity(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Business entity created successfully"));
    }

    @GetMapping("/entities")
    public ResponseEntity<ApiResponse<List<BusinessEntityDto>>> getAllEntities() {
        List<BusinessEntityDto> list = organizationService.getAllBusinessEntities();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/entities/{id}")
    public ResponseEntity<ApiResponse<BusinessEntityDto>> getEntityById(@PathVariable UUID id) {
        BusinessEntityDto entity = organizationService.getBusinessEntityById(id);
        return ResponseEntity.ok(ApiResponse.ok(entity));
    }

    public record CreateBranchRequest(
            @NotBlank(message = "Branch code must not be blank")
            @Size(max = 40, message = "Branch code must not exceed 40 characters")
            String branchCode,

            @NotBlank(message = "Branch name must not be blank")
            @Size(max = 160, message = "Branch name must not exceed 160 characters")
            String name,

            @Size(max = 64, message = "Time zone must not exceed 64 characters")
            String timeZone,

            @NotNull(message = "Branch status must be specified")
            BranchStatus status
    ) {
    }

    @PostMapping("/entities/{entityId}/branches")
    public ResponseEntity<ApiResponse<BranchDto>> createBranch(
            @PathVariable UUID entityId,
            @Valid @RequestBody CreateBranchRequest request) {
        CreateBranchCommand command = new CreateBranchCommand(
                entityId,
                request.branchCode(),
                request.name(),
                request.timeZone(),
                request.status()
        );
        BranchDto created = organizationService.createBranch(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Branch created successfully"));
    }

    @GetMapping("/entities/{entityId}/branches")
    public ResponseEntity<ApiResponse<List<BranchDto>>> getBranchesByEntity(@PathVariable UUID entityId) {
        List<BranchDto> branches = organizationService.getBranchesByEntity(entityId);
        return ResponseEntity.ok(ApiResponse.ok(branches));
    }

    @GetMapping("/entities/{entityId}/branches/{branchId}")
    @PreAuthorize("@branchAuth.hasBranchAccess(#branchId, 'READ')")
    public ResponseEntity<ApiResponse<BranchDto>> getBranchById(
            @PathVariable UUID entityId,
            @PathVariable UUID branchId) {
        BranchDto branch = organizationService.getBranchById(entityId, branchId);
        return ResponseEntity.ok(ApiResponse.ok(branch));
    }
}
