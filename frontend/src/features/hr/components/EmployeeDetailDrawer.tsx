import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  User,
  Phone,
  Mail,
  Calendar,
  Briefcase,
  Building2,
  Coins,
  GitFork,
  Edit,
  Plus,
  Loader2,
  ArrowRight,
  ShieldCheck,
  Star,
  FileText,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { hrApi, formatVnd } from '../api/hrApi'
import {
  EMPLOYEE_STATUS_META,
  RATE_TYPE_META,
  type EmployeeDetail,
} from '../types/hr.types'

interface EmployeeDetailDrawerProps {
  isOpen: boolean
  branchId: string
  employeeId: string | null
  onClose: () => void
  onEdit: (detail: EmployeeDetail) => void
  onAssignBranch: (detail: EmployeeDetail) => void
  onManageSalary: (detail: EmployeeDetail) => void
}

export const EmployeeDetailDrawer: React.FC<EmployeeDetailDrawerProps> = ({
  isOpen,
  branchId,
  employeeId,
  onClose,
  onEdit,
  onAssignBranch,
  onManageSalary,
}) => {
  const [detail, setDetail] = useState<EmployeeDetail | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'ASSIGNMENTS' | 'SALARY'>('OVERVIEW')

  const fetchDetail = useCallback(async () => {
    if (!branchId || !employeeId) return
    try {
      setIsLoading(true)
      const data = await hrApi.getEmployeeDetail(branchId, employeeId)
      setDetail(data)
    } catch (err) {
      console.error('Failed to load employee detail:', err)
    } finally {
      setIsLoading(false)
    }
  }, [branchId, employeeId])

  useEffect(() => {
    if (isOpen && employeeId) {
      fetchDetail()
      setActiveTab('OVERVIEW')
    } else {
      setDetail(null)
    }
  }, [isOpen, employeeId, fetchDetail])

  if (!isOpen) return null

  const statusMeta = detail ? EMPLOYEE_STATUS_META[detail.status] : null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex justify-end">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs"
        />

        {/* Slide-over Drawer */}
        <motion.div
          initial={{ x: '100%' }}
          animate={{ x: 0 }}
          exit={{ x: '100%' }}
          transition={{ type: 'spring', damping: 25, stiffness: 200 }}
          className="relative w-full max-w-xl bg-white shadow-2xl z-10 flex flex-col h-full border-l border-slate-200"
        >
          {/* Header */}
          <div className="px-6 py-5 bg-[#304638] text-white flex items-center justify-between shrink-0">
            <div className="flex items-center space-x-3.5">
              <div className="w-12 h-12 rounded-2xl bg-white/10 flex items-center justify-center text-white ring-2 ring-white/20">
                <User className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold tracking-tight text-white">
                    {detail?.fullName || 'Đang tải...'}
                  </h2>
                  {detail && statusMeta && (
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${statusMeta.badgeClass}`}
                    >
                      {statusMeta.label}
                    </span>
                  )}
                </div>
                <p className="text-xs text-[#C8CEC4] font-mono mt-0.5">
                  Mã NV: <strong>{detail?.staffCode || employeeId}</strong> •{' '}
                  {detail?.positionName}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-xl text-slate-300 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Nav Tabs */}
          <div className="px-6 border-b border-slate-200 bg-slate-50 flex space-x-6 text-xs font-semibold shrink-0">
            <button
              type="button"
              onClick={() => setActiveTab('OVERVIEW')}
              className={`py-3 border-b-2 transition-colors ${
                activeTab === 'OVERVIEW'
                  ? 'border-[#304638] text-[#304638]'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              Hồ sơ tổng quan
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('ASSIGNMENTS')}
              className={`py-3 border-b-2 transition-colors flex items-center gap-1.5 ${
                activeTab === 'ASSIGNMENTS'
                  ? 'border-[#304638] text-[#304638]'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <span>Phân bổ chi nhánh (TC-015)</span>
              {detail?.assignments && (
                <span className="px-1.5 py-0.2 bg-slate-200 text-slate-700 rounded-full text-[10px]">
                  {detail.assignments.length}
                </span>
              )}
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('SALARY')}
              className={`py-3 border-b-2 transition-colors flex items-center gap-1.5 ${
                activeTab === 'SALARY'
                  ? 'border-[#304638] text-[#304638]'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <span>Lịch sử lương (TC-013)</span>
              {detail?.salaryPolicies && (
                <span className="px-1.5 py-0.2 bg-slate-200 text-slate-700 rounded-full text-[10px]">
                  {detail.salaryPolicies.length}
                </span>
              )}
            </button>
          </div>

          {/* Drawer Body */}
          <div className="flex-1 overflow-y-auto p-6">
            {isLoading ? (
              <div className="h-64 flex flex-col items-center justify-center text-slate-500 text-xs">
                <Loader2 className="w-6 h-6 animate-spin text-[#304638] mb-2" />
                <span>Đang tải thông tin chi tiết nhân sự...</span>
              </div>
            ) : !detail ? (
              <div className="p-8 text-center text-slate-500 text-xs">
                Không thể tải thông tin nhân viên hoặc hồ sơ không tồn tại.
              </div>
            ) : (
              <>
                {/* TAB 1: OVERVIEW */}
                {activeTab === 'OVERVIEW' && (
                  <div className="space-y-6">
                    {/* Quick Action Shortcuts */}
                    <div className="grid grid-cols-3 gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => onEdit(detail)}
                        className="text-xs h-9 justify-center border-slate-200 hover:bg-slate-50"
                      >
                        <Edit className="w-3.5 h-3.5 mr-1.5 text-slate-600" />
                        <span>Sửa hồ sơ</span>
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => onAssignBranch(detail)}
                        className="text-xs h-9 justify-center border-slate-200 hover:bg-slate-50"
                      >
                        <GitFork className="w-3.5 h-3.5 mr-1.5 text-blue-600" />
                        <span>Điều chuyển</span>
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => onManageSalary(detail)}
                        className="text-xs h-9 justify-center border-slate-200 hover:bg-slate-50"
                      >
                        <Coins className="w-3.5 h-3.5 mr-1.5 text-emerald-600" />
                        <span>Mức lương</span>
                      </Button>
                    </div>

                    {/* Basic Info Card */}
                    <div className="rounded-2xl border border-slate-200 bg-slate-50/50 p-4 space-y-3">
                      <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                        <FileText className="w-3.5 h-3.5 text-[#304638]" />
                        <span>Thông tin liên hệ & Công tác</span>
                      </h3>

                      <div className="grid grid-cols-2 gap-4 text-xs">
                        <div>
                          <span className="text-slate-400 block mb-0.5">Vị trí đảm nhiệm:</span>
                          <span className="font-semibold text-slate-800 flex items-center gap-1.5">
                            <Briefcase className="w-3.5 h-3.5 text-slate-500" />
                            <span>{detail.positionName}</span>
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-400 block mb-0.5">Số điện thoại:</span>
                          <span className="font-semibold text-slate-800 flex items-center gap-1.5">
                            <Phone className="w-3.5 h-3.5 text-slate-500" />
                            <span>{detail.phoneNumber || '—'}</span>
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-400 block mb-0.5">Email liên hệ:</span>
                          <span className="font-semibold text-slate-800 flex items-center gap-1.5">
                            <Mail className="w-3.5 h-3.5 text-slate-500" />
                            <span className="truncate">{detail.email || '—'}</span>
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-400 block mb-0.5">Ngày tuyển dụng:</span>
                          <span className="font-semibold font-mono text-slate-800 flex items-center gap-1.5">
                            <Calendar className="w-3.5 h-3.5 text-slate-500" />
                            <span>{detail.hiredOn}</span>
                          </span>
                        </div>
                        {detail.leftOn && (
                          <div className="col-span-2 p-2 rounded-xl bg-amber-50 border border-amber-200">
                            <span className="text-amber-800 font-semibold block mb-0.5">
                              Ngày thôi việc:
                            </span>
                            <span className="font-mono text-amber-950 font-bold">
                              {detail.leftOn}
                            </span>
                          </div>
                        )}
                      </div>

                      {detail.notes && (
                        <div className="pt-2 border-t border-slate-200/60 text-xs">
                          <span className="text-slate-400 block mb-0.5">Ghi chú nhân viên:</span>
                          <p className="text-slate-700 bg-white p-2.5 rounded-xl border border-slate-200 italic">
                            "{detail.notes}"
                          </p>
                        </div>
                      )}
                    </div>

                    {/* Active Salary Policy Snapshot */}
                    <div className="rounded-2xl border border-emerald-200 bg-emerald-50/40 p-4 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-emerald-900 flex items-center gap-1.5">
                          <Coins className="w-4 h-4 text-emerald-700" />
                          <span>Chính sách lương hiện hành tại chi nhánh</span>
                        </span>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => onManageSalary(detail)}
                          className="text-xs text-emerald-700 hover:text-emerald-800 p-0 h-auto"
                        >
                          Lịch sử mốc lương &rarr;
                        </Button>
                      </div>

                      {detail.salaryPolicies && detail.salaryPolicies.length > 0 ? (
                        (() => {
                          const activePolicy =
                            detail.salaryPolicies.find((p) => !p.effectiveTo) ||
                            detail.salaryPolicies[0]
                          const meta = RATE_TYPE_META[activePolicy.rateType]
                          return (
                            <div className="bg-white rounded-xl p-3 border border-emerald-200 shadow-xs flex items-center justify-between">
                              <div>
                                <div className="flex items-center gap-2">
                                  <span
                                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${meta.badgeClass}`}
                                  >
                                    {activePolicy.rateType}
                                  </span>
                                  <span className="text-base font-bold font-mono text-slate-900">
                                    {formatVnd(activePolicy.rateAmount)}
                                  </span>
                                  <span className="text-xs text-slate-500 font-medium">
                                    {meta.unit}
                                  </span>
                                </div>
                                <div className="text-[11px] text-slate-500 mt-1 flex items-center gap-2">
                                  <span>Hiệu lực từ: <strong>{activePolicy.effectiveFrom}</strong></span>
                                  <span>•</span>
                                  <span>Làm tròn: <strong>{activePolicy.paidMinutesRoundingBlock || 15}p</strong></span>
                                </div>
                              </div>
                              <Badge className="bg-emerald-600 text-white text-[10px]">
                                Đang áp dụng
                              </Badge>
                            </div>
                          )
                        })()
                      ) : (
                        <p className="text-xs text-slate-500 italic">
                          Chưa có chính sách lương nào được kích hoạt cho nhân viên này.
                        </p>
                      )}
                    </div>
                  </div>
                )}

                {/* TAB 2: BRANCH ASSIGNMENTS (TC-015) */}
                {activeTab === 'ASSIGNMENTS' && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <p className="text-xs text-slate-600">
                        Lịch sử phân bổ các chi nhánh công tác (TC-015)
                      </p>
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => onAssignBranch(detail)}
                        className="bg-[#304638] hover:bg-[#25372C] text-white text-xs h-8 px-3"
                      >
                        <Plus className="w-3.5 h-3.5 mr-1" />
                        <span>Thêm phân bổ chi nhánh</span>
                      </Button>
                    </div>

                    <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-800 flex items-start gap-2">
                      <ShieldCheck className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                      <p className="text-[11px] leading-relaxed">
                        Theo quy chuẩn <strong>TC-015</strong>, chi nhánh có cờ <strong>Chi nhánh chính</strong> vào ngày cuối tháng sẽ chịu trách nhiệm thanh toán và ghi nhận chi phí lương của nhân viên.
                      </p>
                    </div>

                    <div className="space-y-3">
                      {detail.assignments && detail.assignments.length > 0 ? (
                        detail.assignments.map((asg, idx) => (
                          <div
                            key={asg.id || idx}
                            className={`p-3.5 rounded-xl border ${
                              asg.isPrimary
                                ? 'bg-emerald-50/50 border-emerald-300 ring-1 ring-emerald-400/20'
                                : 'bg-white border-slate-200'
                            }`}
                          >
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-2">
                                <Building2 className="w-4 h-4 text-slate-600" />
                                <span className="text-xs font-bold text-slate-900">
                                  {asg.branchName || asg.branchId}
                                </span>
                                {asg.isPrimary && (
                                  <Badge className="bg-emerald-600 text-white text-[10px] py-0 px-1.5 flex items-center gap-1">
                                    <Star className="w-3 h-3 fill-current" />
                                    <span>Chi nhánh chính</span>
                                  </Badge>
                                )}
                              </div>
                              <div className="text-xs font-mono text-slate-600 flex items-center gap-1">
                                <span>{asg.effectiveFrom}</span>
                                <ArrowRight className="w-3 h-3 text-slate-400" />
                                <span>{asg.effectiveTo || 'Hiện tại'}</span>
                              </div>
                            </div>
                            {asg.notes && (
                              <p className="text-[11px] text-slate-500 italic mt-1.5 pt-1.5 border-t border-slate-100">
                                "{asg.notes}"
                              </p>
                            )}
                          </div>
                        ))
                      ) : (
                        <p className="text-xs text-slate-500 italic text-center py-4">
                          Chưa có thông tin phân bổ chi nhánh.
                        </p>
                      )}
                    </div>
                  </div>
                )}

                {/* TAB 3: SALARY POLICIES (TC-013) */}
                {activeTab === 'SALARY' && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <p className="text-xs text-slate-600">
                        Lịch sử thay đổi đơn giá lương theo mốc hiệu lực (TC-013)
                      </p>
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => onManageSalary(detail)}
                        className="bg-[#304638] hover:bg-[#25372C] text-white text-xs h-8 px-3"
                      >
                        <Plus className="w-3.5 h-3.5 mr-1" />
                        <span>Điều chỉnh mức lương</span>
                      </Button>
                    </div>

                    <div className="space-y-3">
                      {detail.salaryPolicies && detail.salaryPolicies.length > 0 ? (
                        detail.salaryPolicies.map((sp, idx) => {
                          const meta = RATE_TYPE_META[sp.rateType]
                          const isCurrent = !sp.effectiveTo
                          return (
                            <div
                              key={sp.id || idx}
                              className={`p-3.5 rounded-xl border ${
                                isCurrent
                                  ? 'bg-blue-50/50 border-blue-300 shadow-xs'
                                  : 'bg-white border-slate-200 opacity-80'
                              }`}
                            >
                              <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                  <span
                                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${meta.badgeClass}`}
                                  >
                                    {sp.rateType}
                                  </span>
                                  <span className="text-sm font-bold font-mono text-slate-900">
                                    {formatVnd(sp.rateAmount)}
                                  </span>
                                  <span className="text-xs text-slate-500">{meta.unit}</span>
                                </div>
                                <div className="text-xs font-mono text-slate-600 flex items-center gap-1">
                                  <span>{sp.effectiveFrom}</span>
                                  <ArrowRight className="w-3 h-3 text-slate-400" />
                                  <span>{sp.effectiveTo || 'Hiện tại'}</span>
                                </div>
                              </div>
                              <div className="text-[11px] text-slate-500 mt-2 flex items-center justify-between pt-1.5 border-t border-slate-100">
                                <span>
                                  Làm tròn: {sp.paidMinutesRoundingBlock || 15} phút
                                  {sp.standardWorkdays ? ` • Công chuẩn: ${sp.standardWorkdays} ngày` : ''}
                                </span>
                                {sp.notes && <span className="italic">"{sp.notes}"</span>}
                              </div>
                            </div>
                          )
                        })
                      ) : (
                        <p className="text-xs text-slate-500 italic text-center py-4">
                          Chưa có lịch sử chính sách lương.
                        </p>
                      )}
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
