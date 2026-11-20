import { apiClient } from '@/shared/api/apiClient';
import {
  CreateTransferPayload,
  ReceiveTransferPayload,
  StockTransfer,
  TransferListResponse,
  TransferStatus,
  TransferSummary
} from '../types/movement.types';

export const movementApi = {
  createTransfer: async (branchId: string, payload: CreateTransferPayload): Promise<StockTransfer> => {
    const res = await apiClient.post<StockTransfer>(`/branches/${branchId}/transfers`, payload);
    return res.data;
  },

  dispatchTransfer: async (branchId: string, transferId: string): Promise<StockTransfer> => {
    const res = await apiClient.post<StockTransfer>(`/branches/${branchId}/transfers/${transferId}/dispatch`);
    return res.data;
  },

  receiveTransfer: async (
    branchId: string,
    transferId: string,
    payload: ReceiveTransferPayload
  ): Promise<StockTransfer> => {
    const res = await apiClient.post<StockTransfer>(
      `/branches/${branchId}/transfers/${transferId}/receive`,
      payload
    );
    return res.data;
  },

  cancelTransfer: async (branchId: string, transferId: string): Promise<void> => {
    await apiClient.post(`/branches/${branchId}/transfers/${transferId}/cancel`);
  },

  getOutboundTransfers: async (
    branchId: string,
    params?: {
      status?: TransferStatus;
      startDate?: string;
      endDate?: string;
      search?: string;
      page?: number;
      size?: number;
    }
  ): Promise<TransferListResponse> => {
    const res = await apiClient.get<TransferListResponse>(`/branches/${branchId}/transfers/outbound`, { params });
    return res.data;
  },

  getInboundTransfers: async (
    branchId: string,
    params?: {
      status?: TransferStatus;
      startDate?: string;
      endDate?: string;
      search?: string;
      page?: number;
      size?: number;
    }
  ): Promise<TransferListResponse> => {
    const res = await apiClient.get<TransferListResponse>(`/branches/${branchId}/transfers/inbound`, { params });
    return res.data;
  },

  getTransferDetail: async (branchId: string, transferId: string): Promise<StockTransfer> => {
    const res = await apiClient.get<StockTransfer>(`/branches/${branchId}/transfers/${transferId}`);
    return res.data;
  },

  getTransferSummary: async (branchId: string): Promise<TransferSummary> => {
    const res = await apiClient.get<TransferSummary>(`/branches/${branchId}/transfers/summary`);
    return res.data;
  }
};
