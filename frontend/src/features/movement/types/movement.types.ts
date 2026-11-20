export type TransferStatus = 'DRAFT' | 'DISPATCHED' | 'PARTIAL_RECEIPT' | 'COMPLETED' | 'CANCELLED';

export const TRANSFER_STATUS_LABELS: Record<TransferStatus, { label: string; color: string }> = {
  DRAFT: { label: 'Bản nháp', color: 'bg-neutral-100 text-neutral-700 dark:bg-neutral-800 dark:text-neutral-300' },
  DISPATCHED: { label: 'Đang vận chuyển', color: 'bg-blue-100 text-blue-700 dark:bg-blue-950 dark:text-blue-300' },
  PARTIAL_RECEIPT: { label: 'Nhận một phần (Có hao hụt)', color: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300' },
  COMPLETED: { label: 'Hoàn tất 100%', color: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300' }
};

export interface TransferLine {
  id: string;
  lineNumber: number;
  itemId: string;
  itemName: string;
  itemCode: string;
  baseUnit: string;
  lotId?: string;
  lotCode?: string;
  dispatchedBaseQty: number;
  receivedBaseQty: number;
  transitVarianceQty: number;
  senderUnitCost: number;
  dispatchedLineCost: number;
  receivedLineCost: number;
  receiverNote?: string;
  notes?: string;
}

export interface StockTransfer {
  id: string;
  transferNumber: string;
  businessEntityId: string;
  fromBranchId: string;
  fromBranchName: string;
  toBranchId: string;
  toBranchName: string;
  requestedOn: string;
  dispatchedAt?: string;
  completedAt?: string;
  status: TransferStatus;
  requestedBy: string;
  requestedByName?: string;
  dispatchedBy?: string;
  dispatchedByName?: string;
  receivedBy?: string;
  receivedByName?: string;
  totalLines: number;
  totalDispatchedQuantity: number;
  totalReceivedQuantity: number;
  totalTransitVarianceQuantity: number;
  totalDispatchedCost: number;
  totalReceivedCost: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  lines: TransferLine[];
}

export interface TransferListResponse {
  items: StockTransfer[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export interface TransferSummary {
  outboundCount: number;
  inboundCount: number;
  inTransitCount: number;
  discrepancyCount: number;
  totalDispatchedCost: number;
  totalReceivedCost: number;
}

export interface CreateTransferLinePayload {
  itemId: string;
  lotId?: string;
  dispatchedBaseQty: number;
  notes?: string;
}

export interface CreateTransferPayload {
  toBranchId: string;
  requestedOn: string;
  autoDispatch: boolean;
  notes?: string;
  lines: CreateTransferLinePayload[];
}

export interface ReceiveTransferLinePayload {
  lineId: string;
  receivedBaseQty: number;
  receiverNote?: string;
}

export interface ReceiveTransferPayload {
  notes?: string;
  lines: ReceiveTransferLinePayload[];
}
