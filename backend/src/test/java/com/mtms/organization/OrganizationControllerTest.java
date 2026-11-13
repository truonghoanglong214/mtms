package com.mtms.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.common.error.GlobalExceptionHandler;
import com.mtms.organization.application.OrganizationService;
import com.mtms.organization.application.dto.BranchDto;
import com.mtms.organization.application.dto.BusinessEntityDto;
import com.mtms.organization.application.dto.CreateBranchCommand;
import com.mtms.organization.application.dto.CreateBusinessEntityCommand;
import com.mtms.organization.application.exception.DuplicateEntityCodeException;
import com.mtms.organization.application.exception.EntityNotFoundException;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.web.OrganizationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrganizationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.mtms.identity.infrastructure.security.SecurityConfig.class),
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;

    @Test
    @DisplayName("POST /entities should return 201 Created on valid input")
    void testCreateEntitySuccess() throws Exception {
        CreateBusinessEntityCommand cmd = new CreateBusinessEntityCommand(
                "ENT_01",
                "Cong ty Tra Sua Sai Gon",
                "DKKD-123",
                BusinessEntityStatus.ACTIVE
        );

        UUID entityId = UUID.randomUUID();
        BusinessEntityDto responseDto = new BusinessEntityDto(
                entityId,
                "ENT_01",
                "Cong ty Tra Sua Sai Gon",
                "DKKD-123",
                BusinessEntityStatus.ACTIVE,
                Instant.now()
        );

        when(organizationService.createBusinessEntity(any(CreateBusinessEntityCommand.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/organizations/entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entityCode").value("ENT_01"))
                .andExpect(jsonPath("$.data.registeredName").value("Cong ty Tra Sua Sai Gon"));
    }

    @Test
    @DisplayName("POST /entities should return 400 Bad Request when entityCode is blank")
    void testCreateEntityBlankCodeValidation() throws Exception {
        CreateBusinessEntityCommand cmd = new CreateBusinessEntityCommand(
                "", // Blank code
                "Cong ty Valid Name",
                null,
                BusinessEntityStatus.ACTIVE
        );

        mockMvc.perform(post("/api/v1/organizations/entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.entityCode").exists());
    }

    @Test
    @DisplayName("POST /entities should return 409 Conflict when entityCode already exists")
    void testCreateEntityDuplicateCodeConflict() throws Exception {
        CreateBusinessEntityCommand cmd = new CreateBusinessEntityCommand(
                "ENT_EXISTING",
                "Cong ty Valid Name",
                null,
                BusinessEntityStatus.ACTIVE
        );

        when(organizationService.createBusinessEntity(any(CreateBusinessEntityCommand.class)))
                .thenThrow(new DuplicateEntityCodeException("ENT_EXISTING"));

        mockMvc.perform(post("/api/v1/organizations/entities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Business entity with code 'ENT_EXISTING' already exists"));
    }

    @Test
    @DisplayName("GET /entities/{id} should return 404 Not Found when entity does not exist")
    void testGetEntityNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();

        when(organizationService.getBusinessEntityById(missingId))
                .thenThrow(new EntityNotFoundException("BusinessEntity", missingId));

        mockMvc.perform(get("/api/v1/organizations/entities/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    @DisplayName("POST /entities/{entityId}/branches should return 201 Created on valid input")
    void testCreateBranchSuccess() throws Exception {
        UUID entityId = UUID.randomUUID();
        OrganizationController.CreateBranchRequest request = new OrganizationController.CreateBranchRequest(
                "BR_01",
                "Chi nhanh 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE
        );

        BranchDto branchDto = new BranchDto(
                UUID.randomUUID(),
                entityId,
                "BR_01",
                "Chi nhanh 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );

        when(organizationService.createBranch(any(CreateBranchCommand.class)))
                .thenReturn(branchDto);

        mockMvc.perform(post("/api/v1/organizations/entities/{entityId}/branches", entityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchCode").value("BR_01"))
                .andExpect(jsonPath("$.data.name").value("Chi nhanh 1"));
    }

    @Test
    @DisplayName("GET /entities/{entityId}/branches should return 200 OK with list of branches")
    void testGetBranchesByEntity() throws Exception {
        UUID entityId = UUID.randomUUID();
        BranchDto branchDto = new BranchDto(
                UUID.randomUUID(),
                entityId,
                "BR_01",
                "Chi nhanh 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );

        when(organizationService.getBranchesByEntity(entityId))
                .thenReturn(List.of(branchDto));

        mockMvc.perform(get("/api/v1/organizations/entities/{entityId}/branches", entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchCode").value("BR_01"));
    }
}
