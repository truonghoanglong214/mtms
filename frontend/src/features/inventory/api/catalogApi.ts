import { apiClient } from '@/shared/api/apiClient'
import type {
  UnitDto,
  CreateUnitCommand,
  SupplierDto,
  CreateSupplierCommand,
  SupplierStatus,
  CatalogItemDto,
  CreateCatalogItemCommand,
  UpdateCatalogItemCommand,
  CatalogItemFilterParams,
  SupplierPackConversionDto,
  CreatePackConversionCommand,
  PackConversionCalculationRequest,
  PackConversionCalculationResponse,
} from '../types/catalog.types'

/**
 * Extracts payload whether backend wraps response in ApiResponse<T> or returns raw DTO.
 */
function extractData<T>(res: { data: unknown }): T {
  if (
    res.data &&
    typeof res.data === 'object' &&
    'data' in res.data &&
    (res.data as { data: unknown }).data !== undefined
  ) {
    return (res.data as { data: T }).data
  }
  return res.data as T
}

export const catalogApi = {
  // --- Canonical Units ---
  async getUnits(): Promise<UnitDto[]> {
    const res = await apiClient.get('/units')
    return extractData<UnitDto[]>(res)
  },

  async createUnit(data: CreateUnitCommand): Promise<UnitDto> {
    const res = await apiClient.post('/units', data)
    return extractData<UnitDto>(res)
  },

  // --- Suppliers ---
  async getSuppliers(entityId: string, status?: SupplierStatus): Promise<SupplierDto[]> {
    const res = await apiClient.get(`/entities/${entityId}/suppliers`, {
      params: status ? { status } : undefined,
    })
    return extractData<SupplierDto[]>(res)
  },

  async createSupplier(entityId: string, data: CreateSupplierCommand): Promise<SupplierDto> {
    const res = await apiClient.post(`/entities/${entityId}/suppliers`, data)
    return extractData<SupplierDto>(res)
  },

  // --- Catalog Items ---
  async getCatalogItems(
    entityId: string,
    params?: CatalogItemFilterParams
  ): Promise<CatalogItemDto[]> {
    const res = await apiClient.get(`/entities/${entityId}/items`, {
      params,
    })
    return extractData<CatalogItemDto[]>(res)
  },

  async getCatalogItemById(entityId: string, itemId: string): Promise<CatalogItemDto> {
    const res = await apiClient.get(`/entities/${entityId}/items/${itemId}`)
    return extractData<CatalogItemDto>(res)
  },

  async createCatalogItem(
    entityId: string,
    data: CreateCatalogItemCommand
  ): Promise<CatalogItemDto> {
    const res = await apiClient.post(`/entities/${entityId}/items`, data)
    return extractData<CatalogItemDto>(res)
  },

  async updateCatalogItem(
    entityId: string,
    itemId: string,
    data: UpdateCatalogItemCommand
  ): Promise<CatalogItemDto> {
    const res = await apiClient.put(`/entities/${entityId}/items/${itemId}`, data)
    return extractData<CatalogItemDto>(res)
  },

  // --- Supplier Pack Conversions (TC-023) ---
  async getPackConversions(
    entityId: string,
    itemId: string
  ): Promise<SupplierPackConversionDto[]> {
    const res = await apiClient.get(`/entities/${entityId}/items/${itemId}/pack-conversions`)
    return extractData<SupplierPackConversionDto[]>(res)
  },

  async createPackConversion(
    entityId: string,
    itemId: string,
    data: CreatePackConversionCommand
  ): Promise<SupplierPackConversionDto> {
    const res = await apiClient.post(
      `/entities/${entityId}/items/${itemId}/pack-conversions`,
      data
    )
    return extractData<SupplierPackConversionDto>(res)
  },

  /**
   * TC-023 Live Calculator API
   * Multiplies pack quantity by baseUnitsPerPack and returns calculation with audit snapshot
   */
  async calculatePackConversion(
    entityId: string,
    data: PackConversionCalculationRequest
  ): Promise<PackConversionCalculationResponse> {
    const res = await apiClient.post(
      `/entities/${entityId}/pack-conversions/calculate`,
      data
    )
    return extractData<PackConversionCalculationResponse>(res)
  },
}
