import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Building2,
  Calendar,
  CreditCard,
  FileText,
  Download,
  Loader2,
  Receipt,
  Clock,
  AlertCircle,
  Landmark,
  Trash2,
  Wallet,
  Ban,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd } from '../api/financeApi'
import { evidenceApi } from '@/features/evidence/evidenceApi'
import {
  TAX_OBLIGATION_TYPE_LABELS,
  TAX_OBLIGATION_STATUS_BADGES,
  type TaxObligationDetailDto,
  type TaxObligationDto,
} from '../types/finance.types'

interface TaxObligationDetailModalProps {
  isOpen: boolean
  branchId: string
  obligationId: string | null
  onClose: () => void
  onOpenPayModal: (obligation: TaxObligationDto) => void
  onObligationUpdated?: () => void
}

export const TaxObligationDetailModal: React.FC<TaxObligationDetailModalProps> = ({
  isOpen,
  branchId,
  obligationId,
  onClose,
  onOpenPayModal,
  onObligationUpdated,
}) => {
  const [detail, setDetail] = useState<TaxObligationDetailDto | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)
  const [isCancelling, setIsCancelling] = useState<boolean>(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState<boolean>(false)
  const [downloadingFileId, setDownloadingFileId] = useState<string | null>(null)

  const loadDetail = useCallback(async () => {
    if (!branchId || !obligationId) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await financeApi.getTaxObligationDetail(branchId, obligationId)
      setDetail(data)
    } catch (err: unknown) {
      console.error('Failed to load tax obligation detail:', err)
      setError('Không thể tải thông tin chi tiết nghĩa vụ thuế. Vui lòng thử lại sau.')
    } finally {
      setIsLoading(false)
    }
  }, [branchId, obligationId])

  useEffect(() => {
    if (isOpen && obligationId) {
      loadDetail()
      setShowCancelConfirm(false)
    } else {
      setDetail(null)
      setShowCancelConfirm(false)
    }
  }, [isOpen, obligationId, loadDetail])

  const handleDownloadEvidence = async (fileId: string) => {
    try {
      setDownloadingFileId(fileId)
      const tokenRes = await evidenceApi.getEvidenceDownloadToken(fileId)
      const url = evidenceApi.getEvidenceContentUrl(fileId, tokenRes.token)
      window.open(url, '_blank')
    } catch (err: unknown) {
      console.error('Failed to download evidence:', err)
      alert('Không thể tải chứng từ. Vui lòng thử lại sau.')
    } finally {
      setDownloadingFileId(null)
    }
  }

  const handleCancelObligation = async () => {
    if (!branchId || !obligationId) return
    setIsCancelling(true)
    try {
      await financeApi.cancelTaxObligation(branchId, obligationId)
      setShowCancelConfirm(false)
      if (onObligationUpdated) {
        onObligationUpdated()
      }
      onClose()
    } catch (err: unknown) {
      console.error('Failed to cancel tax obligation:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      alert(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể hủy nghĩa vụ thuế.'
      )
    } finally {
      setIsCancelling(false)
    }
  }

  if (!isOpen || !obligationId) return null

  const statusBadge = detail
    ? TAX_OBLIGATION_STATUS_BADGES[detail.status]
    : null

  const canCancel = detail && detail.amountPaid === 0 && detail.status === 'OPEN'
  const canPay = detail && detail.remainingLiability > 0 && detail.status !== 'CANCELLED'

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 15 }}
          transition={{ duration: 0.2, ease: 'easeOut' }}
          className="relative w-full max-w-3xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden my-auto"
        >
          {/* Header */}
          <div className="px-5 py-4 bg-slate-900 text-white flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-[#304638] text-white flex items-center justify-center shadow-xs">
                <Landmark className="w-5 h-5 text-emerald-400" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-bold text-white tracking-tight">
                    Chi Tiết Hồ Sơ Nghĩa Vụ Thuế
                  </h3>
                  {statusBadge && (
                    <Badge
                      variant="outline"
                      className={`${statusBadge.badgeClass} text-[10px] font-medium border`}
                    >
                      {statusBadge.label}
                    </Badge>
                  )}
                </div>
                <p className="text-xs text-slate-400">
                  {detail ? `Mã hồ sơ: ${detail.obligationCode}` : 'Đang tải hồ sơ...'}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="text-slate-400 hover:text-white p-1 rounded-lg transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body */}
          <div className="p-5 max-h-[80vh] overflow-y-auto space-y-5">
            {isLoading && (
              <div className="py-12 flex flex-col items-center justify-center text-slate-400 gap-2">
                <Loader2 className="w-7 h-7 animate-spin text-[#304638]" />
                <span className="text-xs">Đang tải chi tiết nghĩa vụ thuế...</span>
              </div>
            )}

            {error && (
              <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center gap-2.5">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {detail && !isLoading && (
              <>
                {/* Financial KPI Summary Cards */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200">
                    <span className="text-[11px] text-slate-500 font-medium block">Tổng phải nộp</span>
                    <span className="text-sm sm:text-base font-bold text-slate-900 font-mono">
                      {formatVnd(detail.amountDue)}
                    </span>
                  </div>
                  <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200">
                    <span className="text-[11px] text-emerald-700 font-medium block">Đã nộp vào NSNN</span>
                    <span className="text-sm sm:text-base font-bold text-emerald-800 font-mono">
                      {formatVnd(detail.amountPaid)}
                    </span>
                  </div>
                  <div className="p-3.5 rounded-xl bg-amber-50/70 border border-amber-200">
                    <span className="text-[11px] text-amber-800 font-medium block">Còn phải nộp</span>
                    <span
                      className={`text-sm sm:text-base font-bold font-mono ${
                        detail.remainingLiability > 0 ? 'text-rose-600' : 'text-slate-800'
                      }`}
                    >
                      {formatVnd(detail.remainingLiability)}
                    </span>
                  </div>
                  <div className="p-3.5 rounded-xl bg-blue-50/70 border border-blue-200">
                    <span className="text-[11px] text-blue-800 font-medium block">Đã phân bổ chi phí</span>
                    <span className="text-sm sm:text-base font-bold text-blue-900 font-mono">
                      {formatVnd(detail.amountAllocated)}
                    </span>
                  </div>
                </div>

                {/* Details Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-4 rounded-xl border border-slate-200 bg-slate-50/50 text-xs">
                  <div>
                    <span className="text-slate-500 block mb-0.5">Loại nghĩa vụ thuế:</span>
                    <span className="font-semibold text-slate-900">
                      {TAX_OBLIGATION_TYPE_LABELS[detail.obligationType]}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-0.5">Phạm vi áp dụng:</span>
                    <span className="font-medium text-slate-800 flex items-center gap-1.5">
                      <Building2 className="w-3.5 h-3.5 text-slate-400" />
                      {detail.branchName ? `Chi nhánh: ${detail.branchName}` : (detail.branchId ? 'Gắn riêng cho chi nhánh' : 'Toàn pháp nhân (Công ty)')}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-0.5">Kỳ tính thuế:</span>
                    <span className="font-medium text-slate-800 flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-slate-400" />
                      {detail.taxPeriodStart} → {detail.taxPeriodEnd}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-0.5">Hạn nộp ngân sách:</span>
                    <span className="font-medium text-slate-800 flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      {detail.dueDate || 'Chưa xác định'}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-0.5">Cơ quan quản lý thuế:</span>
                    <span className="font-medium text-slate-800">
                      {detail.taxAuthorityName || '—'}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-0.5">Người khai báo / Ngày lập:</span>
                    <span className="font-medium text-slate-800">
                      {detail.enteredByName || 'Kế toán viên'} ({new Date(detail.createdAt).toLocaleDateString('vi-VN')})
                    </span>
                  </div>

                  {detail.note && (
                    <div className="sm:col-span-2 pt-2 border-t border-slate-200">
                      <span className="text-slate-500 block mb-0.5">Ghi chú & Căn cứ:</span>
                      <p className="text-slate-800 italic">{detail.note}</p>
                    </div>
                  )}

                  {detail.evidenceFileId && (
                    <div className="sm:col-span-2 pt-2 border-t border-slate-200 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-emerald-600" />
                        <span className="font-medium text-slate-800">Hồ sơ / Tờ khai thuế đính kèm</span>
                      </div>
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => handleDownloadEvidence(detail.evidenceFileId!)}
                        disabled={downloadingFileId === detail.evidenceFileId}
                        className="text-xs flex items-center gap-1.5 h-8"
                      >
                        {downloadingFileId === detail.evidenceFileId ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        ) : (
                          <Download className="w-3.5 h-3.5" />
                        )}
                        <span>Tải xem tờ khai</span>
                      </Button>
                    </div>
                  )}
                </div>

                {/* Payment History Section */}
                <div className="space-y-3 pt-2">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                      <Receipt className="w-4 h-4 text-[#304638]" />
                      <span>Lịch Sử Nộp Thuế Vào NSNN ({detail.payments?.length || 0})</span>
                    </h4>
                    {canPay && (
                      <Button
                        type="button"
                        size="sm"
                        onClick={() => {
                          onClose()
                          onOpenPayModal(detail)
                        }}
                        className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold h-7 px-3 flex items-center gap-1"
                      >
                        <Receipt className="w-3 h-3" />
                        <span>Nộp thuế</span>
                      </Button>
                    )}
                  </div>

                  {(!detail.payments || detail.payments.length === 0) ? (
                    <div className="p-6 rounded-xl border border-dashed border-slate-200 text-center text-slate-400 text-xs">
                      Chưa có bút toán thanh toán nộp thuế nào được ghi nhận cho hồ sơ này.
                    </div>
                  ) : (
                    <div className="rounded-xl border border-slate-200 overflow-hidden shadow-xs">
                      <table className="w-full text-left text-xs">
                        <thead className="bg-slate-100/80 text-slate-600 uppercase font-semibold text-[10px] tracking-wider border-b border-slate-200">
                          <tr>
                            <th className="px-3.5 py-2.5">Ngày nộp</th>
                            <th className="px-3.5 py-2.5">Số tiền</th>
                            <th className="px-3.5 py-2.5">Phương thức / Tài khoản</th>
                            <th className="px-3.5 py-2.5">Số biên lai / UNC</th>
                            <th className="px-3.5 py-2.5 text-right">Chứng từ</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {detail.payments.map((p) => (
                            <tr key={p.id} className="hover:bg-slate-50/60 transition-colors">
                              <td className="px-3.5 py-2.5 font-mono text-slate-700 whitespace-nowrap">
                                {p.paymentDate}
                              </td>
                              <td className="px-3.5 py-2.5 font-bold font-mono text-emerald-800 whitespace-nowrap">
                                {formatVnd(p.amount)}
                              </td>
                              <td className="px-3.5 py-2.5 text-slate-700">
                                <div className="flex items-center gap-1.5">
                                  {p.paymentMethod === 'BANK_TRANSFER' ? (
                                    <CreditCard className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                                  ) : (
                                    <Wallet className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                                  )}
                                  <span className="truncate max-w-[150px]">
                                    {p.financialAccountName || (p.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản' : 'Tiền mặt')}
                                  </span>
                                </div>
                              </td>
                              <td className="px-3.5 py-2.5 font-mono text-slate-600">
                                {p.receiptNumber || '—'}
                              </td>
                              <td className="px-3.5 py-2.5 text-right whitespace-nowrap">
                                {p.evidenceFileId ? (
                                  <button
                                    type="button"
                                    onClick={() => handleDownloadEvidence(p.evidenceFileId!)}
                                    disabled={downloadingFileId === p.evidenceFileId}
                                    className="text-emerald-700 hover:text-emerald-900 font-medium inline-flex items-center gap-1 text-[11px] cursor-pointer"
                                  >
                                    {downloadingFileId === p.evidenceFileId ? (
                                      <Loader2 className="w-3 h-3 animate-spin" />
                                    ) : (
                                      <Download className="w-3 h-3" />
                                    )}
                                    <span>Tải UNC</span>
                                  </button>
                                ) : (
                                  <span className="text-slate-400 text-[11px]">Không có</span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                {/* Cancel Confirmation Dialog */}
                {showCancelConfirm && (
                  <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 space-y-3">
                    <div className="flex items-start gap-2.5 text-rose-800 text-xs">
                      <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-bold">Xác nhận hủy nghĩa vụ thuế:</span> Bạn có chắc chắn muốn hủy hồ sơ nghĩa vụ thuế này? Thao tác này sẽ chuyển trạng thái sang <span className="font-semibold">ĐÃ HỦY</span> và không thể khôi phục.
                      </div>
                    </div>
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => setShowCancelConfirm(false)}
                        disabled={isCancelling}
                        className="text-xs h-7"
                      >
                        Quay lại
                      </Button>
                      <Button
                        type="button"
                        size="sm"
                        onClick={handleCancelObligation}
                        disabled={isCancelling}
                        className="bg-rose-600 hover:bg-rose-700 text-white text-xs h-7 flex items-center gap-1"
                      >
                        {isCancelling ? (
                          <Loader2 className="w-3 h-3 animate-spin" />
                        ) : (
                          <Ban className="w-3 h-3" />
                        )}
                        <span>Xác nhận Hủy</span>
                      </Button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Footer Actions */}
          <div className="px-5 py-3.5 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
            <div>
              {canCancel && !showCancelConfirm && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setShowCancelConfirm(true)}
                  className="text-xs text-rose-600 border-rose-200 hover:bg-rose-50 hover:text-rose-700 flex items-center gap-1.5"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Hủy nghĩa vụ thuế</span>
                </Button>
              )}
            </div>

            <div className="flex items-center gap-2.5">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                className="text-xs"
              >
                Đóng
              </Button>
              {canPay && (
                <Button
                  type="button"
                  onClick={() => {
                    onClose()
                    onOpenPayModal(detail)
                  }}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold flex items-center gap-1.5"
                >
                  <Receipt className="w-3.5 h-3.5" />
                  <span>Nộp thuế</span>
                </Button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
