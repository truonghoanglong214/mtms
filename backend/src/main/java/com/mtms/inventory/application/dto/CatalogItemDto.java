package com.mtms.inventory.application.dto;

import com.mtms.inventory.domain.CatalogItem;
import com.mtms.inventory.domain.ItemKind;
import com.mtms.inventory.domain.ItemStatus;

import java.time.Instant;
import java.util.UUID;

public record CatalogItemDto(
        UUID id,
        UUID businessEntityId,
        String itemCode,
        String name,
        UUID baseUnitId,
        String baseUnitCode,
        String baseUnitName,
        ItemKind itemKind,
        boolean lotTrackingRequired,
        boolean expirationTrackingRequired,
        ItemStatus status,
        int packConversionCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static CatalogItemDto from(
            CatalogItem item,
            String baseUnitCode,
            String baseUnitName,
            int packConversionCount
    ) {
        return new CatalogItemDto(
                item.getId(),
                item.getBusinessEntityId(),
                item.getItemCode(),
                item.getName(),
                item.getBaseUnitId(),
                baseUnitCode,
                baseUnitName,
                item.getItemKind(),
                item.isLotTrackingRequired(),
                item.isExpirationTrackingRequired(),
                item.getStatus(),
                packConversionCount,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
