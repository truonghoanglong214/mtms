import React from 'react'
import {
  Search,
  Filter,
  Eye,
  Edit,
  Coins,
  GitFork,
  ChevronLeft,
  ChevronRight,
  Phone,
  Mail,
  Calendar,
  Briefcase,
  Star,
  Loader2,
  Users,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { formatVnd } from '../api/hrApi'
import {
  EMPLOYEE_STATUS_META,
  RATE_TYPE_META,
  type EmployeeSummary,
} from '../types/hr.types'

interface EmployeeListTabProps {
  employees: EmployeeSummary[]
  isLoading: boolean
  searchQuery: string
  onSearchChange: (query: string) => void
  selectedStatus: string
  onStatusChange: (status: string) => void
  currentPage: number
  totalPages: number
  totalElements: number
  pageSize: number
  onPageChange: (newPage: number) => void
  onViewDetail: (employee: EmployeeSummary) => void
  onEdit: (employee: EmployeeSummary) => void
  onManageSalary: (employee: EmployeeSummary) => void
  onAssignBranch: (employee: EmployeeSummary) => void
  onOpenCreateModal: () => void
}

export const EmployeeListTab: React.FC<EmployeeListTabProps> = ({
  employees,
  isLoading,
  searchQuery,
  onSearchChange,
  selectedStatus,
  onStatusChange,
  currentPage,
  totalPages,
  totalElements,
  onPageChange,
  onViewDetail,
  onEdit,
  onManageSalary,
  onAssignBranch,
  onOpenCreateModal,
}) => {
  return (
    <div className="space-y-4">
      {/* Search and Filters Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border border-slate-200 shadow-2xs">
        <div className="flex-1 flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          {/* Search Input */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Tìm theo họ tên hoặc mã nhân viên (Staff Code)..."
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-full pl-9 pr-4 py-2 rounded-xl border border-slate-200 bg-slate-50/50 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:bg-white transition-all"
            />
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-slate-400 shrink-0" />
            <select
              value={selectedStatus}
              onChange={(e) => onStatusChange(e.target.value)}
              className="px-3 py-2 rounded-xl border border-slate-200 bg-slate-50/50 text-xs font-medium text-slate-700 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:bg-white transition-all"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang làm việc (ACTIVE)</option>
              <option value="SUSPENDED">Tạm dừng (SUSPENDED)</option>
              <option value="TERMINATED">Đã thôi việc (TERMINATED)</option>
            </select>
          </div>
        </div>

        <div className="text-xs text-slate-500 font-medium shrink-0 self-end sm:self-center">
          Tổng số: <strong className="text-slate-800">{totalElements}</strong> nhân sự
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
        {isLoading ? (
          <div className="p-16 text-center text-xs text-slate-500 space-y-2">
            <Loader2 className="w-6 h-6 mx-auto animate-spin text-[#304638]" />
            <p>Đang tải danh sách nhân sự chi nhánh...</p>
          </div>
        ) : employees.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Users className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <h4 className="text-sm font-bold text-slate-800">
                Chưa có nhân viên nào phù hợp
              </h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                {searchQuery || selectedStatus !== 'ALL'
                  ? 'Không tìm thấy hồ sơ nào khớp với điều kiện lọc hiện tại. Hãy thử tìm kiếm với từ khóa khác.'
                  : 'Chi nhánh hiện chưa có hồ sơ nhân viên. Hãy thêm nhân viên đầu tiên ngay bên dưới.'}
              </p>
            </div>
            <Button
              type="button"
              onClick={onOpenCreateModal}
              className="bg-[#304638] hover:bg-[#25372C] text-white text-xs h-9 px-4"
            >
              + Thêm hồ sơ nhân sự mới
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold tracking-wide">
                  <th className="py-3 px-4">Nhân sự & Mã số</th>
                  <th className="py-3 px-3">Vị trí công việc</th>
                  <th className="py-3 px-3">Liên hệ</th>
                  <th className="py-3 px-3">Ngày tuyển dụng</th>
                  <th className="py-3 px-3">Mức lương hiện hành</th>
                  <th className="py-3 px-3">Phân bổ</th>
                  <th className="py-3 px-3 text-center">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {employees.map((emp) => {
                  const statusMeta = EMPLOYEE_STATUS_META[emp.status]
                  const rateMeta = emp.activeRateType ? RATE_TYPE_META[emp.activeRateType] : null

                  return (
                    <tr
                      key={emp.id}
                      className="hover:bg-slate-50/60 transition-colors group"
                    >
                      {/* Nhân sự & Mã số */}
                      <td className="py-3 px-4">
                        <div className="flex items-center space-x-3">
                          <div className="w-8 h-8 rounded-xl bg-[#304638]/10 text-[#304638] flex items-center justify-center font-bold font-mono text-xs shrink-0 ring-1 ring-[#304638]/20">
                            {emp.fullName.slice(0, 1).toUpperCase()}
                          </div>
                          <div>
                            <span
                              onClick={() => onViewDetail(emp)}
                              className="font-bold text-slate-900 group-hover:text-[#304638] cursor-pointer block hover:underline"
                            >
                              {emp.fullName}
                            </span>
                            <span className="font-mono text-[11px] font-semibold text-slate-400 block">
                              {emp.staffCode}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Vị trí */}
                      <td className="py-3 px-3">
                        <span className="font-medium text-slate-700 flex items-center gap-1.5">
                          <Briefcase className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span>{emp.positionName}</span>
                        </span>
                      </td>

                      {/* Liên hệ */}
                      <td className="py-3 px-3">
                        <div className="space-y-0.5">
                          {emp.phoneNumber ? (
                            <span className="text-slate-600 flex items-center gap-1">
                              <Phone className="w-3 h-3 text-slate-400" />
                              <span>{emp.phoneNumber}</span>
                            </span>
                          ) : (
                            <span className="text-slate-400 italic">—</span>
                          )}
                          {emp.email && (
                            <span className="text-[11px] text-slate-400 flex items-center gap-1 truncate max-w-[140px]">
                              <Mail className="w-3 h-3 text-slate-300 shrink-0" />
                              <span className="truncate">{emp.email}</span>
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Ngày tuyển dụng / Thôi việc */}
                      <td className="py-3 px-3">
                        <div className="space-y-0.5 font-mono text-[11px]">
                          <span className="text-slate-700 flex items-center gap-1">
                            <Calendar className="w-3 h-3 text-slate-400" />
                            <span>{emp.hiredOn}</span>
                          </span>
                          {emp.leftOn && (
                            <span className="text-rose-600 font-semibold block">
                              Thôi việc: {emp.leftOn}
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Mức lương hiện hành (TC-013) */}
                      <td className="py-3 px-3">
                        {emp.activeRateAmount !== null && emp.activeRateAmount !== undefined ? (
                          <div className="space-y-0.5">
                            <div className="flex items-center gap-1.5">
                              <span className="font-bold font-mono text-slate-900">
                                {formatVnd(emp.activeRateAmount)}
                              </span>
                              {rateMeta && (
                                <span
                                  className={`px-1.5 py-0.2 rounded text-[10px] font-bold ${rateMeta.badgeClass}`}
                                >
                                  {emp.activeRateType === 'HOURLY' ? '/giờ' : '/tháng'}
                                </span>
                              )}
                            </div>
                            <span className="text-[10px] text-slate-400 block">
                              Chính sách mốc hiệu lực
                            </span>
                          </div>
                        ) : (
                          <span className="text-slate-400 italic text-[11px]">
                            Chưa thiết lập lương
                          </span>
                        )}
                      </td>

                      {/* Phân bổ (TC-015) */}
                      <td className="py-3 px-3">
                        {emp.isPrimaryBranch ? (
                          <Badge className="bg-emerald-600 text-white text-[10px] py-0.5 px-2 flex items-center gap-1 w-fit shadow-2xs">
                            <Star className="w-3 h-3 fill-current" />
                            <span>Chi nhánh chính</span>
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="text-slate-600 border-slate-300 text-[10px] py-0.5 px-2 w-fit">
                            Phân bổ phụ
                          </Badge>
                        )}
                      </td>

                      {/* Trạng thái */}
                      <td className="py-3 px-3 text-center">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${statusMeta.badgeClass}`}
                        >
                          <span className={`w-1.5 h-1.5 rounded-full ${statusMeta.dotClass}`} />
                          <span>{statusMeta.label}</span>
                        </span>
                      </td>

                      {/* Thao tác */}
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end space-x-1">
                          <button
                            type="button"
                            title="Xem chi tiết hồ sơ"
                            onClick={() => onViewDetail(emp)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-[#304638] hover:bg-[#304638]/10 transition-colors"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            title="Chỉnh sửa thông tin"
                            onClick={() => onEdit(emp)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            title="Thiết lập chính sách lương"
                            onClick={() => onManageSalary(emp)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-emerald-700 hover:bg-emerald-50 transition-colors"
                          >
                            <Coins className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            title="Điều chuyển / Phân bổ chi nhánh"
                            onClick={() => onAssignBranch(emp)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-blue-700 hover:bg-blue-50 transition-colors"
                          >
                            <GitFork className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div className="px-4 py-3 bg-slate-50/80 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
            <span>
              Trang <strong>{currentPage + 1}</strong> / <strong>{totalPages}</strong> (
              {totalElements} nhân viên)
            </span>
            <div className="flex items-center space-x-1.5">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={currentPage === 0 || isLoading}
                onClick={() => onPageChange(currentPage - 1)}
                className="h-8 px-2.5 text-xs"
              >
                <ChevronLeft className="w-3.5 h-3.5 mr-1" />
                <span>Trước</span>
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={currentPage >= totalPages - 1 || isLoading}
                onClick={() => onPageChange(currentPage + 1)}
                className="h-8 px-2.5 text-xs"
              >
                <span>Sau</span>
                <ChevronRight className="w-3.5 h-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
