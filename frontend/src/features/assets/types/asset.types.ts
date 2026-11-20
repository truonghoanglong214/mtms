export type AssetCategory =
  | 'COFFEE_MACHINE'
  | 'REFRIGERATION'
  | 'SEALING_MACHINE'
  | 'BLENDER'
  | 'POS_HARDWARE'
  | 'FURNITURE'
  | 'KITCHEN_EQUIPMENT'
  | 'OTHER';

export type AssetStatus =
  | 'IN_USE'
  | 'IN_REPAIR'
  | 'STORED'
  | 'DISPOSED'
  | 'TRANSFERRED';

export interface EquipmentAsset {
  id: string;
  branchId: string;
  branchName: string;
  assetCode: string;
  name: string;
  category: AssetCategory;
  serialNumber?: string | null;
  model?: string | null;
  purchasedOn?: string | null;
  acquisitionCost: number;
  warrantyUntil?: string | null;
  status: AssetStatus;
  locationInBranch?: string | null;
  supplierId?: string | null;
  supplierName?: string | null;
  evidenceFileId?: string | null;
  maintenanceNote?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEquipmentAssetRequest {
  assetCode: string;
  name: string;
  category: AssetCategory;
  serialNumber?: string;
  model?: string;
  purchasedOn?: string;
  acquisitionCost?: number;
  warrantyUntil?: string;
  status?: AssetStatus;
  locationInBranch?: string;
  supplierId?: string;
  evidenceFileId?: string;
  maintenanceNote?: string;
}

export interface UpdateEquipmentAssetRequest {
  name: string;
  category: AssetCategory;
  serialNumber?: string;
  model?: string;
  purchasedOn?: string;
  acquisitionCost?: number;
  warrantyUntil?: string;
  status?: AssetStatus;
  locationInBranch?: string;
  supplierId?: string;
  evidenceFileId?: string;
  maintenanceNote?: string;
}

export interface AssetSummary {
  branchId: string;
  totalAssets: number;
  totalAcquisitionCost: number;
  inUseCount: number;
  inRepairCount: number;
  storedCount: number;
  disposedCount: number;
  warrantyExpiringCount: number;
  warrantyExpiredCount: number;
  categoryBreakdown: Record<AssetCategory, number>;
  depreciationDisclaimer: string;
}

export interface AssetPageResponse {
  content: EquipmentAsset[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const ASSET_CATEGORY_LABELS: Record<AssetCategory, string> = {
  COFFEE_MACHINE: 'Máy pha cà phê',
  REFRIGERATION: 'Thiết bị lạnh (Tủ đông/mát/đá)',
  SEALING_MACHINE: 'Máy dập nắp cốc',
  BLENDER: 'Máy xay / Đảo trà',
  POS_HARDWARE: 'Thiết bị POS / Thu ngân',
  FURNITURE: 'Bàn ghế & Nội thất',
  KITCHEN_EQUIPMENT: 'Dụng cụ quầy bar & bếp',
  OTHER: 'Khác / Thiết bị phụ trợ',
};

export const ASSET_STATUS_LABELS: Record<AssetStatus, { label: string; color: string; bg: string }> = {
  IN_USE: {
    label: 'Đang hoạt động',
    color: 'text-emerald-700 dark:text-emerald-300',
    bg: 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800',
  },
  IN_REPAIR: {
    label: 'Đang sửa chữa / Bảo hành',
    color: 'text-amber-700 dark:text-amber-300',
    bg: 'bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800',
  },
  STORED: {
    label: 'Lưu kho / Tạm ngưng',
    color: 'text-blue-700 dark:text-blue-300',
    bg: 'bg-blue-50 dark:bg-blue-950/40 border-blue-200 dark:border-blue-800',
  },
  DISPOSED: {
    label: 'Đã thanh lý / Hủy',
    color: 'text-rose-700 dark:text-rose-300',
    bg: 'bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800',
  },
  TRANSFERRED: {
    label: 'Đã điều chuyển',
    color: 'text-purple-700 dark:text-purple-300',
    bg: 'bg-purple-50 dark:bg-purple-950/40 border-purple-200 dark:border-purple-800',
  },
};
