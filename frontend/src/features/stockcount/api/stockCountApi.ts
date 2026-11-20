import { apiClient } from '@/shared/api/apiClient';
import {
  CreateStockCountRequest,
  StockCountDetailDto,
  StockCountListResponse,
  StockCountStatus,
  StockCountSummaryDto,
  StockCountType,
  UpdateStockCountRequest
} from '../types/stockCount.types';

export const stockCountApi = {
  createStockCount: async (branchId: string, request: CreateStockCountRequest): Promise<StockCountDetailDto> => {
    const response = await apiClient.post<StockCountDetailDto>(
      `/api/v1/branches/${branchId}/stock-counts`,
      request
    );
    return response.data;
  },

  updateCounts: async (
    branchId: string,
    countId: string,
    request: UpdateStockCountRequest
  ): Promise<StockCountDetailDto> => {
    const response = await apiClient.put<StockCountDetailDto>(
      `/api/v1/branches/${branchId}/stock-counts/${countId}`,
      request
    );
    return response.data;
  },

  submitStockCount: async (branchId: string, countId: string): Promise<StockCountDetailDto> => {
    const response = await apiClient.post<StockCountDetailDto>(
      `/api/v1/branches/${branchId}/stock-counts/${countId}/submit`
    );
    return response.data;
  },

  approveAndPostStockCount: async (branchId: string, countId: string): Promise<StockCountDetailDto> => {
    const response = await apiClient.post<StockCountDetailDto>(
      `/api/v1/branches/${branchId}/stock-counts/${countId}/approve`
    );
    return response.data;
  },

  cancelStockCount: async (branchId: string, countId: string): Promise<void> => {
    await apiClient.post(`/api/v1/branches/${branchId}/stock-counts/${countId}/cancel`);
  },

  getStockCounts: async (
    branchId: string,
    params?: {
      status?: StockCountStatus;
      countType?: StockCountType;
      startDate?: string;
      endDate?: string;
      search?: string;
      page?: number;
      size?: number;
    }
  ): Promise<StockCountListResponse> => {
    const response = await apiClient.get<StockCountListResponse>(
      `/api/v1/branches/${branchId}/stock-counts`,
      { params }
    );
    return response.data;
  },

  getStockCountDetail: async (branchId: string, countId: string): Promise<StockCountDetailDto> => {
    const response = await apiClient.get<StockCountDetailDto>(
      `/api/v1/branches/${branchId}/stock-counts/${countId}`
    );
    return response.data;
  },

  getStockCountSummary: async (branchId: string): Promise<StockCountSummaryDto> => {
    const response = await apiClient.get<StockCountSummaryDto>(
      `/api/v1/branches/${branchId}/stock-counts/summary`
    );
    return response.data;
  }
};
