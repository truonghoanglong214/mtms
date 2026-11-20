package com.mtms.asset.infrastructure.persistence;

import com.mtms.asset.domain.model.AssetCategory;
import com.mtms.asset.domain.model.AssetStatus;
import com.mtms.asset.domain.model.EquipmentAsset;
import com.mtms.asset.domain.port.EquipmentAssetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EquipmentAssetRepositoryAdapter implements EquipmentAssetRepository {

    private final SpringDataEquipmentAssetRepository repository;

    public EquipmentAssetRepositoryAdapter(SpringDataEquipmentAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public EquipmentAsset save(EquipmentAsset asset) {
        EquipmentAssetJpaEntity entity = EquipmentAssetJpaEntity.fromDomain(asset);
        EquipmentAssetJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<EquipmentAsset> findById(UUID id) {
        return repository.findById(id).map(EquipmentAssetJpaEntity::toDomain);
    }

    @Override
    public Optional<EquipmentAsset> findByAssetCode(String assetCode) {
        return repository.findByAssetCode(assetCode).map(EquipmentAssetJpaEntity::toDomain);
    }

    @Override
    public boolean existsByAssetCode(String assetCode) {
        return repository.existsByAssetCode(assetCode);
    }

    @Override
    public boolean existsByAssetCodeAndIdNot(String assetCode, UUID id) {
        return repository.existsByAssetCodeAndIdNot(assetCode, id);
    }

    @Override
    public Page<EquipmentAsset> findByBranchId(UUID branchId, AssetCategory category, AssetStatus status, String search, Pageable pageable) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return repository.findByFilter(branchId, category, status, trimmedSearch, pageable)
                .map(EquipmentAssetJpaEntity::toDomain);
    }

    @Override
    public List<EquipmentAsset> findAllByBranchId(UUID branchId) {
        return repository.findAllByBranchIdOrderByCreatedAtDesc(branchId).stream()
                .map(EquipmentAssetJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countByBranchIdAndStatus(UUID branchId, AssetStatus status) {
        return repository.countByBranchIdAndStatus(branchId, status);
    }

    @Override
    public long countByBranchIdAndCategory(UUID branchId, AssetCategory category) {
        return repository.countByBranchIdAndCategory(branchId, category);
    }

    @Override
    public long countExpiringWarranty(UUID branchId, LocalDate today, LocalDate thresholdDate) {
        return repository.countExpiringWarranty(branchId, today, thresholdDate);
    }

    @Override
    public long countExpiredWarranty(UUID branchId, LocalDate today) {
        return repository.countExpiredWarranty(branchId, today);
    }

    @Override
    public BigDecimal calculateTotalAcquisitionCost(UUID branchId) {
        return repository.calculateTotalAcquisitionCost(branchId);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
