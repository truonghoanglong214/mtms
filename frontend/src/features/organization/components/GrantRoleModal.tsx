import React, { useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, ShieldCheck, AlertCircle, Loader2, Eye } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { organizationApi } from '../organizationApi'
import type {
  BusinessEntityDto,
  BranchDto,
  UserDto,
  RoleKey,
  ScopedRoleGrantDto,
} from '../types'

interface GrantRoleModalProps {
  isOpen: boolean
  users: UserDto[]
  entities: BusinessEntityDto[]
  branches: BranchDto[]
  preselectedUserId?: string
  onClose: () => void
  onSuccess: (grant: ScopedRoleGrantDto) => void
}

const ROLE_DESCRIPTIONS: Record<
  RoleKey,
  { title: string; desc: string; allowed: string[]; forbidden: string[] }
> = {
  SYSTEM_ADMIN: {
    title: 'Quản trị viên Hệ thống (System Admin)',
    desc: 'Toàn quyền cấu hình hệ thống, tạo pháp nhân, chi nhánh, người dùng và phân quyền.',
    allowed: ['Toàn quyền trên mọi pháp nhân & chi nhánh', 'Cấp và thu hồi quyền truy cập'],
    forbidden: [],
  },
  BRANCH_OWNER: {
    title: 'Chủ sở hữu Chi nhánh (Branch Owner)',
    desc: 'Quyền quản trị chi nhánh và xem các báo cáo tài chính theo phạm vi pháp nhân/chi nhánh được chỉ định.',
    allowed: ['Xem báo cáo P&L tạm tính', 'Duyệt chi phí và bảng lương chi nhánh', 'Phân công nhân sự'],
    forbidden: ['Không xem hoặc can thiệp chi nhánh ngoài phạm vi pháp nhân sở hữu'],
  },
  MANAGER: {
    title: 'Quản lý Chi nhánh (Branch Manager)',
    desc: 'Chịu trách nhiệm vận hành tại quán, báo cáo doanh thu ca và kiểm soát kho.',
    allowed: ['Nhập báo cáo doanh thu ngày', 'Kiểm đếm tồn kho', 'Duyệt giờ công nhân viên trong quán'],
    forbidden: ['Cấm tự duyệt giờ công của chính mình', 'Cấm truy cập dữ liệu chi nhánh khác (HTTP 403)'],
  },
  FINANCE: {
    title: 'Kế toán & Tài chính (Finance)',
    desc: 'Theo dõi dòng tiền, quản lý công nợ nhà cung cấp và ghi nhận chi trả.',
    allowed: ['Xem sổ quỹ tiền mặt & tiền gửi ngân hàng', 'Ghi nhận chi trả lương và nhập kho'],
    forbidden: ['Không tự ý chỉnh sửa báo cáo doanh thu đã chốt'],
  },
  EMPLOYEE: {
    title: 'Nhân viên (Employee / Barista)',
    desc: 'Nhân sự phục vụ/pha chế tại cửa hàng, chỉ thao tác theo ca.',
    allowed: ['Xem lịch làm việc cá nhân', 'Xem phiếu lương cá nhân'],
    forbidden: ['Không có quyền xem doanh thu, báo cáo tài chính hay danh sách lương người khác'],
  },
  INVESTOR: {
    title: 'Nhà đầu tư (Investor)',
    desc: 'Cổ đông góp vốn chỉ có quyền xem báo cáo tổng hợp theo tỷ lệ thoả thuận.',
    allowed: ['Xem báo cáo doanh thu & lợi nhuận tổng hợp được phê duyệt'],
    forbidden: ['Mặc định bị từ chối xem chi tiết lương nhân viên và dữ liệu GPS chấm công'],
  },
}

export const GrantRoleModal: React.FC<GrantRoleModalProps> = ({
  isOpen,
  users,
  entities,
  branches,
  preselectedUserId,
  onClose,
  onSuccess,
}) => {
  const [userId, setUserId] = useState<string>(preselectedUserId || (users[0]?.id ?? ''))
  const [entityId, setEntityId] = useState<string>(entities[0]?.id ?? '')
  const [branchId, setBranchId] = useState<string>('')
  const [roleKey, setRoleKey] = useState<RoleKey>('MANAGER')
  const [validFrom, setValidFrom] = useState<string>(new Date().toISOString().split('T')[0])
  const [validTo, setValidTo] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filter branches according to selected entity
  const filteredBranches = useMemo(() => {
    return branches.filter((b) => b.businessEntityId === entityId)
  }, [branches, entityId])

  // Auto-select first branch when entity changes
  React.useEffect(() => {
    if (filteredBranches.length > 0 && roleKey !== 'SYSTEM_ADMIN') {
      setBranchId(filteredBranches[0].id)
    } else if (roleKey === 'SYSTEM_ADMIN') {
      setBranchId('')
    }
  }, [filteredBranches, roleKey])

  React.useEffect(() => {
    if (preselectedUserId) {
      setUserId(preselectedUserId)
    } else if (users.length > 0 && !userId) {
      setUserId(users[0].id)
    }
  }, [preselectedUserId, users, userId])

  if (!isOpen) return null

  const selectedUser = users.find((u) => u.id === userId)
  const rolePreview = ROLE_DESCRIPTIONS[roleKey]

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!userId) {
      setError('Vui lòng chọn người dùng cần cấp quyền')
      return
    }
    if (!entityId) {
      setError('Vui lòng chọn pháp nhân')
      return
    }
    if (roleKey !== 'SYSTEM_ADMIN' && !branchId) {
      setError(`Vai trò ${roleKey} bắt buộc phải chọn chi nhánh áp dụng`)
      return
    }
    if (validTo && validTo < validFrom) {
      setError('Ngày hết hạn (validTo) không được trước ngày bắt đầu (validFrom)')
      return
    }

    setIsLoading(true)
    try {
      const grant = await organizationApi.grantRole({
        userId,
        businessEntityId: entityId,
        branchId: roleKey === 'SYSTEM_ADMIN' ? null : branchId || null,
        roleKey,
        actionScope: '*',
        rowScope: 'ALL',
        reportPermissionGroup: 'ALL',
        validFrom,
        validTo: validTo || null,
      })
      onSuccess(grant)
      onClose()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể cấp quyền. Vui lòng kiểm tra lại thông tin.'
      )
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-stone-900/60 backdrop-blur-sm"
        />

        {/* Modal Window */}
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-xl max-h-[90vh] overflow-y-auto rounded-xl border border-stone-800 bg-stone-900 p-6 shadow-2xl z-10 text-stone-100"
        >
          <div className="flex items-center justify-between pb-4 border-b border-stone-800">
            <div className="flex items-center space-x-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-950/80 border border-emerald-500/20 text-emerald-400">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-stone-100">Cấp Quyền Chi nhánh</h3>
                <p className="text-xs text-stone-400">Thiết lập phạm vi quyền hạn và thời hạn hiệu lực</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="rounded-lg p-1 text-stone-400 hover:bg-stone-800 hover:text-stone-200 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {error && (
            <div className="mt-4 flex items-center space-x-2 rounded-lg border border-red-500/30 bg-red-950/40 p-3 text-xs text-red-300">
              <AlertCircle className="h-4 w-4 shrink-0 text-red-400" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            {/* User selection */}
            <div>
              <label className="block text-xs font-medium text-stone-300 mb-1.5">
                Người dùng nhận quyền <span className="text-emerald-400 font-mono">*</span>
              </label>
              <select
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                className="w-full h-10 rounded-md border border-stone-800 bg-stone-950 px-3 text-xs text-stone-100 focus:outline-none focus:border-emerald-500"
                required
              >
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.displayName} (@{u.loginIdentifier}) — Trạng thái: {u.status}
                  </option>
                ))}
              </select>
            </div>

            {/* Entity selection */}
            <div>
              <label className="block text-xs font-medium text-stone-300 mb-1.5">
                Pháp nhân <span className="text-emerald-400 font-mono">*</span>
              </label>
              <select
                value={entityId}
                onChange={(e) => setEntityId(e.target.value)}
                className="w-full h-10 rounded-md border border-stone-800 bg-stone-950 px-3 text-xs text-stone-100 focus:outline-none focus:border-emerald-500"
                required
              >
                {entities.map((ent) => (
                  <option key={ent.id} value={ent.id}>
                    [{ent.entityCode}] {ent.registeredName}
                  </option>
                ))}
              </select>
            </div>

            {/* Role & Branch */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-stone-300 mb-1.5">
                  Vai trò (Role) <span className="text-emerald-400 font-mono">*</span>
                </label>
                <select
                  value={roleKey}
                  onChange={(e) => setRoleKey(e.target.value as RoleKey)}
                  className="w-full h-10 rounded-md border border-stone-800 bg-stone-950 px-3 text-xs text-stone-100 focus:outline-none focus:border-emerald-500 font-medium"
                  required
                >
                  <option value="MANAGER">MANAGER (Quản lý quán)</option>
                  <option value="BRANCH_OWNER">BRANCH_OWNER (Chủ chi nhánh)</option>
                  <option value="FINANCE">FINANCE (Kế toán chi nhánh)</option>
                  <option value="EMPLOYEE">EMPLOYEE (Nhân viên phục vụ/pha chế)</option>
                  <option value="INVESTOR">INVESTOR (Nhà đầu tư)</option>
                  <option value="SYSTEM_ADMIN">SYSTEM_ADMIN (Quản trị hệ thống)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-stone-300 mb-1.5">
                  Chi nhánh áp dụng {roleKey !== 'SYSTEM_ADMIN' && <span className="text-emerald-400 font-mono">*</span>}
                </label>
                {roleKey === 'SYSTEM_ADMIN' ? (
                  <div className="h-10 flex items-center px-3 rounded-md bg-stone-800/50 border border-stone-800 text-xs text-stone-400">
                    Toàn hệ thống (Không giới hạn)
                  </div>
                ) : (
                  <select
                    value={branchId}
                    onChange={(e) => setBranchId(e.target.value)}
                    className="w-full h-10 rounded-md border border-stone-800 bg-stone-950 px-3 text-xs text-stone-100 focus:outline-none focus:border-emerald-500"
                    required
                  >
                    {filteredBranches.length === 0 ? (
                      <option value="">(Pháp nhân chưa có chi nhánh nào)</option>
                    ) : (
                      filteredBranches.map((b) => (
                        <option key={b.id} value={b.id}>
                          [{b.branchCode}] {b.name}
                        </option>
                      ))
                    )}
                  </select>
                )}
              </div>
            </div>

            {/* Validity Range */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-stone-300 mb-1.5">
                  Ngày bắt đầu hiệu lực <span className="text-emerald-400 font-mono">*</span>
                </label>
                <Input
                  type="date"
                  value={validFrom}
                  onChange={(e) => setValidFrom(e.target.value)}
                  className="font-mono bg-stone-950 border-stone-800 text-stone-100 focus:border-emerald-500 text-xs"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-stone-300 mb-1.5">
                  Ngày hết hạn (để trống nếu vô thời hạn)
                </label>
                <Input
                  type="date"
                  value={validTo}
                  onChange={(e) => setValidTo(e.target.value)}
                  className="font-mono bg-stone-950 border-stone-800 text-stone-100 focus:border-emerald-500 text-xs"
                />
              </div>
            </div>

            {/* PERMISSION PREVIEW CARD */}
            <div className="mt-4 rounded-lg border border-emerald-500/20 bg-emerald-950/20 p-4 text-xs">
              <div className="flex items-center space-x-2 text-emerald-400 font-semibold mb-2">
                <Eye className="h-4 w-4" />
                <span>Xem trước quyền hạn (Permission Preview)</span>
              </div>
              <p className="text-stone-300 mb-2 font-medium">{rolePreview.title}:</p>
              <p className="text-stone-400 text-[11px] mb-3 leading-relaxed">{rolePreview.desc}</p>

              <div className="space-y-1.5">
                {rolePreview.allowed.map((item, idx) => (
                  <div key={idx} className="flex items-start space-x-1.5 text-emerald-300/90 text-[11px]">
                    <span className="text-emerald-400 font-bold">✓</span>
                    <span>{item}</span>
                  </div>
                ))}
                {rolePreview.forbidden.map((item, idx) => (
                  <div key={idx} className="flex items-start space-x-1.5 text-amber-400/90 text-[11px]">
                    <span className="text-amber-400 font-bold">✕</span>
                    <span>{item}</span>
                  </div>
                ))}
              </div>

              <div className="mt-3 pt-2 border-t border-emerald-500/10 flex items-center justify-between text-[11px] text-stone-400 font-mono">
                <span>Người nhận: {selectedUser?.displayName}</span>
                <span>Phạm vi: {roleKey === 'SYSTEM_ADMIN' ? 'GLOBAL' : 'BRANCH-SCOPED'}</span>
              </div>
            </div>

            {/* Actions */}
            <div className="mt-6 flex justify-end space-x-2 pt-4 border-t border-stone-800">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isLoading}
                className="border-stone-800 text-stone-300 hover:bg-stone-800"
              >
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={isLoading || (roleKey !== 'SYSTEM_ADMIN' && !branchId)}
                className="bg-emerald-600 text-white hover:bg-emerald-500 min-w-[120px]"
              >
                {isLoading ? (
                  <span className="flex items-center space-x-2">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Đang lưu...</span>
                  </span>
                ) : (
                  'Xác nhận Cấp quyền'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
