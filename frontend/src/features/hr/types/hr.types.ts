export type EmployeeStatus = 'ACTIVE' | 'TERMINATED' | 'SUSPENDED'

export type RateType = 'HOURLY' | 'MONTHLY'

/**
 * Summary item for workforce directory listing (P0-HR-01 / FE-P0-17)
 */
export interface EmployeeSummary {
  id: string
  staffCode: string
  fullName: string
  positionName: string
  phoneNumber: string | null
  email: string | null
  hiredOn: string
  leftOn: string | null
  status: EmployeeStatus
  isPrimaryBranch: boolean
  activeRateType: RateType | null
  activeRateAmount: number | null
  createdAt: string
}

/**
 * Branch allocation history slice (TC-015)
 */
export interface EmployeeAssignment {
  id: string
  employeeId?: string
  branchId: string
  branchName?: string
  isPrimary: boolean
  effectiveFrom: string
  effectiveTo: string | null
  notes?: string | null
  createdAt?: string
}

/**
 * Effective salary policy with slice dates (TC-013 / P0-HR-02 / FE-P0-18)
 */
export interface SalaryPolicy {
  id: string
  employeeId?: string
  branchId?: string
  rateType: RateType
  rateAmount: number
  standardWorkdays: number | null
  paidMinutesRoundingBlock: number | null
  effectiveFrom: string
  effectiveTo: string | null
  notes?: string | null
  approvedBy?: string | null
  createdAt?: string
}

/**
 * Full employee profile with assignments and salary history
 */
export interface EmployeeDetail {
  id: string
  userId?: string | null
  staffCode: string
  fullName: string
  positionName: string
  phoneNumber: string | null
  email: string | null
  hiredOn: string
  leftOn: string | null
  status: EmployeeStatus
  notes: string | null
  assignments: EmployeeAssignment[]
  salaryPolicies: SalaryPolicy[]
  createdAt?: string
  updatedAt?: string
}

/**
 * Payload to register a new employee (P0-HR-01)
 */
export interface CreateEmployeePayload {
  staffCode: string
  fullName: string
  positionName: string
  phoneNumber?: string | null
  email?: string | null
  hiredOn: string
  notes?: string | null
  initialSalaryPolicy?: {
    rateType: RateType
    rateAmount: number
    standardWorkdays?: number | null
    paidMinutesRoundingBlock?: number | null
    effectiveFrom: string
    notes?: string | null
  }
}

/**
 * Payload to update employee demographics and status
 */
export interface UpdateEmployeePayload {
  fullName: string
  positionName: string
  phoneNumber?: string | null
  email?: string | null
  status: EmployeeStatus
  leftOn?: string | null
  notes?: string | null
}

/**
 * Payload to assign branch or transfer primary branch (TC-015)
 */
export interface AssignBranchPayload {
  targetBranchId: string
  isPrimary: boolean
  effectiveFrom: string
  effectiveTo?: string | null
  notes?: string | null
}

/**
 * Payload to configure new salary policy (TC-013)
 */
export interface CreateSalaryPolicyPayload {
  rateType: RateType
  rateAmount: number
  standardWorkdays?: number | null
  paidMinutesRoundingBlock?: number | null
  effectiveFrom: string
  notes?: string | null
}

/**
 * Standard pagination response for employee listing
 */
export interface EmployeePageResponse {
  content: EmployeeSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * Query filter parameters
 */
export interface EmployeeFilterParams {
  search?: string
  status?: string
  page?: number
  size?: number
}

/**
 * Employee Status metadata & Vietnamese UI badges
 */
export const EMPLOYEE_STATUS_META: Record<
  EmployeeStatus,
  { label: string; badgeClass: string; dotClass: string }
> = {
  ACTIVE: {
    label: 'Đang làm việc',
    badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    dotClass: 'bg-emerald-500',
  },
  TERMINATED: {
    label: 'Đã thôi việc',
    badgeClass: 'bg-slate-100 text-slate-600 border-slate-300',
    dotClass: 'bg-slate-400',
  },
  SUSPENDED: {
    label: 'Tạm dừng',
    badgeClass: 'bg-amber-50 text-amber-700 border-amber-200',
    dotClass: 'bg-amber-500',
  },
}

/**
 * Rate Type metadata & Vietnamese UI badges
 */
export const RATE_TYPE_META: Record<
  RateType,
  { label: string; unit: string; badgeClass: string }
> = {
  HOURLY: {
    label: 'Lương theo giờ',
    unit: '₫/giờ',
    badgeClass: 'bg-blue-50 text-blue-700 border-blue-200',
  },
  MONTHLY: {
    label: 'Lương theo tháng',
    unit: '₫/tháng',
    badgeClass: 'bg-purple-50 text-purple-700 border-purple-200',
  },
}

/**
 * Standard rounding block options for time rounding (minutes)
 */
export const ROUNDING_BLOCK_OPTIONS = [
  { value: 1, label: '1 phút (Chính xác từng phút)' },
  { value: 5, label: '5 phút' },
  { value: 10, label: '10 phút' },
  { value: 15, label: '15 phút (Quy chuẩn tiêu chuẩn MTMS)' },
  { value: 30, label: '30 phút' },
  { value: 60, label: '60 phút (Làm tròn 1 giờ)' },
]

/**
 * Popular beverage / coffee shop staff positions
 */
export const COMMON_POSITIONS = [
  'Pha chế (Barista)',
  'Thu ngân',
  'Phục vụ bàn',
  'Trưởng ca',
  'Quản lý cửa hàng (Store Manager)',
  'Bếp / Sơ chế nguyên liệu',
  'Bảo vệ / Giữ xe',
  'Tạp vụ / Vệ sinh',
]
