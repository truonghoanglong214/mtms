/**
 * TypeScript Data Transfer Objects and Commands for P0-INV-01 (Catalog & Pack Conversions).
 * Strictly mirrors docs/api/contracts/P0-INV-01.md.
 */

export type UnitDimension = 'MASS' | 'VOLUME' | 'COUNT' | 'OTHER'

export interface UnitDto {
  id: string
  unitCode: string
  unitName: string
  dimension: UnitDimension
  createdAt?: string
}

export interface CreateUnitCommand {
  unitCode: string
  unitName: string
  dimension: UnitDimension
}

export type SupplierStatus = 'ACTIVE' | 'INACTIVE'

export interface SupplierDto {
  id: string
  businessEntityId: string
  supplierCode: string
  name: string
  contactNote: string | null
  status: SupplierStatus
  createdAt: string
}

export interface CreateSupplierCommand {
  supplierCode: string
  name: string
  contactNote?: string | null
  status?: SupplierStatus
}

export type ItemKind = 'INGREDIENT' | 'CONSUMABLE' | 'EQUIPMENT' | 'OTHER'
export type ItemStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'

export interface CatalogItemDto {
  id: string
  businessEntityId: string
  itemCode: string
  name: string
  baseUnitId: string
  baseUnitCode: string
  baseUnitName: string
  itemKind: ItemKind
  lotTrackingRequired: boolean
  expirationTrackingRequired: boolean
  status: ItemStatus
  packConversionCount?: number
  createdAt: string
  updatedAt: string
}

export interface CreateCatalogItemCommand {
  itemCode: string
  name: string
  baseUnitId: string
  itemKind: ItemKind
  lotTrackingRequired: boolean
  expirationTrackingRequired: boolean
}

export interface UpdateCatalogItemCommand {
  name: string
  itemKind: ItemKind
  lotTrackingRequired: boolean
  expirationTrackingRequired: boolean
  status: ItemStatus
}

export interface CatalogItemFilterParams {
  itemKind?: ItemKind
  status?: ItemStatus
  search?: string
}

export interface SupplierPackConversionDto {
  id: string
  businessEntityId: string
  supplierId: string
  supplierName?: string
  itemId: string
  itemCode?: string
  itemName?: string
  packName: string
  baseUnitsPerPack: string
  effectiveFrom: string
  effectiveTo: string | null
  createdAt: string
}

export interface CreatePackConversionCommand {
  supplierId: string
  packName: string
  baseUnitsPerPack: string
  effectiveFrom: string
  effectiveTo?: string | null
}

export interface PackConversionCalculationRequest {
  itemId: string
  supplierId: string
  packQuantity: string
  businessDate: string
}

export interface PackConversionCalculationResponse {
  itemId: string
  itemCode: string
  itemName: string
  supplierId: string
  supplierName: string
  packName: string
  packQuantity: string
  baseUnitsPerPack: string
  baseUnitCode: string
  baseUnitName: string
  calculatedBaseQuantity: string
  conversionSnapshot: string
  formulaExplanation: string
}
