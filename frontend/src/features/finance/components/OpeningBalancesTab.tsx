import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Lock,
  Calendar,
  Wallet,
  Landmark,
  Package,
  CreditCard,
  TrendingUp,
  Scale,
  FileText,
  Download,
  RefreshCw,
  Loader2,
  ShieldCheck,
  Check,
  X,
  Info,
  FolderOpen,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import type { EvidenceFileDto } from '@/features/evidence/types'
import { evidenceApi } from '@/features/evidence/evidenceApi'
import { financeApi, formatVnd } from '../api/financeApi'
import {
  OPENING_BATCH_STATUS_BADGES,
} from '../types/finance.types'
import type {
  FinancialAccountDto,
  OpeningBalanceStatusResponse,
  CreateOpeningBatchPayload,
  AccountOpeningBalanceInput,
} from '../types/finance.types'

export interface OpeningBalancesTabProps {
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  onRefreshAccounts?: () => void
}

export const OpeningBalancesTab: React.FC<OpeningBalancesTabProps> = ({
  branchId,
  branchName,
  accounts,
  onRefreshAccounts,
}) => {
  // Main data state
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [statusData, setStatusData] = useState<OpeningBalanceStatusResponse | null>(null)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Wizard form inputs (for View 2: isPosted === false)
  const [cutoverDate, setCutoverDate] = useState<string>(() => {
    return new Date().toISOString().split('T')[0]
  })
  const [note, setNote] = useState<string>('')
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null)
  const [evidenceFileName, setEvidenceFileName] = useState<string | null>(null)
  const [showDropzone, setShowDropzone] = useState<boolean>(false)
  const [inputBalances, setInputBalances] = useState<Record<string, string>>({})

  // Modal & operation states
  const [isConfirmPostModalOpen, setIsConfirmPostModalOpen] = useState<boolean>(false)
  const [isSavingDraft, setIsSavingDraft] = useState<boolean>(false)
  const [isPostingBatch, setIsPostingBatch] = useState<boolean>(false)
  const [downloadingFileId, setDownloadingFileId] = useState<string | null>(null)

  // Fetch opening balance status
  const fetchOpeningStatus = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setErrorNotice(null)
    try {
      const res = await financeApi.getOpeningBalanceStatus(branchId)
      setStatusData(res)

      // Pre-fill form if activeBatch exists
      if (res.activeBatch) {
        const batch = res.activeBatch
        if (batch.cutoverDate) {
          setCutoverDate(batch.cutoverDate)
        }
        if (batch.note) {
          setNote(batch.note)
        }
        if (batch.evidenceFileId) {
          setEvidenceFileId(batch.evidenceFileId)
        }
        // Populate account input balances
        if (batch.accountBalances && batch.accountBalances.length > 0) {
          const map: Record<string, string> = {}
          batch.accountBalances.forEach((ab) => {
            map[ab.accountId] = String(ab.openingBalance)
          })
          setInputBalances(map)
        }
      }
    } catch (err: unknown) {
      console.error('Failed to load opening balance status:', err)
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      const msg =
        apiErr.response?.data?.detail ||
        apiErr.response?.data?.message ||
        'Không thể tải dữ liệu chốt số dư đầu kỳ của chi nhánh.'
      setErrorNotice(msg)
    } finally {
      setIsLoading(false)
    }
  }, [branchId])

  useEffect(() => {
    fetchOpeningStatus()
  }, [fetchOpeningStatus])

  // Active accounts in this branch
  const activeAccounts = useMemo(() => {
    return accounts.filter((a) => a.status === 'ACTIVE')
  }, [accounts])

  // Real-time calculation of inputs for Live Preview
  const liveTotals = useMemo(() => {
    let totalCash = 0
    let totalBank = 0

    activeAccounts.forEach((acc) => {
      const rawVal = inputBalances[acc.id]
      const numVal = rawVal ? parseFloat(rawVal) || 0 : 0
      if (acc.accountKind === 'CASH') {
        totalCash += numVal
      } else {
        totalBank += numVal
      }
    })

    const batch = statusData?.activeBatch
    const inventory = batch?.totalInventoryValuation ?? 0
    const receivables = batch?.totalReceivableAmount ?? 0
    const payables = batch?.totalPayableAmount ?? 0
    const tax = batch?.totalTaxLiabilityAmount ?? 0

    const netWorkingCapital = totalCash + totalBank + inventory + receivables - (payables + tax)

    return {
      totalCash,
      totalBank,
      inventory,
      receivables,
      payables,
      tax,
      netWorkingCapital,
    }
  }, [activeAccounts, inputBalances, statusData?.activeBatch])

  // Handle balance input change
  const handleBalanceChange = (accountId: string, value: string) => {
    setInputBalances((prev) => ({
      ...prev,
      [accountId]: value,
    }))
  }

  // Handle Evidence Upload
  const handleEvidenceUploaded = (file: EvidenceFileDto) => {
    setEvidenceFileId(file.id)
    setEvidenceFileName(file.originalFilename)
    setShowDropzone(false)
    setSuccessToast(`Đã tải lên chứng từ: ${file.originalFilename}`)
    setTimeout(() => setSuccessToast(null), 4000)
  }

  // Handle Evidence Download
  const handleDownloadEvidence = async (fileId: string) => {
    try {
      setDownloadingFileId(fileId)
      const tokenRes = await evidenceApi.getEvidenceDownloadToken(fileId)
      const url = evidenceApi.getEvidenceContentUrl(fileId, tokenRes.token)
      window.open(url, '_blank')
    } catch (err) {
      console.error('Failed to download evidence:', err)
      setErrorNotice('Không thể tải chứng từ kiểm kê. Vui lòng kiểm tra quyền truy cập.')
    } finally {
      setDownloadingFileId(null)
    }
  }

  // Build payload
  const buildPayload = (): CreateOpeningBatchPayload => {
    const accountBalancesPayload: AccountOpeningBalanceInput[] = activeAccounts.map((acc) => {
      const rawVal = inputBalances[acc.id]
      const numVal = rawVal ? parseFloat(rawVal) || 0 : 0
      return {
        accountId: acc.id,
        openingBalance: Math.max(0, numVal),
      }
    })

    return {
      cutoverDate: cutoverDate || new Date().toISOString().split('T')[0],
      note: note.trim() || undefined,
      evidenceFileId: evidenceFileId || undefined,
      accountBalances: accountBalancesPayload,
    }
  }

  // Save Draft action
  const handleSaveDraft = async () => {
    setIsSavingDraft(true)
    setErrorNotice(null)
    try {
      const payload = buildPayload()
      const savedBatch = await financeApi.saveOpeningBatch(branchId, payload)
      setStatusData((prev) => (prev ? { ...prev, activeBatch: savedBatch } : null))
      setSuccessToast('Đã lưu bản nháp số dư đầu kỳ thành công.')
      setTimeout(() => setSuccessToast(null), 4000)
    } catch (err: unknown) {
      console.error('Failed to save draft:', err)
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      const msg =
        apiErr.response?.data?.detail ||
        apiErr.response?.data?.message ||
        'Không thể lưu bản nháp số dư đầu kỳ.'
      setErrorNotice(msg)
    } finally {
      setIsSavingDraft(false)
    }
  }

  // Confirm Post Action
  const handleConfirmPost = async () => {
    setIsPostingBatch(true)
    setErrorNotice(null)
    try {
      // 1. Ensure latest values are saved first
      const payload = buildPayload()
      const savedBatch = await financeApi.saveOpeningBatch(branchId, payload)

      // 2. Post the batch (Owner action)
      const postedBatch = await financeApi.postOpeningBatch(branchId, savedBatch.id)

      // 3. Update view
      setStatusData({
        isPosted: true,
        cutoverDate: postedBatch.cutoverDate,
        activeBatch: postedBatch,
      })
      setIsConfirmPostModalOpen(false)
      setSuccessToast(
        'Đã chốt sổ thành công! Các bút toán ghi tăng số dư khả dụng ban đầu đã được cập nhật vào sổ quỹ.'
      )
      setTimeout(() => setSuccessToast(null), 6000)

      // 4. Trigger account balance refresh in header
      if (onRefreshAccounts) {
        onRefreshAccounts()
      }
    } catch (err: unknown) {
      console.error('Failed to post opening batch:', err)
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      const msg =
        apiErr.response?.data?.detail ||
        apiErr.response?.data?.message ||
        'Không thể chốt số dư đầu kỳ. Vui lòng kiểm tra quyền Chủ sở hữu (OWNER).'
      setErrorNotice(msg)
    } finally {
      setIsPostingBatch(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 space-y-3 bg-white rounded-2xl border border-slate-200">
        <Loader2 className="w-8 h-8 animate-spin text-[#304638]" />
        <span className="text-xs font-medium text-slate-500">
          Đang kiểm tra trạng thái chốt số dư đầu kỳ...
        </span>
      </div>
    )
  }

  const isPosted = statusData?.isPosted === true
  const activeBatch = statusData?.activeBatch

  return (
    <div className="space-y-6">
      {/* Toast Notification */}
      {successToast && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800 shadow-sm animate-in fade-in duration-200">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium">{successToast}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessToast(null)}
            className="text-emerald-700 hover:text-emerald-900 cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Error Notice */}
      {errorNotice && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center justify-between text-xs text-rose-800 shadow-sm">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorNotice}</span>
          </div>
          <button
            type="button"
            onClick={() => setErrorNotice(null)}
            className="text-rose-700 hover:text-rose-900 cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* ==================================================================== */}
      {/* VIEW 1: WHEN isPosted === true (Certified Post-Cutover View)          */}
      {/* ==================================================================== */}
      {isPosted && activeBatch && (
        <div className="space-y-6 animate-in fade-in duration-200">
          {/* 1. Certified Status Banner */}
          <div className="p-5 rounded-2xl bg-gradient-to-r from-emerald-50 via-teal-50 to-emerald-100 border border-emerald-300 shadow-xs">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-sm mt-0.5">
                  <CheckCircle2 className="w-6 h-6" />
                </div>
                <div className="space-y-1">
                  <div className="flex items-center gap-2.5 flex-wrap">
                    <h2 className="text-base font-bold text-emerald-950">
                      CHI NHÁNH ĐÃ CHỐT SỐ DƯ ĐẦU KỲ
                    </h2>
                    <Badge className="bg-emerald-600 hover:bg-emerald-600 text-white font-mono text-[11px] px-2.5 py-0.5">
                      POSTED / ĐÃ CHỐT SỔ
                    </Badge>
                  </div>
                  <p className="text-xs text-emerald-800 leading-relaxed">
                    Hồ sơ chốt sổ đầu kỳ chuyển giao hệ thống đã được phê duyệt và ghi nhận đầy đủ vào sổ quỹ kế toán.
                    Số dư khả dụng ban đầu đã sẵn sàng cho vận hành chi nhánh.
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={fetchOpeningStatus}
                  className="h-8 text-xs text-emerald-800 border-emerald-300 bg-white/80 hover:bg-white flex items-center gap-1.5 cursor-pointer"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Làm mới</span>
                </Button>
              </div>
            </div>
          </div>

          {/* 2. Audit & Cutover Information Strip */}
          <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
              <div className="space-y-1">
                <span className="text-slate-400 font-medium flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  Ngày chốt sổ (Cutover Date)
                </span>
                <p className="font-semibold text-slate-800 text-sm">
                  {activeBatch.cutoverDate
                    ? new Date(activeBatch.cutoverDate).toLocaleDateString('vi-VN')
                    : '-'}
                </p>
              </div>

              <div className="space-y-1">
                <span className="text-slate-400 font-medium flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-slate-400" />
                  Người duyệt chốt sổ
                </span>
                <p className="font-semibold text-slate-800 text-sm">
                  {activeBatch.postedByName || 'Chủ sở hữu cơ sở'}
                </p>
              </div>

              <div className="space-y-1">
                <span className="text-slate-400 font-medium flex items-center gap-1">
                  <Lock className="w-3.5 h-3.5 text-slate-400" />
                  Thời điểm khóa sổ
                </span>
                <p className="font-semibold text-slate-800 text-sm">
                  {activeBatch.postedAt
                    ? new Date(activeBatch.postedAt).toLocaleString('vi-VN')
                    : '-'}
                </p>
              </div>

              <div className="space-y-1">
                <span className="text-slate-400 font-medium flex items-center gap-1">
                  <FileText className="w-3.5 h-3.5 text-slate-400" />
                  Chứng từ kiểm kê / Biên bản
                </span>
                {activeBatch.evidenceFileId ? (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDownloadEvidence(activeBatch.evidenceFileId!)}
                    disabled={downloadingFileId === activeBatch.evidenceFileId}
                    className="h-6 px-2 text-xs text-emerald-700 hover:text-emerald-900 hover:bg-emerald-50 flex items-center gap-1 cursor-pointer"
                  >
                    {downloadingFileId === activeBatch.evidenceFileId ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Download className="w-3.5 h-3.5" />
                    )}
                    <span>Tải chứng từ đính kèm</span>
                  </Button>
                ) : (
                  <p className="text-slate-400 italic">Không có chứng từ</p>
                )}
              </div>
            </div>

            {activeBatch.note && (
              <div className="mt-3 pt-3 border-t border-slate-100 flex items-start gap-2 text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg">
                <Info className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                <div>
                  <span className="font-semibold text-slate-700">Ghi chú chốt sổ: </span>
                  <span>{activeBatch.note}</span>
                </div>
              </div>
            )}
          </div>

          {/* 3. 7 KPI Summary Cards */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600 flex items-center gap-1.5">
                <FolderOpen className="w-4 h-4 text-[#304638]" />
                Bảng Cân Đối Vốn & Số Dư Đầu Kỳ
              </h3>
              <Badge variant="outline" className="text-[10px] font-mono text-slate-500">
                7 Trụ cột tài chính
              </Badge>
            </div>

            {/* 6 Sub-KPIs Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {/* Card 1: Total Cash */}
              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1.5">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span className="font-medium flex items-center gap-1.5">
                    <Wallet className="w-4 h-4 text-emerald-600" />
                    Tiền mặt ban đầu
                  </span>
                  <Badge variant="secondary" className="text-[10px] font-mono bg-emerald-50 text-emerald-800">
                    CASH
                  </Badge>
                </div>
                <div className="text-lg font-bold text-slate-900">
                  {formatVnd(activeBatch.totalCashAmount)}
                </div>
                <p className="text-[11px] text-slate-400">
                  Tổng số dư quỹ tiền mặt tại quầy & két chi nhánh
                </p>
              </div>

              {/* Card 2: Total Bank */}
              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1.5">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span className="font-medium flex items-center gap-1.5">
                    <Landmark className="w-4 h-4 text-blue-600" />
                    Tiền gửi ngân hàng ban đầu
                  </span>
                  <Badge variant="secondary" className="text-[10px] font-mono bg-blue-50 text-blue-800">
                    BANK
                  </Badge>
                </div>
                <div className="text-lg font-bold text-slate-900">
                  {formatVnd(activeBatch.totalBankAmount)}
                </div>
                <p className="text-[11px] text-slate-400">
                  Tổng số dư theo sao kê ngân hàng chính thức
                </p>
              </div>

              {/* Card 3: Opening Inventory */}
              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1.5">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span className="font-medium flex items-center gap-1.5">
                    <Package className="w-4 h-4 text-purple-600" />
                    Tồn kho ban đầu
                  </span>
                </div>
                <div className="text-lg font-bold text-slate-900">
                  {formatVnd(activeBatch.totalInventoryValuation)}
                </div>
                <p className="text-[11px] text-slate-400">
                  Giá trị kiểm kê kho nguyên vật liệu đầu kỳ (WAC)
                </p>
              </div>

              {/* Card 4: Opening Payables */}
              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1.5">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span className="font-medium flex items-center gap-1.5">
                    <CreditCard className="w-4 h-4 text-amber-600" />
                    Công nợ NCC đầu kỳ
                  </span>
                  <Badge variant="secondary" className="text-[10px] font-mono bg-amber-50 text-amber-800">
                    PHẢI TRẢ
                  </Badge>
                </div>
                <div className="text-lg font-bold text-amber-700">
                  {formatVnd(activeBatch.totalPayableAmount)}
                </div>
                <p className="text-[11px] text-slate-400">
                  Nợ nhà cung cấp chưa thanh toán trước cutover
                </p>
              </div>

              {/* Card 5: Opening Receivables */}
              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1.5">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span className="font-medium flex items-center gap-1.5">
                    <TrendingUp className="w-4 h-4 text-teal-600" />
                    Khoản phải thu đầu kỳ
                  </span>
                  <Badge variant="secondary" className="text-[10px] font-mono bg-teal-50 text-teal-800">
                    PHẢI THU
                  </Badge>
                </div>
                <div className="text-lg font-bold text-slate-900">
                  {formatVnd(activeBatch.totalReceivableAmount)}
                </div>
                <p className="text-[11px] text-slate-400">
                  Phải thu đối tác / ứng trước chưa tất toán
                </p>
              </div>

              {/* Card 6: Opening Tax Liabilities */}
              <div className="p-4 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1.5">
                <div className="flex items-center justify-between text-xs text-slate-500">
                  <span className="font-medium flex items-center gap-1.5">
                    <Scale className="w-4 h-4 text-rose-600" />
                    Nợ thuế đầu kỳ
                  </span>
                  <Badge variant="secondary" className="text-[10px] font-mono bg-rose-50 text-rose-800">
                    THUẾ / NS
                  </Badge>
                </div>
                <div className="text-lg font-bold text-rose-700">
                  {formatVnd(activeBatch.totalTaxLiabilityAmount)}
                </div>
                <p className="text-[11px] text-slate-400">
                  Nghĩa vụ thuế môn bài, GTGT, TNDN còn nợ đầu kỳ
                </p>
              </div>
            </div>

            {/* Featured Card 7: Net Working Capital (Prominent Box) */}
            <div className="p-6 rounded-2xl bg-gradient-to-br from-[#304638] via-[#24372c] to-[#1a2820] text-white shadow-lg space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                    <ShieldCheck className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold tracking-tight text-white uppercase">
                      Vốn Lưu Động Ròng Ban Đầu (Net Opening Working Capital)
                    </h4>
                    <p className="text-xs text-emerald-200/80">
                      Chỉ số thanh khoản cơ bản khi chi nhánh bắt đầu vận hành hệ thống MTMS
                    </p>
                  </div>
                </div>

                <div className="text-2xl sm:text-3xl font-extrabold text-emerald-300 font-mono">
                  {formatVnd(activeBatch.netWorkingCapital)}
                </div>
              </div>

              <div className="pt-3 border-t border-emerald-500/30 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs text-emerald-100/90 font-mono">
                <div className="bg-emerald-950/60 px-3 py-2 rounded-xl border border-emerald-500/20">
                  <span className="text-emerald-400 font-bold">Công thức chuẩn hóa: </span>
                  <span>(Tiền mặt + Ngân hàng + Tồn kho + Phải thu) - (Phải trả + Nợ thuế)</span>
                </div>
                <div className="flex items-center gap-3 text-[11px]">
                  <span>
                    Tài sản lưu động: <strong className="text-emerald-300">+{formatVnd(activeBatch.totalCashAmount + activeBatch.totalBankAmount + activeBatch.totalInventoryValuation + activeBatch.totalReceivableAmount)}</strong>
                  </span>
                  <span>|</span>
                  <span>
                    Nợ ngắn hạn: <strong className="text-rose-300">-{formatVnd(activeBatch.totalPayableAmount + activeBatch.totalTaxLiabilityAmount)}</strong>
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* 4. Table of Financial Accounts Opening Balances */}
          <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-800">
                  Chi Tiết Số Dư Đầu Kỳ Các Tài Khoản Tài Chính
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Các bút toán ghi tăng số dư đã được đẩy vào Sổ nhật ký dòng tiền (Cash Ledger)
                </p>
              </div>
              <Badge variant="outline" className="text-xs font-mono text-slate-600">
                {activeBatch.accountBalances?.length || 0} tài khoản
              </Badge>
            </div>

            <div className="overflow-x-auto border border-slate-100 rounded-xl">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-100">
                  <tr>
                    <th className="py-3 px-4">Tên tài khoản tài chính</th>
                    <th className="py-3 px-4">Loại tài khoản</th>
                    <th className="py-3 px-4 text-right">Số dư đầu kỳ ghi nhận</th>
                    <th className="py-3 px-4">Bút toán sổ quỹ liên kết</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {(!activeBatch.accountBalances || activeBatch.accountBalances.length === 0) && (
                    <tr>
                      <td colSpan={4} className="py-8 text-center text-slate-400">
                        Không có chi tiết tài khoản nào trong đợt chốt sổ này.
                      </td>
                    </tr>
                  )}
                  {activeBatch.accountBalances?.map((ab) => (
                    <tr key={ab.id || ab.accountId} className="hover:bg-slate-50/60 transition-colors">
                      <td className="py-3 px-4 font-semibold text-slate-900">
                        {ab.accountName || 'Tài khoản tài chính'}
                      </td>
                      <td className="py-3 px-4">
                        {ab.accountKind === 'CASH' ? (
                          <Badge variant="secondary" className="bg-emerald-50 text-emerald-800 border-emerald-200 text-[10px]">
                            <Wallet className="w-3 h-3 mr-1" />
                            Tiền mặt quầy
                          </Badge>
                        ) : (
                          <Badge variant="secondary" className="bg-blue-50 text-blue-800 border-blue-200 text-[10px]">
                            <Landmark className="w-3 h-3 mr-1" />
                            Tài khoản ngân hàng
                          </Badge>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right font-bold text-emerald-700 font-mono text-sm">
                        {formatVnd(ab.openingBalance)}
                      </td>
                      <td className="py-3 px-4 font-mono text-[11px] text-slate-500">
                        {ab.cashLedgerEntryId ? (
                          <span className="inline-flex items-center gap-1 text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                            <Check className="w-3 h-3" />
                            Đã ghi sổ ({ab.cashLedgerEntryId.slice(0, 8)}...)
                          </span>
                        ) : (
                          <span className="text-slate-400 italic">Số dư 0 ₫ / Không tạo bút toán</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* 5. Immutability notice / Audit tag */}
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-3 text-xs text-slate-600">
            <Lock className="w-4 h-4 text-slate-500 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <span className="font-semibold text-slate-800">
                Khóa sổ bất biến theo chuẩn kiểm toán MTMS:
              </span>
              <p>
                Đợt chốt sổ đầu kỳ này đã được ghi nhận vào cơ sở dữ liệu và khóa vĩnh viễn. Không cho phép xóa,
                sửa hoặc chốt sổ lại. Bất kỳ chênh lệch số dư nào phát sinh sau thời điểm này đều phải được giải trình
                và điều chỉnh thông qua tab <strong>Kiểm kê & Đối soát quỹ</strong>.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* VIEW 2: WHEN isPosted === false (Onboarding & Cutover Entry Wizard)  */}
      {/* ==================================================================== */}
      {!isPosted && (
        <div className="space-y-6 animate-in fade-in duration-200">
          {/* 1. Amber Guidance Banner */}
          <div className="p-5 rounded-2xl bg-amber-50/80 border border-amber-300 shadow-xs space-y-2">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-xl bg-amber-500 text-white flex items-center justify-center shrink-0 shadow-sm mt-0.5">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <h2 className="text-base font-bold text-amber-950">
                    CHI NHÁNH CHƯA CHỐT SỐ DƯ ĐẦU KỲ
                  </h2>
                  <Badge
                    variant="outline"
                    className={`font-mono text-[11px] ${
                      activeBatch
                        ? OPENING_BATCH_STATUS_BADGES[activeBatch.status]?.badgeClass
                        : 'border-amber-600 text-amber-900 bg-amber-100'
                    }`}
                  >
                    {activeBatch
                      ? OPENING_BATCH_STATUS_BADGES[activeBatch.status]?.label || activeBatch.status
                      : 'CHƯA KHỞI TẠO'}
                  </Badge>
                </div>
                <p className="text-xs text-amber-900 leading-relaxed">
                  Chi nhánh chưa chốt số dư đầu kỳ. Thiết lập số dư tiền mặt, tiền gửi ngân hàng và chứng từ chốt sổ
                  để khởi tạo sổ sách kế toán. MTMS áp dụng nguyên tắc chốt sổ đầu kỳ:
                  Không nhập lại lịch sử giao dịch cũ, chỉ chốt số dư thực tế tại ngày bắt đầu vận hành hệ thống.
                </p>
              </div>
            </div>
          </div>

          {/* 2. Cutover Configuration Card */}
          <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-4">
            <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
              <Calendar className="w-4 h-4 text-[#304638]" />
              Thiết Lập Ngày Chốt Sổ & Chứng Từ Đính Kèm
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Cutover Date Picker */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Ngày chốt số dư đầu kỳ (Cutover Date) <span className="text-rose-500">*</span>
                </label>
                <Input
                  type="date"
                  value={cutoverDate}
                  onChange={(e) => setCutoverDate(e.target.value)}
                  className="text-xs"
                />
                <p className="text-[11px] text-slate-400">
                  Ngày ghi nhận số dư ban đầu làm mốc bắt đầu tính dòng tiền trên MTMS.
                </p>
              </div>

              {/* Note input */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Ghi chú biên bản chốt sổ
                </label>
                <Input
                  type="text"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Ví dụ: Biên bản kiểm kê quỹ và chốt sao kê ngân hàng chuyển giao hệ thống..."
                  className="text-xs"
                />
                <p className="text-[11px] text-slate-400">
                  Thông tin tham chiếu biên bản bàn giao hoặc quyết định chốt sổ.
                </p>
              </div>
            </div>

            {/* Evidence Document Section */}
            <div className="pt-3 border-t border-slate-100 space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <FileText className="w-4 h-4 text-slate-500" />
                  Chứng từ kiểm kê & sao kê đính kèm (Biên bản chốt sổ)
                </label>
                {evidenceFileId && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setEvidenceFileId(null)
                      setEvidenceFileName(null)
                      setShowDropzone(true)
                    }}
                    className="h-6 text-[11px] text-rose-600 hover:text-rose-800 hover:bg-rose-50 cursor-pointer"
                  >
                    Thay đổi chứng từ
                  </Button>
                )}
              </div>

              {/* Uploaded File Banner */}
              {evidenceFileId && (
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800">
                  <div className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <div>
                      <span className="font-semibold">Đã đính kèm chứng từ: </span>
                      <span>{evidenceFileName || evidenceFileId}</span>
                    </div>
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDownloadEvidence(evidenceFileId)}
                    disabled={downloadingFileId === evidenceFileId}
                    className="h-6 text-xs text-emerald-700 hover:text-emerald-900 cursor-pointer flex items-center gap-1"
                  >
                    {downloadingFileId === evidenceFileId ? (
                      <Loader2 className="w-3 h-3 animate-spin" />
                    ) : (
                      <Download className="w-3 h-3" />
                    )}
                    <span>Tải về</span>
                  </Button>
                </div>
              )}

              {/* Dropzone trigger / component */}
              {!evidenceFileId && !showDropzone && (
                <div
                  onClick={() => setShowDropzone(true)}
                  className="p-4 rounded-xl border border-dashed border-slate-300 hover:border-slate-400 bg-slate-50 hover:bg-slate-100/60 transition-colors cursor-pointer text-center space-y-1"
                >
                  <FileText className="w-6 h-6 text-slate-400 mx-auto" />
                  <p className="text-xs font-semibold text-slate-700">
                    Bấm để tải lên biên bản kiểm kê hoặc file sao kê ngân hàng
                  </p>
                  <p className="text-[11px] text-slate-400">
                    Định dạng hỗ trợ: PDF, PNG, JPG, JPEG (Tối đa 10MB)
                  </p>
                </div>
              )}

              {showDropzone && !evidenceFileId && (
                <div className="p-3 rounded-xl border border-slate-200 bg-slate-50 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-700">Tải lên tệp chứng từ:</span>
                    <button
                      type="button"
                      onClick={() => setShowDropzone(false)}
                      className="text-slate-400 hover:text-slate-600"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  <EvidenceDropzone
                    branchId={branchId}
                    onUploadSuccess={handleEvidenceUploaded}
                    defaultVisibility="BRANCH_PRIVATE"
                  />
                </div>
              )}
            </div>
          </div>

          {/* 3. Financial Accounts Opening Balance Table */}
          <div className="p-5 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-4">
            <div>
              <h3 className="text-sm font-bold text-slate-800">
                Nhập Số Dư Đầu Kỳ Cho Từng Tài Khoản
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Nhập số dư tiền mặt tại quầy và số dư tiền gửi ngân hàng theo biên bản kiểm kê tại ngày chốt sổ.
              </p>
            </div>

            {activeAccounts.length === 0 ? (
              <div className="p-6 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-800 text-center space-y-1">
                <AlertCircle className="w-6 h-6 text-amber-600 mx-auto" />
                <p className="font-semibold">Chi nhánh chưa có tài khoản tài chính nào hoạt động.</p>
                <p className="text-amber-700">
                  Vui lòng tạo ít nhất một tài khoản Tiền mặt hoặc Ngân hàng trong tab &quot;Sổ quỹ Tiền mặt & Ngân hàng&quot; trước khi thực hiện chốt số dư đầu kỳ.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto border border-slate-100 rounded-xl">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-100">
                    <tr>
                      <th className="py-3 px-4">Tên tài khoản</th>
                      <th className="py-3 px-4">Loại tài khoản</th>
                      <th className="py-3 px-4">Số dư sổ sách hiện tại</th>
                      <th className="py-3 px-4 w-60">Số dư đầu kỳ nhập mới (VNĐ)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 text-slate-700">
                    {activeAccounts.map((acc) => {
                      const curVal = inputBalances[acc.id] ?? ''
                      const numVal = parseFloat(curVal) || 0

                      return (
                        <tr key={acc.id} className="hover:bg-slate-50/60 transition-colors">
                          <td className="py-3 px-4 font-semibold text-slate-900">
                            {acc.accountName}
                          </td>
                          <td className="py-3 px-4">
                            {acc.accountKind === 'CASH' ? (
                              <Badge variant="secondary" className="bg-emerald-50 text-emerald-800 border-emerald-200 text-[10px]">
                                <Wallet className="w-3 h-3 mr-1" />
                                Tiền mặt quầy
                              </Badge>
                            ) : (
                              <Badge variant="secondary" className="bg-blue-50 text-blue-800 border-blue-200 text-[10px]">
                                <Landmark className="w-3 h-3 mr-1" />
                                Ngân hàng
                              </Badge>
                            )}
                          </td>
                          <td className="py-3 px-4 text-slate-500 font-mono">
                            {formatVnd(acc.currentBalance)}
                          </td>
                          <td className="py-3 px-4">
                            <div className="space-y-1">
                              <Input
                                type="number"
                                min={0}
                                step="any"
                                value={curVal}
                                onChange={(e) => handleBalanceChange(acc.id, e.target.value)}
                                placeholder="0"
                                className="h-8 text-xs font-mono font-bold text-slate-900 text-right"
                              />
                              <div className="text-[11px] text-slate-400 text-right font-mono">
                                {formatVnd(numVal)}
                              </div>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* 4. Live Preview Calculation of Working Capital */}
          <div className="space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600 flex items-center gap-1.5">
              <TrendingUp className="w-4 h-4 text-[#304638]" />
              Ước Tính Vốn Lưu Động Ròng Ban Đầu (Real-time Preview)
            </h3>

            {/* Sub-cards */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div className="p-3.5 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1">
                <span className="text-[11px] font-medium text-slate-400">Tổng tiền mặt</span>
                <div className="text-base font-bold text-slate-900 font-mono">
                  {formatVnd(liveTotals.totalCash)}
                </div>
              </div>
              <div className="p-3.5 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1">
                <span className="text-[11px] font-medium text-slate-400">Tổng tiền gửi NH</span>
                <div className="text-base font-bold text-slate-900 font-mono">
                  {formatVnd(liveTotals.totalBank)}
                </div>
              </div>
              <div className="p-3.5 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1">
                <span className="text-[11px] font-medium text-slate-400">Tồn kho ban đầu</span>
                <div className="text-base font-bold text-slate-900 font-mono">
                  {formatVnd(liveTotals.inventory)}
                </div>
              </div>
              <div className="p-3.5 rounded-xl bg-white border border-slate-200 shadow-xs space-y-1">
                <span className="text-[11px] font-medium text-slate-400">Công nợ & Thuế phải trả</span>
                <div className="text-base font-bold text-rose-600 font-mono">
                  -{formatVnd(liveTotals.payables + liveTotals.tax)}
                </div>
              </div>
            </div>

            {/* Main Preview Banner */}
            <div className="p-5 rounded-2xl bg-gradient-to-r from-emerald-900 to-[#24372c] text-white shadow-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs uppercase font-bold tracking-wider text-emerald-400">
                    Vốn Lưu Động Ròng Dự Kiến (Net Working Capital)
                  </span>
                  <Badge variant="outline" className="text-[10px] font-mono border-emerald-400 text-emerald-300">
                    PREVIEW
                  </Badge>
                </div>
                <p className="text-xs text-emerald-100/80">
                  (Tiền mặt + Ngân hàng + Tồn kho + Phải thu) - (Phải trả NCC + Nợ thuế)
                </p>
              </div>

              <div className="text-2xl sm:text-3xl font-extrabold text-emerald-300 font-mono">
                {formatVnd(liveTotals.netWorkingCapital)}
              </div>
            </div>
          </div>

          {/* 5. Actions Strip */}
          <div className="flex flex-col sm:flex-row items-center justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              disabled={isSavingDraft || isPostingBatch}
              onClick={handleSaveDraft}
              className="w-full sm:w-auto text-xs font-semibold h-9 px-4 text-slate-700 border-slate-300 hover:bg-slate-50 cursor-pointer"
            >
              {isSavingDraft ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                  Đang lưu nháp...
                </>
              ) : (
                'Lưu nháp (Draft)'
              )}
            </Button>

            <Button
              type="button"
              disabled={activeAccounts.length === 0 || isSavingDraft || isPostingBatch}
              onClick={() => setIsConfirmPostModalOpen(true)}
              className="w-full sm:w-auto text-xs font-bold h-9 px-5 bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5 cursor-pointer shadow-xs"
            >
              <Lock className="w-3.5 h-3.5" />
              <span>Chốt số dư đầu kỳ (Owner)</span>
            </Button>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* CONFIRMATION MODAL: POST CUTOVER BATCH (PERMANENT LOCK)              */}
      {/* ==================================================================== */}
      <AnimatePresence>
        {isConfirmPostModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.15 }}
              className="w-full max-w-lg bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden"
            >
              {/* Header */}
              <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-rose-100 text-rose-700 flex items-center justify-center">
                    <Lock className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-bold text-slate-900">
                    Xác Nhận Chốt Số Dư Đầu Kỳ Chi Nhánh
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={() => setIsConfirmPostModalOpen(false)}
                  disabled={isPostingBatch}
                  className="text-slate-400 hover:text-slate-600 cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Body */}
              <div className="p-5 space-y-4 text-xs text-slate-600">
                {/* Warning callout */}
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-rose-800">
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <div className="space-y-1">
                    <span className="font-bold">Cảnh báo khóa sổ vĩnh viễn:</span>
                    <p className="leading-relaxed">
                      Sau khi chốt số dư đầu kỳ, hệ thống sẽ tự động ghi các bút toán ghi tăng số dư khả dụng ban đầu
                      vào sổ quỹ và <strong>khóa vĩnh viễn</strong> đợt chốt sổ này. Thao tác này không thể hoàn tác.
                    </p>
                  </div>
                </div>

                {/* Summary details */}
                <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2 font-mono">
                  <div className="flex justify-between">
                    <span className="text-slate-500 font-sans">Chi nhánh:</span>
                    <span className="font-bold text-slate-800 font-sans">{branchName || branchId}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 font-sans">Ngày chốt sổ (Cutover Date):</span>
                    <span className="font-bold text-slate-800">{cutoverDate}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 font-sans">Tổng tiền mặt ghi tăng:</span>
                    <span className="font-bold text-emerald-700">{formatVnd(liveTotals.totalCash)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 font-sans">Tổng tiền gửi NH ghi tăng:</span>
                    <span className="font-bold text-blue-700">{formatVnd(liveTotals.totalBank)}</span>
                  </div>
                  <div className="flex justify-between pt-2 border-t border-slate-200">
                    <span className="text-slate-500 font-sans">Vốn lưu động ròng dự kiến:</span>
                    <span className="font-bold text-emerald-800 text-sm">
                      {formatVnd(liveTotals.netWorkingCapital)}
                    </span>
                  </div>
                </div>

                <p className="text-[11px] text-slate-500 italic">
                  Chỉ người có quyền Chủ sở hữu (OWNER) của chi nhánh mới được phép thực hiện hành động này.
                </p>
              </div>

              {/* Footer */}
              <div className="p-4 border-t border-slate-100 bg-slate-50 flex items-center justify-end gap-2.5">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsConfirmPostModalOpen(false)}
                  disabled={isPostingBatch}
                  className="text-xs text-slate-600 hover:text-slate-800 cursor-pointer"
                >
                  Hủy bỏ
                </Button>
                <Button
                  type="button"
                  size="sm"
                  onClick={handleConfirmPost}
                  disabled={isPostingBatch}
                  className="text-xs font-bold bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-1.5 cursor-pointer shadow-xs"
                >
                  {isPostingBatch ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang khóa sổ & ghi nhận...</span>
                    </>
                  ) : (
                    <>
                      <Lock className="w-3.5 h-3.5" />
                      <span>Xác nhận chốt sổ vĩnh viễn</span>
                    </>
                  )}
                </Button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default OpeningBalancesTab
