import { apiClient } from '@/shared/api/apiClient';
import {
  AssetCategory,
  AssetPageResponse,
  AssetStatus,
  AssetSummary,
  CreateEquipmentAssetRequest,
  EquipmentAsset,
  UpdateEquipmentAssetRequest,
} from '../types/asset.types';

function extractData<T>(res: { data: unknown }): T {
  if (
    res.data &&
    typeof res.data === 'object' &&
    'data' in res.data &&
    (res.data as { data: unknown }).data !== undefined
  ) {
    return (res.data as { data: T }).data;
  }
  return res.data as T;
}

export const assetApi = {
  getAssets: async (
    branchId: string,
    params?: {
      category?: AssetCategory;
      status?: AssetStatus;
      search?: string;
      page?: number;
      size?: number;
    }
  ): Promise<AssetPageResponse> => {
    const res = await apiClient.get(`/branches/${branchId}/assets`, {
      params,
    });
    return extractData<AssetPageResponse>(res);
  },

  getAssetSummary: async (branchId: string): Promise<AssetSummary> => {
    const res = await apiClient.get(`/branches/${branchId}/assets/summary`);
    return extractData<AssetSummary>(res);
  },

  getAssetById: async (branchId: string, id: string): Promise<EquipmentAsset> => {
    const res = await apiClient.get(`/branches/${branchId}/assets/${id}`);
    return extractData<EquipmentAsset>(res);
  },

  createAsset: async (
    branchId: string,
    payload: CreateEquipmentAssetRequest
  ): Promise<EquipmentAsset> => {
    const res = await apiClient.post(`/branches/${branchId}/assets`, payload);
    return extractData<EquipmentAsset>(res);
  },

  updateAsset: async (
    branchId: string,
    id: string,
    payload: UpdateEquipmentAssetRequest
  ): Promise<EquipmentAsset> => {
    const res = await apiClient.put(`/branches/${branchId}/assets/${id}`, payload);
    return extractData<EquipmentAsset>(res);
  },

  deleteAsset: async (branchId: string, id: string): Promise<void> => {
    await apiClient.delete(`/branches/${branchId}/assets/${id}`);
  },
};
