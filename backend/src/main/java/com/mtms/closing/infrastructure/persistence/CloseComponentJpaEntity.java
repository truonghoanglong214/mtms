package com.mtms.closing.infrastructure.persistence;

import com.mtms.closing.domain.model.CloseComponentCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * P0-CLS-03 / BR-CLS-02 / TC-041
 * JPA entity mapping mtms.close_components.
 */
@Entity
@Table(name = "close_components", schema = "mtms")
public class CloseComponentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "monthly_close_id", nullable = false)
    private UUID monthlyCloseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_close_id", insertable = false, updatable = false)
    private MonthlyCloseJpaEntity monthlyClose;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_code", nullable = false, length = 50)
    private CloseComponentCode componentCode;

    @Column(name = "signed_amount", nullable = false, precision = 22, scale = 2)
    private BigDecimal signedAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provenance", nullable = false)
    private String provenance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CloseComponentJpaEntity() {}

    public CloseComponentJpaEntity(
            UUID id,
            UUID monthlyCloseId,
            CloseComponentCode componentCode,
            BigDecimal signedAmount,
            String provenance,
            Instant createdAt
    ) {
        this.id = id;
        this.monthlyCloseId = monthlyCloseId;
        this.componentCode = componentCode;
        this.signedAmount = signedAmount;
        this.provenance = provenance;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMonthlyCloseId() {
        return monthlyCloseId;
    }

    public void setMonthlyCloseId(UUID monthlyCloseId) {
        this.monthlyCloseId = monthlyCloseId;
    }

    public MonthlyCloseJpaEntity getMonthlyClose() {
        return monthlyClose;
    }

    public void setMonthlyClose(MonthlyCloseJpaEntity monthlyClose) {
        this.monthlyClose = monthlyClose;
    }

    public CloseComponentCode getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(CloseComponentCode componentCode) {
        this.componentCode = componentCode;
    }

    public BigDecimal getSignedAmount() {
        return signedAmount;
    }

    public void setSignedAmount(BigDecimal signedAmount) {
        this.signedAmount = signedAmount;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
