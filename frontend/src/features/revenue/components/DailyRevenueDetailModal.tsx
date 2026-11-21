import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Calendar,
  DollarSign,
  QrCode,
  Smartphone,
  SlidersHorizontal,
  CheckCircle2,
  Clock,
  UserCheck,
  AlertCircle,
  FileCheck2,
  Loader2,
  ExternalLink,
  ShieldCheck,
  Link2,
  Wallet,
  Landmark,
  ArrowDownRight,
  RefreshCw,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { revenueApi, formatVnd } from '../api/revenueApi'
import { evidenceApi } from '@/features/evidence/evidenceApi'
import type { DailyRevenueReportDto } from '../types/revenue.types'
import type { CashLedgerEntryDto } from '@/features/finance'

interface DailyRevenueDetailModalProps {
  isOpen: boolean
  report: DailyRevenueReportDto | null
  branchId: string
  branchName?: string
  onClose: () => void
  onConfirmed?: (updated: DailyRevenueReportDto) => void
}

export const DailyRevenueDetailModal: React.FC<DailyRevenueDetailModalProps> = ({
  isOpen,
  report,
  branchId,
  branchName,
  onClose,
  onConfirmed,
}) => {
  const [isConfirming, setIsConfirming] = useState<boolean>(false)
  const [adjustmentReason, setAdjustmentReason] = useState<string>('')
  const [showAdjustField, setShowAdjustField] = useState<boolean>(false)
  const [isDownloading, setIsDownloading] = useState<boolean>(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Linked cash & bank ledger state (P0-REV-05 / BR-REV-04 / TC-007)
  const [ledgerEntries, setLedgerEntries] = useState<CashLedgerEntryDto[]>([])
  const [isLoadingLedger, setIsLoadingLedger] = useState<boolean>(false)
  const [ledgerError, setLedgerError] = useState<string | null>(null)

  const loadLedgerEntries = useCallback(async () => {
    if (!report || report.status !== 'CONFIRMED' || !branchId) {
      setLedgerEntries([])
      setLedgerError(null)
      setIsLoadingLedger(false)
      return
    }
    setIsLoadingLedger(true)
    setLedgerError(null)
    try {
      const entries = await revenueApi.getRevenueLedgerEntries(branchId, report.id)
      setLedgerEntries(Array.isArray(entries) ? entries : [])
    } catch (err: unknown) {
      console.error('Failed to load linked ledger entries:', err)
      const errorObj = err as {
        message?: string
        response?: { data?: { detail?: string; message?: string } }
      }
      setLedgerError(
        errorObj.response?.data?.detail ||
        errorObj.response?.data?.message ||
        errorObj.message ||
        'Không thể tải dữ liệu sổ quỹ liên kết.'
      )
    } finally {
      setIsLoadingLedger(false)
    }
  }, [branchId, report?.id, report?.status])

  useEffect(() => {
    if (isOpen && report?.status === 'CONFIRMED') {
      loadLedgerEntries()
    } else {
      setLedgerEntries([])
      setLedgerError(null)
      setIsLoadingLedger(false)
    }
  }, [isOpen, report?.status, loadLedgerEntries])

  if (!isOpen || !report) return null

  const getStatusBadge = (status: DailyRevenueReportDto['status']) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-semibold px-2.5 py-0.5">
            <CheckCircle2 className="w-3.5 h-3.5 mr-1 text-emerald-600" />
            ĐÃ XÁC NHẬN (CONFIRMED)
          </Badge>
        )
      case 'SUBMITTED':
        return (
          <Badge className="bg-blue-100 text-blue-800 border-blue-300 font-semibold px-2.5 py-0.5">
            <Clock className="w-3.5 h-3.5 mr-1 text-blue-600" />
            CHỜ DUYỆT (SUBMITTED)
          </Badge>
        )
      case 'DRAFT':
        return (
          <Badge className="bg-amber-100 text-amber-800 border-amber-300 font-semibold px-2.5 py-0.5">
            <Clock className="w-3.5 h-3.5 mr-1 text-amber-600" />
            BẢN NHÁP (DRAFT)
          </Badge>
        )
      case 'SUPERSEDED':
        return (
          <Badge variant="outline" className="bg-zinc-100 text-zinc-600 border-zinc-300 font-semibold px-2.5 py-0.5">
            ĐÃ THAY THẾ (SUPERSEDED)
          </Badge>
        )
      case 'CANCELLED':
        return (
          <Badge variant="destructive" className="font-semibold px-2.5 py-0.5">
            ĐÃ HỦY (CANCELLED)
          </Badge>
        )
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const handleDownloadEvidence = async () => {
    if (!report.evidenceFileId) return
    setIsDownloading(true)
    setErrorMsg(null)
    try {
      const tokenData = await evidenceApi.getEvidenceDownloadToken(report.evidenceFileId)
      const downloadUrl = evidenceApi.getEvidenceContentUrl(report.evidenceFileId, tokenData.token)
      window.open(downloadUrl, '_blank')
    } catch (err: unknown) {
      const errorObj = err as { message?: string }
      setErrorMsg(errorObj.message || 'Không thể mở file bằng chứng.')
    } finally {
      setIsDownloading(false)
    }
  }

  const handleConfirm = async () => {
    setIsConfirming(true)
    setErrorMsg(null)
    try {
      const updated = await revenueApi.confirmDailyRevenue(branchId, report.id, {
        adjustmentReason: adjustmentReason.trim() || undefined,
      })
      if (onConfirmed) {
        onConfirmed(updated)
      }
      onClose()
    } catch (err: unknown) {
      const errorObj = err as {
        response?: { data?: { detail?: string; message?: string } }
        message?: string
      }
      setErrorMsg(
        errorObj.response?.data?.detail ||
        errorObj.response?.data?.message ||
        errorObj.message ||
        'Không thể duyệt báo cáo doanh thu.'
      )
    } finally {
      setIsConfirming(false)
    }
  }

  const canConfirm = report.status === 'SUBMITTED' || report.status === 'DRAFT'

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-2xl bg-[#FFFCF8] rounded-2xl shadow-2xl border border-[#E5DED4] overflow-hidden my-auto"
        >
          {/* Header */}
          <div className="bg-[#304638] text-[#F7F3EC] px-5 py-4 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-[#819B78]/25 rounded-xl text-[#F7F3EC]">
                <Calendar className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h2 className="text-lg font-bold text-white tracking-tight">
                    Doanh thu ngày {report.businessDate}
                  </h2>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-white/20 text-white font-mono">
                    Rev #{report.revision}
                  </span>
                </div>
                <p className="text-xs text-[#C8CEC4]">
                  {branchName ? `Chi nhánh: ${branchName}` : `Mã báo cáo: ${report.id.substring(0, 8)}...`}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-lg text-[#C8CEC4] hover:text-white hover:bg-white/10 transition-colors"
              aria-label="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-5 sm:p-6 space-y-5 max-h-[80vh] overflow-y-auto">
            {/* Status & Revision Bar */}
            <div className="flex flex-wrap items-center justify-between gap-2 p-3 bg-[#F7F3EC] rounded-xl border border-[#E5DED4]">
              <div className="flex items-center space-x-2">
                <span className="text-xs font-semibold text-[#352D26]">Trạng thái:</span>
                {getStatusBadge(report.status)}
              </div>
              {report.isCurrent ? (
                <span className="text-[11px] font-medium text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                  Bản ghi hiện hành (Current)
                </span>
              ) : (
                <span className="text-[11px] font-medium text-zinc-600 bg-zinc-100 px-2 py-0.5 rounded-md border border-zinc-200">
                  Bản lưu lịch sử
                </span>
              )}
            </div>

            {errorMsg && (
              <div className="p-3.5 rounded-xl bg-[#B7423E]/10 border border-[#B7423E]/30 text-[#B7423E] flex items-start space-x-2.5 text-xs">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span className="font-medium">{errorMsg}</span>
              </div>
            )}

            {/* Financial Breakdown Grid */}
            <div className="space-y-3">
              <h3 className="text-xs font-bold text-[#352D26] uppercase tracking-wider">
                Chi tiết phân rã doanh thu
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Tiền mặt */}
                <div className="p-3.5 bg-white rounded-xl border border-[#E5DED4] shadow-xs">
                  <div className="flex items-center justify-between text-xs text-[#786C60] mb-1">
                    <span className="flex items-center gap-1.5 font-medium">
                      <DollarSign className="w-3.5 h-3.5 text-[#304638]" /> Tiền mặt tại quầy
                    </span>
                    <Badge variant="outline" className="text-[10px]">CASH</Badge>
                  </div>
                  <p className="text-lg font-bold text-[#304638]">
                    {formatVnd(report.cashAmount)}
                  </p>
                </div>

                {/* Chuyển khoản */}
                <div className="p-3.5 bg-white rounded-xl border border-[#E5DED4] shadow-xs">
                  <div className="flex items-center justify-between text-xs text-[#786C60] mb-1">
                    <span className="flex items-center gap-1.5 font-medium">
                      <QrCode className="w-3.5 h-3.5 text-[#B96849]" /> Chuyển khoản VietQR
                    </span>
                    <Badge variant="outline" className="text-[10px]">BANK</Badge>
                  </div>
                  <p className="text-lg font-bold text-[#B96849]">
                    {formatVnd(report.bankTransferAmount)}
                  </p>
                </div>

                {/* App giao hàng NET */}
                <div className="p-3.5 bg-white rounded-xl border border-[#E5DED4] shadow-xs">
                  <div className="flex items-center justify-between text-xs text-[#786C60] mb-1">
                    <span className="flex items-center gap-1.5 font-medium">
                      <Smartphone className="w-3.5 h-3.5 text-blue-600" /> App giao hàng NET
                    </span>
                    <Badge variant="outline" className="text-[10px]">APP NET</Badge>
                  </div>
                  <p className="text-lg font-bold text-blue-700">
                    {formatVnd(report.deliveryAppNetAmount)}
                  </p>
                </div>

                {/* Điều chỉnh */}
                <div className="p-3.5 bg-white rounded-xl border border-[#E5DED4] shadow-xs">
                  <div className="flex items-center justify-between text-xs text-[#786C60] mb-1">
                    <span className="flex items-center gap-1.5 font-medium">
                      <SlidersHorizontal className="w-3.5 h-3.5 text-[#786C60]" /> Điều chỉnh khác
                    </span>
                    <Badge variant="outline" className="text-[10px]">ADJUST</Badge>
                  </div>
                  <p className={`text-lg font-bold ${report.otherAdjustmentAmount < 0 ? 'text-[#B7423E]' : 'text-[#786C60]'}`}>
                    {formatVnd(report.otherAdjustmentAmount)}
                  </p>
                </div>
              </div>

              {/* Total Card */}
              <div className="p-4 rounded-xl bg-gradient-to-br from-[#304638] to-[#25372C] text-[#F7F3EC] shadow-md flex items-center justify-between">
                <div>
                  <span className="text-xs uppercase tracking-wider text-[#C8CEC4] font-medium block">
                    TỔNG DOANH THU GHI NHẬN (TOTAL REVENUE)
                  </span>
                  <span className="text-[11px] text-[#819B78]">
                    Công thức: Tiền mặt + Chuyển khoản + App NET + Điều chỉnh
                  </span>
                </div>
                <div className="text-right">
                  <span className="text-2xl sm:text-3xl font-extrabold text-white">
                    {formatVnd(report.totalRevenue)}
                  </span>
                </div>
              </div>
            </div>

            {/* Cash & Bank Ledger Linkage Section (BR-REV-04 / TC-007) */}
            <div className="space-y-3 pt-3 border-t border-[#E5DED4]">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="p-1.5 bg-[#304638]/10 text-[#304638] rounded-lg">
                    <Link2 className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="text-xs font-bold text-[#352D26] uppercase tracking-wider">
                      Liên kết Sổ quỹ tiền mặt & Ngân hàng
                    </h3>
                    <p className="text-[11px] text-[#786C60]">
                      Đối soát tự động giữa dòng tiền thực tế và doanh thu ghi nhận
                    </p>
                  </div>
                </div>
                {report.status === 'CONFIRMED' && (
                  <Badge className="bg-emerald-50 text-emerald-800 border-emerald-200 text-[10px] font-semibold">
                    <CheckCircle2 className="w-3 h-3 mr-1 text-emerald-600" />
                    Đã liên kết sổ quỹ
                  </Badge>
                )}
              </div>

              {/* Summary Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {/* Tiền mặt */}
                <div className="p-3 bg-white rounded-xl border border-[#E5DED4] shadow-xs flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2 bg-emerald-50 text-emerald-700 rounded-lg">
                      <Wallet className="w-4 h-4" />
                    </div>
                    <div>
                      <span className="text-[11px] font-medium text-[#786C60] block">Quỹ tiền mặt (Cash)</span>
                      <span className="text-xs font-bold text-emerald-800">
                        {report.status === 'CONFIRMED'
                          ? `Đã ghi nhận vào Quỹ tiền mặt (+${formatVnd(report.cashAmount)})`
                          : `Dự kiến ghi nhận (+${formatVnd(report.cashAmount)})`}
                      </span>
                    </div>
                  </div>
                  <Badge variant="outline" className="text-[10px] font-mono border-emerald-200 text-emerald-700 bg-emerald-50/50">
                    CASH_LEG
                  </Badge>
                </div>

                {/* Chuyển khoản VietQR */}
                <div className="p-3 bg-white rounded-xl border border-[#E5DED4] shadow-xs flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2 bg-[#B96849]/10 text-[#B96849] rounded-lg">
                      <Landmark className="w-4 h-4" />
                    </div>
                    <div>
                      <span className="text-[11px] font-medium text-[#786C60] block">Quỹ ngân hàng (Bank)</span>
                      <span className="text-xs font-bold text-[#B96849]">
                        {report.status === 'CONFIRMED'
                          ? `Đã ghi nhận vào Quỹ ngân hàng (+${formatVnd(report.bankTransferAmount)})`
                          : `Dự kiến ghi nhận (+${formatVnd(report.bankTransferAmount)})`}
                      </span>
                    </div>
                  </div>
                  <Badge variant="outline" className="text-[10px] font-mono border-[#B96849]/30 text-[#B96849] bg-[#B96849]/5">
                    BANK_LEG
                  </Badge>
                </div>
              </div>

              {/* Purple TC-007 Delivery App Warning */}
              <div className="p-3.5 bg-purple-50/90 rounded-xl border border-purple-200 text-purple-900 text-xs flex items-start gap-2.5">
                <AlertCircle className="w-4 h-4 text-purple-600 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <Badge className="bg-purple-200 text-purple-900 border-purple-300 text-[10px] font-bold px-1.5 py-0">
                      BẢO VỆ DÒNG TIỀN
                    </Badge>
                    <span className="font-bold text-purple-950 text-xs">
                      Doanh thu App giao hàng NET: {formatVnd(report.deliveryAppNetAmount)}
                    </span>
                  </div>
                  <p className="text-[11px] text-purple-800 leading-relaxed">
                    Doanh thu ghi nhận vào P&L ngày kinh doanh, tiền thực tế chờ đối soát từ sàn giao hàng, không tự động tăng số dư ngân hàng.
                  </p>
                </div>
              </div>

              {/* Detailed Linked Ledger Entries Table when CONFIRMED */}
              {report.status === 'CONFIRMED' ? (
                <div className="space-y-2 pt-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-semibold text-[#352D26]">
                      Bút toán sổ quỹ đã hạch toán ({ledgerEntries.length}):
                    </span>
                    {isLoadingLedger && (
                      <span className="text-[11px] text-[#786C60] flex items-center gap-1">
                        <Loader2 className="w-3 h-3 animate-spin" /> Đang tải bút toán...
                      </span>
                    )}
                  </div>

                  {isLoadingLedger ? (
                    <div className="p-4 bg-white rounded-xl border border-[#E5DED4] text-center text-xs text-[#786C60] flex items-center justify-center gap-2">
                      <Loader2 className="w-4 h-4 animate-spin text-[#304638]" />
                      <span>Đang tải thông tin bút toán sổ quỹ liên kết...</span>
                    </div>
                  ) : ledgerError ? (
                    <div className="p-3 bg-red-50 rounded-xl border border-red-200 text-xs text-red-700 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
                        <span>{ledgerError}</span>
                      </div>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={loadLedgerEntries}
                        className="h-7 text-[11px] border-red-200 text-red-800 hover:bg-red-100"
                      >
                        <RefreshCw className="w-3 h-3 mr-1" /> Thử lại
                      </Button>
                    </div>
                  ) : ledgerEntries.length === 0 ? (
                    <div className="p-3 bg-zinc-50 rounded-xl border border-dashed border-zinc-300 text-center text-xs text-zinc-500">
                      Chưa có bút toán sổ quỹ được tạo (hoặc số tiền tiền mặt/chuyển khoản bằng 0).
                    </div>
                  ) : (
                    <div className="bg-white rounded-xl border border-[#E5DED4] overflow-hidden shadow-xs">
                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs">
                          <thead className="bg-[#F7F3EC]/70 border-b border-[#E5DED4] text-[#786C60] text-[10px] font-semibold uppercase tracking-wider">
                            <tr>
                              <th className="py-2 px-3">Tài khoản</th>
                              <th className="py-2 px-3">Danh mục</th>
                              <th className="py-2 px-2 text-center">Hướng</th>
                              <th className="py-2 px-3 text-right">Số tiền</th>
                              <th className="py-2 px-3 text-right">Số dư sau GD</th>
                              <th className="py-2 px-3 text-right">Thời gian ghi nhận</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-[#E5DED4]">
                            {ledgerEntries.map((entry) => (
                              <tr key={entry.id} className="hover:bg-[#F7F3EC]/30">
                                <td className="py-2.5 px-3">
                                  <div className="font-medium text-[#352D26]">
                                    {entry.accountName || (entry.sourceLeg === 'CASH_LEG' ? 'Quỹ tiền mặt chi nhánh' : 'Tài khoản ngân hàng')}
                                  </div>
                                  <span className="font-mono text-[9px] text-[#786C60]">
                                    {entry.sourceLeg || entry.sourceType}
                                  </span>
                                </td>
                                <td className="py-2.5 px-3">
                                  <span className="text-[#352D26] block">
                                    {entry.categoryName || entry.categoryCode || 'Doanh thu bán hàng'}
                                  </span>
                                  {entry.memo && (
                                    <span className="text-[10px] text-[#786C60] line-clamp-1 italic">
                                      {entry.memo}
                                    </span>
                                  )}
                                </td>
                                <td className="py-2.5 px-2 text-center">
                                  <span className="inline-flex items-center text-[10px] font-semibold text-emerald-800 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200">
                                    <ArrowDownRight className="w-3 h-3 text-emerald-600 mr-0.5" />
                                    THU
                                  </span>
                                </td>
                                <td className="py-2.5 px-3 text-right font-bold text-emerald-700">
                                  +{formatVnd(entry.amount)}
                                </td>
                                <td className="py-2.5 px-3 text-right font-mono font-medium text-[#352D26]">
                                  {entry.runningBalance !== undefined && entry.runningBalance !== null
                                    ? formatVnd(entry.runningBalance)
                                    : '-'}
                                </td>
                                <td className="py-2.5 px-3 text-right text-[11px] text-[#786C60]">
                                  {entry.postedAt
                                    ? new Date(entry.postedAt).toLocaleString('vi-VN', {
                                        dateStyle: 'short',
                                        timeStyle: 'short',
                                      })
                                    : entry.businessDate}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="p-3 bg-amber-50/50 rounded-xl border border-amber-200/60 text-xs text-amber-800 flex items-start gap-2">
                  <Clock className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                  <div>
                    <p className="font-medium">Chưa ghi nhận vào sổ quỹ</p>
                    <p className="text-[11px] text-amber-700">
                      Bút toán tăng quỹ tiền mặt và tài khoản ngân hàng sẽ được hệ thống tự động ghi nhận ngay khi báo cáo được duyệt chốt (CONFIRMED).
                    </p>
                  </div>
                </div>
              )}
            </div>

            {/* Evidence attachment */}
            <div className="space-y-2">
              <h3 className="text-xs font-bold text-[#352D26] uppercase tracking-wider">
                Bằng chứng đính kèm (Z-report / Sao kê)
              </h3>
              {report.evidenceFileId ? (
                <div className="p-3 bg-white rounded-xl border border-emerald-200 flex items-center justify-between shadow-xs">
                  <div className="flex items-center space-x-2.5">
                    <div className="p-2 bg-emerald-50 text-emerald-700 rounded-lg">
                      <FileCheck2 className="w-4 h-4" />
                    </div>
                    <div>
                      <p className="text-xs font-medium text-[#352D26]">Chứng từ đối soát đính kèm</p>
                      <p className="text-[10px] text-[#786C60] font-mono">ID: {report.evidenceFileId}</p>
                    </div>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={isDownloading}
                    onClick={handleDownloadEvidence}
                    className="text-xs h-8 border-[#E5DED4] gap-1.5"
                  >
                    {isDownloading ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <ExternalLink className="w-3.5 h-3.5" />
                    )}
                    Mở chứng từ
                  </Button>
                </div>
              ) : (
                <div className="p-3 bg-zinc-50 rounded-xl border border-dashed border-zinc-300 text-xs text-zinc-500 text-center">
                  Không có file bằng chứng đính kèm cho báo cáo này.
                </div>
              )}
            </div>

            {/* Audit & Notes Information */}
            <div className="space-y-2 pt-2 border-t border-[#E5DED4]">
              <h3 className="text-xs font-bold text-[#352D26] uppercase tracking-wider">
                Lịch sử phê duyệt & Ghi chú (Audit Trail)
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs bg-[#F7F3EC] p-3 rounded-xl border border-[#E5DED4]">
                <div className="space-y-1">
                  <span className="text-[#786C60] block text-[11px]">Người lập / gửi duyệt:</span>
                  <p className="font-semibold text-[#352D26] flex items-center gap-1.5">
                    <UserCheck className="w-3.5 h-3.5 text-[#304638]" />
                    {report.submittedByName || 'Chưa xác định'}
                  </p>
                  {report.submittedAt && (
                    <span className="text-[10px] text-[#786C60] block">
                      Thời gian: {new Date(report.submittedAt).toLocaleString('vi-VN')}
                    </span>
                  )}
                </div>

                <div className="space-y-1">
                  <span className="text-[#786C60] block text-[11px]">Người duyệt chốt:</span>
                  <p className="font-semibold text-[#352D26] flex items-center gap-1.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-700" />
                    {report.confirmedByName || 'Chưa duyệt'}
                  </p>
                  {report.confirmedAt && (
                    <span className="text-[10px] text-[#786C60] block">
                      Thời gian: {new Date(report.confirmedAt).toLocaleString('vi-VN')}
                    </span>
                  )}
                </div>
              </div>

              {report.adjustmentReason && (
                <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs">
                  <span className="font-semibold text-amber-900 block mb-0.5">Lý do hiệu chỉnh số liệu:</span>
                  <p className="text-amber-800">{report.adjustmentReason}</p>
                </div>
              )}

              {report.note && (
                <div className="p-3 bg-white rounded-xl border border-[#E5DED4] text-xs">
                  <span className="font-semibold text-[#352D26] block mb-0.5">Ghi chú:</span>
                  <p className="text-[#786C60]">{report.note}</p>
                </div>
              )}
            </div>

            {/* Confirm Adjustment Form (for Owner/Manager) */}
            {canConfirm && (
              <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-xl space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-emerald-950 flex items-center gap-1.5">
                    <ShieldCheck className="w-4 h-4 text-emerald-700" /> Phê duyệt báo cáo doanh thu (Owner/Manager)
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowAdjustField(!showAdjustField)}
                    className="text-[11px] text-emerald-700 hover:underline font-medium"
                  >
                    {showAdjustField ? 'Ẩn ghi chú duyệt' : '+ Thêm ghi chú duyệt/hiệu chỉnh'}
                  </button>
                </div>

                {showAdjustField && (
                  <textarea
                    rows={2}
                    value={adjustmentReason}
                    onChange={(e) => setAdjustmentReason(e.target.value)}
                    placeholder="Nhập lý do hiệu chỉnh hoặc căn cứ đối soát (nếu có)..."
                    className="w-full text-xs p-2.5 rounded-lg border border-emerald-300 bg-white focus:outline-hidden focus:ring-2 focus:ring-emerald-600"
                  />
                )}
              </div>
            )}
          </div>

          {/* Footer Actions */}
          <div className="bg-[#F7F3EC] px-5 py-4 border-t border-[#E5DED4] flex items-center justify-between gap-3">
            <span className="text-[11px] text-[#786C60]">
              Mã ID: <code className="font-mono">{report.id}</code>
            </span>
            <div className="flex items-center space-x-2.5">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                className="border-[#E5DED4] text-xs h-9"
              >
                Đóng
              </Button>
              {canConfirm && (
                <Button
                  type="button"
                  disabled={isConfirming}
                  onClick={handleConfirm}
                  className="text-xs h-9 bg-emerald-700 hover:bg-emerald-800 text-white shadow-sm font-semibold"
                >
                  {isConfirming ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                  ) : (
                    <CheckCircle2 className="w-3.5 h-3.5 mr-1.5" />
                  )}
                  Xác nhận duyệt (Confirm)
                </Button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
