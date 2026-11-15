import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  CreditCard,
  Landmark,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  ShieldCheck,
  Star,
  PlusCircle,
  ArrowRight,
  Info,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { organizationApi } from '../organizationApi'
import type {
  BranchDto,
  BranchBankAccountDto,
  BranchReadinessDto,
} from '../types'

interface BranchBankModalProps {
  isOpen: boolean
  branch: BranchDto | null
  onClose: () => void
  onUpdated?: () => void
}

const POPULAR_BANKS = [
  'Vietcombank (Ngân hàng TMCP Ngoại thương Việt Nam)',
  'MB Bank (Ngân hàng TMCP Quân đội)',
  'Techcombank (Ngân hàng TMCP Kỹ thương Việt Nam)',
  'VietinBank (Ngân hàng TMCP Công thương Việt Nam)',
  'BIDV (Ngân hàng TMCP Đầu tư và Phát triển Việt Nam)',
  'ACB (Ngân hàng TMCP Á Châu)',
  'VPBank (Ngân hàng TMCP Việt Nam Thịnh Vượng)',
  'TPBank (Ngân hàng TMCP Tiên Phong)',
  'Sacombank (Ngân hàng TMCP Sài Gòn Thương Tín)',
  'OCB (Ngân hàng TMCP Phương Đông)',
  'Agribank (Ngân hàng Nông nghiệp & PTNT Việt Nam)',
  'Khác (Nhập tên ngân hàng tùy chọn)...',
]

export const BranchBankModal: React.FC<BranchBankModalProps> = ({
  isOpen,
  branch,
  onClose,
  onUpdated,
}) => {
  // Data states
  const [bankAccounts, setBankAccounts] = useState<BranchBankAccountDto[]>([])
  const [readiness, setReadiness] = useState<BranchReadinessDto | null>(null)
  const [currentBranchStatus, setCurrentBranchStatus] = useState<string>(branch?.status || 'DRAFT')

  // Loading states
  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isActivating, setIsActivating] = useState(false)
  const [settingPrimaryId, setSettingPrimaryId] = useState<string | null>(null)

  // Messages
  const [error, setError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  // Form states
  const [selectedBank, setSelectedBank] = useState(POPULAR_BANKS[0])
  const [customBankName, setCustomBankName] = useState('')
  const [accountHolderName, setAccountHolderName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [isPrimary, setIsPrimary] = useState(true)

  // Load branch bank accounts and readiness
  const loadBranchDetails = useCallback(async (branchId: string) => {
    setIsLoading(true)
    setError(null)
    try {
      const [accountsData, readinessData] = await Promise.all([
        organizationApi.getBranchBankAccounts(branchId),
        organizationApi.getBranchReadiness(branchId),
      ])
      setBankAccounts(accountsData)
      setReadiness(readinessData)
      setCurrentBranchStatus(readinessData.status)

      // If already has accounts, default isPrimary checkbox to false
      if (accountsData.length > 0) {
        setIsPrimary(false)
      } else {
        setIsPrimary(true)
      }
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải thông tin tài khoản ngân hàng và kiểm tra độ sẵn sàng của chi nhánh.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (isOpen && branch) {
      setCurrentBranchStatus(branch.status)
      loadBranchDetails(branch.id)
      setSuccessMsg(null)
      setError(null)
      setAccountNumber('')
      setAccountHolderName('')
      setCustomBankName('')
    }
  }, [isOpen, branch, loadBranchDetails])

  if (!isOpen || !branch) return null

  const handleCreateAccount = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccessMsg(null)

    const resolvedBankName =
      selectedBank.startsWith('Khác') ? customBankName.trim() : selectedBank.split(' (')[0]

    if (!resolvedBankName) {
      setError('Vui lòng chọn hoặc nhập tên ngân hàng')
      return
    }

    const trimmedHolder = accountHolderName.trim().toUpperCase()
    if (!trimmedHolder) {
      setError('Vui lòng nhập tên chủ tài khoản')
      return
    }

    const trimmedNumber = accountNumber.trim().replace(/\s+/g, '')
    if (!trimmedNumber) {
      setError('Vui lòng nhập số tài khoản ngân hàng')
      return
    }
    if (trimmedNumber.length < 6 || trimmedNumber.length > 30) {
      setError('Số tài khoản ngân hàng không hợp lệ (từ 6 - 30 ký tự)')
      return
    }

    setIsSubmitting(true)
    try {
      await organizationApi.createBranchBankAccount(branch.id, {
        bankName: resolvedBankName,
        accountHolderName: trimmedHolder,
        accountNumber: trimmedNumber,
        isPrimary: bankAccounts.length === 0 ? true : isPrimary,
      })

      setSuccessMsg('Đã thêm tài khoản ngân hàng thành công')
      setAccountNumber('')
      setAccountHolderName('')
      setCustomBankName('')

      // Reload accounts and readiness
      await loadBranchDetails(branch.id)
      onUpdated?.()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể thêm tài khoản ngân hàng. Vui lòng kiểm tra lại.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSetPrimary = async (accountId: string) => {
    setError(null)
    setSuccessMsg(null)
    setSettingPrimaryId(accountId)
    try {
      await organizationApi.setPrimaryBankAccount(branch.id, accountId)
      setSuccessMsg('Đã cập nhật tài khoản chính thành công')
      await loadBranchDetails(branch.id)
      onUpdated?.()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể đặt làm tài khoản chính.'
      )
    } finally {
      setSettingPrimaryId(null)
    }
  }

  const handleActivateBranch = async () => {
    setError(null)
    setSuccessMsg(null)
    setIsActivating(true)
    try {
      const updated = await organizationApi.activateBranch(branch.id)
      setCurrentBranchStatus(updated.status)
      setSuccessMsg(`Chi nhánh [${updated.branchCode}] đã được kích hoạt thành công (ACTIVE)`)
      await loadBranchDetails(branch.id)
      onUpdated?.()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Kích hoạt chi nhánh thất bại. Vui lòng kiểm tra lại điều kiện sẵn sàng.'
      )
    } finally {
      setIsActivating(false)
    }
  }

  const isDraft = currentBranchStatus === 'DRAFT'
  const isActive = currentBranchStatus === 'ACTIVE'
  const isReady = readiness?.readyForActivation ?? false

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-stone-950/75 backdrop-blur-sm"
        />

        {/* Modal Window */}
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-2xl max-h-[92vh] overflow-y-auto rounded-xl border border-stone-800 bg-stone-900 p-6 shadow-2xl z-10 text-stone-100 space-y-6"
        >
          {/* Header */}
          <div className="flex items-start justify-between pb-4 border-b border-stone-800">
            <div className="flex items-center space-x-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-950/80 border border-emerald-500/30 text-emerald-400">
                <Landmark className="h-5 w-5" />
              </span>
              <div>
                <div className="flex items-center space-x-2">
                  <h2 className="text-base font-bold text-stone-100">
                    Tài khoản Ngân hàng & Go-Live Readiness
                  </h2>
                  <Badge
                    variant="outline"
                    className={
                      isActive
                        ? 'border-emerald-500/40 text-emerald-300 bg-emerald-950/40 text-[10px]'
                        : isDraft
                        ? 'border-amber-500/40 text-amber-300 bg-amber-950/40 text-[10px]'
                        : 'border-stone-700 text-stone-400 text-[10px]'
                    }
                  >
                    {currentBranchStatus}
                  </Badge>
                </div>
                <p className="text-xs text-stone-400 mt-0.5">
                  Chi nhánh:{' '}
                  <span className="font-mono font-bold text-emerald-400">
                    {branch.branchCode}
                  </span>{' '}
                  — <span className="text-stone-200">{branch.name}</span>
                </p>
              </div>
            </div>

            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-stone-400 hover:bg-stone-800 hover:text-stone-200 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Alerts */}
          {error && (
            <div className="flex items-start space-x-2 rounded-lg border border-red-500/40 bg-red-950/40 p-3 text-xs text-red-300">
              <AlertTriangle className="h-4 w-4 shrink-0 text-red-400 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {successMsg && (
            <div className="flex items-center space-x-2 rounded-lg border border-emerald-500/40 bg-emerald-950/40 p-3 text-xs text-emerald-300">
              <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
              <span>{successMsg}</span>
            </div>
          )}

          {/* Go-Live Readiness Status Card */}
          <div className="rounded-lg border border-stone-800 bg-stone-950/60 p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-stone-300 uppercase tracking-wider font-mono">
                Điều kiện kích hoạt chi nhánh (BR-ORG-03 / TC-003)
              </span>

              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin text-stone-400" />
              ) : isReady || isActive ? (
                <span className="inline-flex items-center space-x-1 text-xs font-semibold text-emerald-400 bg-emerald-950/60 border border-emerald-500/30 px-2.5 py-0.5 rounded-full">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  <span>Sẵn sàng hoạt động</span>
                </span>
              ) : (
                <span className="inline-flex items-center space-x-1 text-xs font-semibold text-amber-400 bg-amber-950/60 border border-amber-500/30 px-2.5 py-0.5 rounded-full">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  <span>Chưa đủ điều kiện</span>
                </span>
              )}
            </div>

            {/* Readiness Details */}
            {isLoading ? (
              <div className="py-2 flex items-center justify-center text-xs text-stone-500 font-mono">
                Đang kiểm tra độ sẵn sàng...
              </div>
            ) : isActive ? (
              <p className="text-xs text-stone-300 leading-relaxed">
                Chi nhánh đang ở trạng thái <strong className="text-emerald-400">ACTIVE</strong>. Đã
                có tài khoản ngân hàng chuyên dụng hoạt động hợp lệ theo chuẩn BR-ORG-03.
              </p>
            ) : isDraft && isReady ? (
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-emerald-950/30 border border-emerald-500/20 rounded-md p-3">
                <div className="space-y-0.5">
                  <p className="text-xs font-medium text-emerald-300">
                    Đã đáp ứng đầy đủ điều kiện go-live!
                  </p>
                  <p className="text-[11px] text-stone-400">
                    Chi nhánh đã có tài khoản ngân hàng hoạt động. Bạn có thể kích hoạt để chi nhánh
                    bắt đầu vận hành.
                  </p>
                </div>
                <Button
                  size="sm"
                  onClick={handleActivateBranch}
                  disabled={isActivating}
                  className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium shrink-0 shadow-lg shadow-emerald-950/50"
                >
                  {isActivating ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                      Đang kích hoạt...
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="h-3.5 w-3.5 mr-1.5" />
                      Kích hoạt Chi nhánh
                      <ArrowRight className="h-3.5 w-3.5 ml-1" />
                    </>
                  )}
                </Button>
              </div>
            ) : (
              <div className="space-y-2 bg-amber-950/20 border border-amber-500/20 rounded-md p-3 text-xs">
                <p className="text-amber-300 font-medium">
                  Chi nhánh chưa thể chuyển sang trạng thái Hoạt động:
                </p>
                <ul className="list-disc list-inside space-y-1 text-stone-300 text-[11px]">
                  {readiness?.missingRequirements && readiness.missingRequirements.length > 0 ? (
                    readiness.missingRequirements.map((req, i) => <li key={i}>{req}</li>)
                  ) : (
                    <li>
                      Cần ít nhất 1 tài khoản ngân hàng hoạt động để chi nhánh có thể mở bán
                      (BR-ORG-03).
                    </li>
                  )}
                </ul>
              </div>
            )}
          </div>

          {/* Bank Accounts List */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-stone-400 font-mono flex items-center space-x-1.5">
                <CreditCard className="h-3.5 w-3.5 text-emerald-400" />
                <span>Tài khoản ngân hàng ({bankAccounts.length})</span>
              </h3>
            </div>

            {isLoading ? (
              <div className="flex h-24 items-center justify-center rounded-lg border border-stone-800 bg-stone-950/40">
                <Loader2 className="h-5 w-5 animate-spin text-emerald-500" />
              </div>
            ) : bankAccounts.length === 0 ? (
              <div className="flex flex-col items-center justify-center p-6 text-center rounded-lg border border-dashed border-stone-800 bg-stone-950/30">
                <CreditCard className="h-7 w-7 text-stone-600 mb-1.5" />
                <p className="text-xs font-medium text-stone-400">
                  Chưa có tài khoản ngân hàng nào được thiết lập.
                </p>
                <p className="text-[11px] text-stone-500 mt-0.5">
                  Vui lòng thêm tài khoản bên dưới để đáp ứng điều kiện mở bán (Go-Live).
                </p>
              </div>
            ) : (
              <div className="space-y-2">
                {bankAccounts.map((acc) => (
                  <div
                    key={acc.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 rounded-lg border border-stone-800 bg-stone-950/50 hover:border-stone-700 transition-colors gap-3"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center space-x-2">
                        <span className="text-xs font-bold text-stone-200">{acc.bankName}</span>
                        {acc.isPrimary ? (
                          <span className="inline-flex items-center space-x-1 text-[10px] font-semibold text-emerald-400 bg-emerald-950/70 border border-emerald-500/30 px-2 py-0.5 rounded">
                            <Star className="h-2.5 w-2.5 fill-emerald-400" />
                            <span>Tài khoản chính</span>
                          </span>
                        ) : null}
                      </div>

                      <div className="flex items-center space-x-3 text-xs">
                        <span className="font-mono font-bold text-emerald-400 tracking-wider">
                          {acc.maskedAccountNumber}
                        </span>
                        <span className="text-stone-400">•</span>
                        <span className="text-stone-300 font-mono text-[11px] uppercase">
                          {acc.accountHolderName}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center space-x-2 shrink-0">
                      {!acc.isPrimary && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleSetPrimary(acc.id)}
                          disabled={settingPrimaryId === acc.id}
                          className="h-7 px-2.5 text-[11px] border-stone-700 text-stone-300 hover:text-emerald-300 hover:border-emerald-500/40 hover:bg-emerald-950/30"
                        >
                          {settingPrimaryId === acc.id ? (
                            <Loader2 className="h-3 w-3 animate-spin mr-1" />
                          ) : (
                            <Star className="h-3 w-3 mr-1 text-amber-400" />
                          )}
                          Đặt làm chính
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Form: Add New Bank Account */}
          <form
            onSubmit={handleCreateAccount}
            className="rounded-lg border border-stone-800 bg-stone-950/40 p-4 space-y-4"
          >
            <div className="flex items-center space-x-2 pb-2 border-b border-stone-800">
              <PlusCircle className="h-4 w-4 text-emerald-400" />
              <h3 className="text-xs font-semibold text-stone-200 uppercase tracking-wider font-mono">
                Thêm tài khoản ngân hàng mới
              </h3>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Bank Name Selector */}
              <div className="sm:col-span-2 space-y-1.5">
                <label className="text-xs font-medium text-stone-300">Ngân hàng</label>
                <select
                  value={selectedBank}
                  onChange={(e) => setSelectedBank(e.target.value)}
                  className="w-full rounded-md border border-stone-800 bg-stone-950 px-3 py-2 text-xs text-stone-100 focus:border-emerald-500 focus:outline-none"
                >
                  {POPULAR_BANKS.map((b) => (
                    <option key={b} value={b} className="bg-stone-900 text-stone-100">
                      {b}
                    </option>
                  ))}
                </select>

                {selectedBank.startsWith('Khác') && (
                  <div className="mt-2">
                    <Input
                      placeholder="Nhập tên ngân hàng (Ví dụ: VPBank, MSB, Nam A Bank...)"
                      value={customBankName}
                      onChange={(e) => setCustomBankName(e.target.value)}
                      className="bg-stone-950 border-stone-800 text-xs text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
                    />
                  </div>
                )}
              </div>

              {/* Account Number */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-stone-300">
                  Số tài khoản <span className="text-red-400">*</span>
                </label>
                <Input
                  placeholder="Ví dụ: 0123456789"
                  value={accountNumber}
                  onChange={(e) => setAccountNumber(e.target.value)}
                  className="bg-stone-950 border-stone-800 text-xs text-stone-100 font-mono placeholder:text-stone-600 focus:border-emerald-500"
                />
              </div>

              {/* Account Holder Name */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-stone-300">
                  Tên chủ tài khoản <span className="text-red-400">*</span>
                </label>
                <Input
                  placeholder="NGUYEN VAN A (Tự động in hoa)"
                  value={accountHolderName}
                  onChange={(e) => setAccountHolderName(e.target.value.toUpperCase())}
                  className="bg-stone-950 border-stone-800 text-xs text-stone-100 font-mono uppercase placeholder:text-stone-600 focus:border-emerald-500"
                />
              </div>
            </div>

            {/* Checkbox Primary */}
            <div className="flex items-center space-x-2 pt-1">
              <input
                type="checkbox"
                id="isPrimaryCheckbox"
                checked={bankAccounts.length === 0 ? true : isPrimary}
                disabled={bankAccounts.length === 0}
                onChange={(e) => setIsPrimary(e.target.checked)}
                className="h-4 w-4 rounded border-stone-700 bg-stone-950 text-emerald-600 focus:ring-emerald-500 focus:ring-offset-stone-900"
              />
              <label
                htmlFor="isPrimaryCheckbox"
                className="text-xs text-stone-300 select-none cursor-pointer"
              >
                Đặt làm tài khoản ngân hàng chính của chi nhánh{' '}
                {bankAccounts.length === 0 && (
                  <span className="text-[11px] text-stone-500">(Mặc định cho tài khoản đầu tiên)</span>
                )}
              </label>
            </div>

            {/* Security note */}
            <div className="flex items-start space-x-2 rounded border border-stone-800/80 bg-stone-950/60 p-2.5 text-[11px] text-stone-400">
              <ShieldCheck className="h-4 w-4 shrink-0 text-emerald-400 mt-0.5" />
              <span>
                Số tài khoản được mã hóa bảo mật ở tầng lưu trữ. Chỉ hiển thị dạng che (masked
                **** **** 1234) trên giao diện theo tiêu chuẩn an toàn dữ liệu tài chính MTMS
                (01-financial-data-safety, 08-sensitive-data).
              </span>
            </div>

            {/* Submit button */}
            <div className="flex justify-end pt-2">
              <Button
                type="submit"
                disabled={isSubmitting}
                className="bg-emerald-600 text-white hover:bg-emerald-500 text-xs font-medium"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  <>
                    <PlusCircle className="h-3.5 w-3.5 mr-1.5" />
                    Lưu tài khoản ngân hàng
                  </>
                )}
              </Button>
            </div>
          </form>

          {/* Footer note */}
          <div className="pt-2 border-t border-stone-800 flex items-center justify-between text-[11px] text-stone-500">
            <span className="flex items-center space-x-1">
              <Info className="h-3.5 w-3.5 text-stone-500" />
              <span>Mỗi chi nhánh cần ít nhất 1 tài khoản hoạt động để mở bán</span>
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={onClose}
              className="border-stone-800 text-stone-400 hover:bg-stone-800 text-xs"
            >
              Đóng
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
