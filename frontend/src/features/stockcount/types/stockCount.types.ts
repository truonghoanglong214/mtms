export type StockCountStatus = 'DRAFT' | 'SUBMITTED' | 'POSTED' | 'CANCELLED';
export type StockCountType = 'PERIODIC' | 'SURPRISE';

export interface StockCountLineDto {
  id: string;
  lineNumber: number;
  itemId: string;
  itemCode: string;
  itemName: string;
  baseUnit: string;
  lotId: string | null;
  expectedQty: number;
  countedQty: number;
  varianceQty: number;
  snapshotUnitCost: number;
  varianceValue: number;
  discrepancyReason: string | null;
  notes: string | null;
}

export interface StockCountDetailDto {
  id: string;
  countNumber: string;
  businessEntityId: string;
  branchId: string;
  branchName: string;
  countedOn: string;
  status: StockCountStatus;
  countType: StockCountType;
  totalItems: number;
  totalSurplusQty: number;
  totalShortageQty: number;
  totalSurplusValue: number;
  totalShortageValue: number;
  netVarianceValue: number;
  notes: string | null;
  createdBy: string;
  createdByName: string;
  submittedBy: string | null;
  submittedByName: string | null;
  submittedAt: string | null;
  approvedBy: string | null;
  approvedByName: string | null;
  postedAt: string | null;
  createdAt: string;
  updatedAt: string;
  lines: StockCountLineDto[];
}

export interface StockCountListResponse {
  items: StockCountDetailDto[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export interface StockCountSummaryDto {
  totalCounts: number;
  pendingApprovalCount: number;
  totalSurplusValue: number;
  totalShortageValue: number;
  netVarianceValue: number;
}

export interface CreateStockCountRequest {
  countedOn: string;
  countType: StockCountType;
  notes?: string | null;
  itemIds?: string[] | null;
}

export interface UpdateCountLineRequest {
  lineId: string;
  countedQty: number;
  discrepancyReason?: string | null;
  notes?: string | null;
}

export interface UpdateStockCountRequest {
  notes?: string | null;
  lines: UpdateCountLineRequest[];
}

export const STOCK_COUNT_STATUS_META: Record<
  StockCountStatus,
  { label: string; bg: string; text: string; border: string }
> = {
  DRAFT: {
    label: 'Bản nháp',
    bg: 'bg-slate-500/10',
    text: 'text-slate-400',
    border: 'border-slate-500/20'
  },
  SUBMITTED: {
    label: 'Chờ duyệt (TC-026)',
    bg: 'bg-amber-500/10',
    text: 'text-amber-400',
    border: 'border-amber-500/20'
  },
  POSTED: {
    label: 'Đã hoàn tất (Đã chốt sổ)',
    bg: 'bg-emerald-500/10',
    text: 'text-emerald-400',
    border: 'border-emerald-500/20'
  },
  CANCELLED: {
    label: 'Đã hủy',
    bg: 'bg-rose-500/10',
    text: 'text-rose-400',
    border: 'border-rose-500/20'
  }
};

export const STOCK_COUNT_TYPE_META: Record<
  StockCountType,
  { label: string; badge: string }
> = {
  PERIODIC: {
    label: 'Định kỳ',
    badge: 'bg-blue-500/10 text-blue-400 border-blue-500/20'
  },
  SURPRISE: {
    label: 'Đột xuất',
    badge: 'bg-purple-500/10 text-purple-400 border-purple-500/20'
  }
};
