package com.mtms.identity.infrastructure.bootstrap;

import com.mtms.identity.domain.UserStatus;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaEntity;
import com.mtms.identity.infrastructure.persistence.ScopedRoleGrantJpaRepository;
import com.mtms.identity.infrastructure.persistence.UserJpaEntity;
import com.mtms.identity.infrastructure.persistence.UserJpaRepository;
import com.mtms.organization.domain.BranchStatus;
import com.mtms.organization.domain.BusinessEntityStatus;
import com.mtms.organization.infrastructure.persistence.BranchJpaEntity;
import com.mtms.organization.infrastructure.persistence.BranchJpaRepository;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaEntity;
import com.mtms.organization.infrastructure.persistence.BusinessEntityJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Developer Superpower: Automatic sample data seeding for dev profile.
 * Only runs if the users table is completely empty.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserJpaRepository userRepository;
    private final ScopedRoleGrantJpaRepository grantRepository;
    private final BusinessEntityJpaRepository entityRepository;
    private final BranchJpaRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UserJpaRepository userRepository,
                         ScopedRoleGrantJpaRepository grantRepository,
                         BusinessEntityJpaRepository entityRepository,
                         BranchJpaRepository branchRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.grantRepository = grantRepository;
        this.entityRepository = entityRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("==> [DevDataSeeder] Initializing sample data for dev profile...");

        // 1. Create Business Entity
        UUID entityId = UUID.randomUUID();
        BusinessEntityJpaEntity entity = new BusinessEntityJpaEntity(
                entityId,
                "LTT_VN",
                "Cong ty CP Luc Tra Tinh Hoa",
                "0109999999",
                BusinessEntityStatus.ACTIVE,
                Instant.now()
        );
        entityRepository.save(entity);

        // 2. Create Branches
        UUID branch1Id = UUID.randomUUID();
        BranchJpaEntity branch1 = new BranchJpaEntity(
                branch1Id,
                entityId,
                "LTT_HCM_01",
                "Luc Tra Tinh Hoa - Quan 1",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branch1);

        UUID branch2Id = UUID.randomUUID();
        BranchJpaEntity branch2 = new BranchJpaEntity(
                branch2Id,
                entityId,
                "LTT_HCM_02",
                "Luc Tra Tinh Hoa - Binh Thanh",
                "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE,
                Instant.now()
        );
        branchRepository.save(branch2);

        // 3. Create System Admin
        UUID adminId = UUID.randomUUID();
        UserJpaEntity admin = new UserJpaEntity(
                adminId,
                "admin",
                passwordEncoder.encode("Admin@123456"),
                "Quan Tri Vien He Thong",
                UserStatus.ACTIVE,
                Instant.now()
        );
        userRepository.save(admin);

        // Grant SYSTEM_ADMIN
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                null,
                adminId,
                "SYSTEM_ADMIN",
                "*",
                "ALL",
                "ALL",
                LocalDate.now().minusDays(1),
                null,
                adminId,
                Instant.now()
        ));

        // 4. Create Branch Manager
        UUID managerId = UUID.randomUUID();
        UserJpaEntity manager = new UserJpaEntity(
                managerId,
                "manager_q1",
                passwordEncoder.encode("Manager@123456"),
                "Nguyen Van Quan Ly (Q1)",
                UserStatus.ACTIVE,
                Instant.now()
        );
        userRepository.save(manager);

        // Grant MANAGER on branch1
        grantRepository.save(new ScopedRoleGrantJpaEntity(
                UUID.randomUUID(),
                entityId,
                branch1Id,
                managerId,
                "MANAGER",
                "*",
                "ALL",
                "ALL",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(6),
                adminId,
                Instant.now()
        ));

        log.info("==> [DevDataSeeder] Sample data initialized successfully:");
        log.info("==> [DevDataSeeder] - System Admin: username='admin', password='Admin@123456'");
        log.info("==> [DevDataSeeder] - Manager:      username='manager_q1', password='Manager@123456'");
    }
}
