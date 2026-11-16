export type ImportKind = 'OPENING_INVENTORY' | 'ITEMS_CATALOG' | 'SUPPLIERS'

export type ImportBatchStatus = 'PREVIEW' | 'VALIDATED' | 'COMMITTED' | 'REJECTED'

export type ImportRowStatus = 'PENDING' | 'VALID' | 'ERROR'

export interface RowValidationError {
  field: string
  error: string
  message: string
}

export interface ImportBatch {
  id: string
  branchId: string
  importKind: ImportKind
  sourceFilename: string
  status: ImportBatchStatus
  totalRows: number
  validRows: number
  errorRows: number
  previewSummary: Record<string, any>
  committedBy?: string
  committedAt?: string
  createdAt: string
}

export interface ImportRow {
  id: string
  importBatchId: string
  rowNumber: number
  rawData: Record<string, any>
  normalizedData?: Record<string, any>
  errors: RowValidationError[]
  status: ImportRowStatus
}

export interface ColumnDefinition {
  key: string
  label: string
  required: boolean
  example: string
  description: string
}

export interface ImportKindConfig {
  kind: ImportKind
  title: string
  description: string
  badgeText: string
  columns: ColumnDefinition[]
}

export const IMPORT_KIND_CONFIGS: Record<ImportKind, ImportKindConfig> = {
  OPENING_INVENTORY: {
    kind: 'OPENING_INVENTORY',
    title: 'Tồn kho đầu kỳ',
    description: 'Nhập số dư tồn kho ban đầu (Opening stock cutover) và đơn giá bình quân gia quyền WAC cho chi nhánh',
    badgeText: 'P0-INV-02',
    columns: [
      { key: 'item_code', label: 'Mã NVL', required: true, example: 'CF-ROB-01', description: 'Mã định danh duy nhất của vật tư' },
      { key: 'item_name', label: 'Tên nguyên vật liệu', required: true, example: 'Cà phê Robusta hạt rang', description: 'Tên hiển thị' },
      { key: 'unit', label: 'Đơn vị tính', required: true, example: 'KG', description: 'Đơn vị đo lường (KG, HOP, THUNG, CHAI)' },
      { key: 'quantity', label: 'Số lượng tồn', required: true, example: '150.5', description: 'Số lượng tồn thực tế (> 0)' },
      { key: 'unit_cost', label: 'Đơn giá vốn', required: true, example: '120000', description: 'Giá nhập bình quân VNĐ (>= 0)' },
    ],
  },
  ITEMS_CATALOG: {
    kind: 'ITEMS_CATALOG',
    title: 'Danh mục nguyên vật liệu',
    description: 'Khởi tạo danh mục định danh vật tư chuẩn hóa, nhóm hàng và mức tồn an toàn',
    badgeText: 'CATALOG',
    columns: [
      { key: 'item_code', label: 'Mã định danh', required: true, example: 'TRA-OOLONG-01', description: 'Mã duy nhất vật tư' },
      { key: 'item_name', label: 'Tên nguyên vật liệu', required: true, example: 'Trà Oolong Bảo Lộc loại A', description: 'Tên mô tả đầy đủ' },
      { key: 'unit', label: 'Đơn vị tính chuẩn', required: true, example: 'HOP', description: 'Đơn vị đo lường cơ bản' },
      { key: 'category', label: 'Nhóm hàng', required: false, example: 'TRÀ NGUYÊN LIỆU', description: 'Phân loại kho' },
      { key: 'min_stock', label: 'Tồn kho tối thiểu', required: false, example: '10', description: 'Ngưỡng cảnh báo đặt hàng' },
    ],
  },
  SUPPLIERS: {
    kind: 'SUPPLIERS',
    title: 'Danh bạ nhà cung cấp',
    description: 'Nhập danh sách đối tác cung ứng, mã số thuế doanh nghiệp và thông tin liên hệ',
    badgeText: 'PARTNERS',
    columns: [
      { key: 'supplier_code', label: 'Mã nhà cung cấp', required: true, example: 'NCC-VINACAFE', description: 'Mã đối tác' },
      { key: 'supplier_name', label: 'Tên nhà cung cấp', required: true, example: 'Công ty Cổ phần VinaCafe Buôn Ma Thuột', description: 'Tên pháp nhân NCC' },
      { key: 'tax_code', label: 'Mã số thuế', required: false, example: '0301234567', description: 'Mã số thuế doanh nghiệp' },
      { key: 'phone', label: 'Số điện thoại', required: false, example: '0912345678', description: 'Số liên lạc giao dịch' },
      { key: 'address', label: 'Địa chỉ', required: false, example: '45 Đinh Tiên Hoàng, TP. Buôn Ma Thuột', description: 'Địa chỉ kho / trụ sở' },
    ],
  },
}
