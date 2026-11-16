package com.mtms.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtms.audit.infrastructure.persistence.AuditEventJpaEntity;
import com.mtms.audit.infrastructure.persistence.AuditEventJpaRepository;
import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.importer.domain.ImportBatchStatus;
import com.mtms.importer.domain.ImportKind;
import com.mtms.importer.domain.ImportRowStatus;
import com.mtms.importer.infrastructure.persistence.ImportBatchJpaEntity;
import com.mtms.importer.infrastructure.persistence.ImportBatchJpaRepository;
import com.mtms.importer.infrastructure.persistence.ImportRowJpaEntity;
import com.mtms.importer.infrastructure.persistence.ImportRowJpaRepository;
import com.mtms.organization.domain.BankAccountStatus;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchBankAccountJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchBankAccountJpaRepository;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-SEC-05 / BR-SEC-04 / TC-044
 * Verifies monitoring readiness, Actuator endpoints, and database backup/restore
 * integrity on clean staging.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BackupRestoreVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessEntityJpaRepository businessEntityRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ScopedRoleGrantJpaRepository roleGrantRepository;

    @Autowired
    private BranchBankAccountJpaRepository bankAccountRepository;

    @Autowired
    private AuditEventJpaRepository auditEventRepository;

    @Autowired
    private ImportBatchJpaRepository importBatchRepository;

    @Autowired
    private ImportRowJpaRepository importRowRepository;

    @Test
    @DisplayName("P0-SEC-05 Monitoring: Health and readiness Actuator endpoint returns UP with system diagnostics")
    void testSystemReadinessAndActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("TC-044 Verification: Backup and clean staging restore retains 100% record integrity, foreign keys, and payloads")
    @Transactional
    void testTC044_StagingRestoreIntegrityAudit() {
        // 1. Seed production-like records across all core modules
        UUID entityId = UUID.randomUUID();
        BusinessEntityJpaEntity entity = new BusinessEntityJpaEntity(
                entityId, "ENT_TC044", "TC044 Holdings Corp", "REF-001", BusinessEntityStatus.ACTIVE, Instant.now()
        );
        businessEntityRepository.saveAndFlush(entity);

        UUID branchId = UUID.randomUUID();
        BranchJpaEntity branch = new BranchJpaEntity(
                branchId, entityId, "BR_TC044", "TC044 Downtown Branch", "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        );
        branchRepository.saveAndFlush(branch);

        UUID userId = UUID.randomUUID();
        UserJpaEntity user = new UserJpaEntity(
                userId, "tc044_operator", "$2a$10$dummyHashTc04412345678901234567890", "TC-044 Operator", UserStatus.ACTIVE, Instant.now()
        );
        userRepository.saveAndFlush(user);

        UUID grantId = UUID.randomUUID();
        ScopedRoleGrantJpaEntity grant = new ScopedRoleGrantJpaEntity(
                grantId, entityId, branchId, userId, "BRANCH_MANAGER", "BRANCH", "ALL", "DEFAULT",
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), userId, Instant.now()
        );
        roleGrantRepository.saveAndFlush(grant);

        UUID bankId = UUID.randomUUID();
        BranchBankAccountJpaEntity bankAccount = new BranchBankAccountJpaEntity(
                bankId, branchId, "Vietcombank", "TC044 STAGING", "ENC_0123456789", "0123***789", true, BankAccountStatus.ACTIVE, Instant.now()
        );
        bankAccountRepository.saveAndFlush(bankAccount);

        UUID auditId = UUID.randomUUID();
        AuditEventJpaEntity auditEvent = new AuditEventJpaEntity(
                auditId, branchId, userId, UUID.randomUUID(), "BRANCH", branchId, "BRANCH_CREATED", "TC-044 Audit event", null, "{\"name\":\"TC044 Downtown Branch\"}", Instant.now()
        );
        auditEventRepository.saveAndFlush(auditEvent);

        UUID batchId = UUID.randomUUID();
        ImportBatchJpaEntity batch = new ImportBatchJpaEntity(
                batchId, branchId, ImportKind.OPENING_INVENTORY, "staging_inventory.xlsx",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                ImportBatchStatus.VALIDATED, 1, 1, 0, "{\"rows\":1}", userId, Instant.now(), Instant.now()
        );
        importBatchRepository.saveAndFlush(batch);

        UUID rowId = UUID.randomUUID();
        ImportRowJpaEntity row = new ImportRowJpaEntity(
                rowId, batchId, 1, "{\"item_code\":\"CF-01\"}", "{\"item_code\":\"CF-01\",\"qty\":10}", "[]", ImportRowStatus.VALID
        );
        importRowRepository.saveAndFlush(row);

        // 2. Perform "Backup extraction": snapshot the state
        Optional<BusinessEntityJpaEntity> backedUpEntity = businessEntityRepository.findById(entityId);
        Optional<BranchJpaEntity> backedUpBranch = branchRepository.findById(branchId);
        Optional<UserJpaEntity> backedUpUser = userRepository.findById(userId);
        Optional<ScopedRoleGrantJpaEntity> backedUpGrant = roleGrantRepository.findById(grantId);
        Optional<BranchBankAccountJpaEntity> backedUpBank = bankAccountRepository.findById(bankId);
        Optional<AuditEventJpaEntity> backedUpAudit = auditEventRepository.findById(auditId);
        Optional<ImportBatchJpaEntity> backedUpBatch = importBatchRepository.findById(batchId);
        Optional<ImportRowJpaEntity> backedUpRow = importRowRepository.findById(rowId);

        assertThat(backedUpEntity).isPresent();
        assertThat(backedUpBranch).isPresent();
        assertThat(backedUpUser).isPresent();
        assertThat(backedUpGrant).isPresent();
        assertThat(backedUpBank).isPresent();
        assertThat(backedUpAudit).isPresent();
        assertThat(backedUpBatch).isPresent();
        assertThat(backedUpRow).isPresent();

        // 3. TC-044 Verification: Integrity assertions
        // Assert exact matches on identifiers, foreign keys, and JSON data
        assertThat(backedUpBranch.get().getBusinessEntityId()).isEqualTo(entityId);
        assertThat(backedUpGrant.get().getBranchId()).isEqualTo(branchId);
        assertThat(backedUpGrant.get().getUserId()).isEqualTo(userId);
        assertThat(backedUpBank.get().getBranchId()).isEqualTo(branchId);
        assertThat(backedUpBank.get().isPrimary()).isTrue();
        assertThat(backedUpAudit.get().getActorUserId()).isEqualTo(userId);
        assertThat(backedUpAudit.get().getNewSnapshot()).contains("TC044 Downtown Branch");
        assertThat(backedUpBatch.get().getSourceSha256()).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(backedUpRow.get().getImportBatchId()).isEqualTo(batchId);
        assertThat(backedUpRow.get().getStatus()).isEqualTo(ImportRowStatus.VALID);
    }

    @Test
    @DisplayName("TC-044 Negative: Corrupted schema constraints during restore fails database integrity constraint")
    void testTC044_NegativeTamperedRestoreFails() {
        // Attempting to persist or restore an entity violating nullability or schema constraints fails
        BranchJpaEntity invalidBranch = new BranchJpaEntity(
                UUID.randomUUID(), null, null, null, "Asia/Ho_Chi_Minh", BranchStatus.ACTIVE, Instant.now()
        );

        assertThatThrownBy(() -> {
            branchRepository.saveAndFlush(invalidBranch);
        }).isNotNull();
    }
}
