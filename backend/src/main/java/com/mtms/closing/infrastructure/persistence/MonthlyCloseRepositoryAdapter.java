package com.mtms.closing.infrastructure.persistence;

import com.mtms.closing.domain.model.CloseComponent;
import com.mtms.closing.domain.model.CloseStatus;
import com.mtms.closing.domain.model.MonthlyClose;
import com.mtms.closing.domain.port.MonthlyCloseRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MonthlyCloseRepositoryAdapter implements MonthlyCloseRepository {

    private final SpringDataMonthlyCloseRepository monthlyCloseRepository;

    public MonthlyCloseRepositoryAdapter(SpringDataMonthlyCloseRepository monthlyCloseRepository) {
        this.monthlyCloseRepository = monthlyCloseRepository;
    }

    @Override
    public MonthlyClose save(MonthlyClose domain) {
        MonthlyCloseJpaEntity entity = toEntity(domain);

        if (domain.components() != null && !domain.components().isEmpty()) {
            for (CloseComponent comp : domain.components()) {
                CloseComponentJpaEntity compEntity = new CloseComponentJpaEntity(
                        comp.id() != null ? comp.id() : UUID.randomUUID(),
                        entity.getId(),
                        comp.componentCode(),
                        comp.signedAmount(),
                        comp.provenance(),
                        comp.createdAt()
                );
                entity.addComponent(compEntity);
            }
        }

        MonthlyCloseJpaEntity saved = monthlyCloseRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<MonthlyClose> findCurrentByBranchIdAndMonthStart(UUID branchId, LocalDate monthStart) {
        return monthlyCloseRepository.findByBranchIdAndMonthStartAndIsCurrentTrue(branchId, monthStart)
                .map(this::toDomain);
    }

    @Override
    public List<MonthlyClose> findByBranchIdOrderByMonthStartDesc(UUID branchId) {
        return monthlyCloseRepository.findByBranchIdOrderByMonthStartDescRevisionDesc(branchId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsClosedByBranchIdAndMonthStart(UUID branchId, LocalDate monthStart) {
        return monthlyCloseRepository.existsByBranchIdAndMonthStartAndStatus(branchId, monthStart, CloseStatus.CLOSED);
    }

    private MonthlyCloseJpaEntity toEntity(MonthlyClose domain) {
        return new MonthlyCloseJpaEntity(
                domain.id(),
                domain.branchId(),
                domain.monthStart(),
                domain.revision(),
                domain.isCurrent(),
                domain.status(),
                domain.managerialProfit(),
                domain.isProvisional(),
                domain.closedBy(),
                domain.closedAt(),
                domain.snapshot(),
                domain.note(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }

    private MonthlyClose toDomain(MonthlyCloseJpaEntity entity) {
        List<CloseComponent> components = entity.getComponents() != null
                ? entity.getComponents().stream()
                .map(c -> new CloseComponent(
                        c.getId(),
                        c.getMonthlyCloseId(),
                        c.getComponentCode(),
                        c.getSignedAmount(),
                        c.getProvenance(),
                        c.getCreatedAt()
                ))
                .toList()
                : List.of();

        return new MonthlyClose(
                entity.getId(),
                entity.getBranchId(),
                entity.getMonthStart(),
                entity.getRevision(),
                entity.getIsCurrent(),
                entity.getStatus(),
                entity.getManagerialProfit(),
                entity.getIsProvisional(),
                entity.getClosedBy(),
                entity.getClosedAt(),
                entity.getSnapshot(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                components
        );
    }
}
