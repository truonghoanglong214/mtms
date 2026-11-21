import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Calendar,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  Clock,
  UserCheck,
  FileText,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { issueApi } from '../api/issueApi'
import type { StockDocumentDto, StockDocumentType, StockDocumentStatus } from '../types/issue.types'

interface IssueDetailModalProps {
  isOpen: boolean
  branchId: string
  documentId: string | null
  onClose: () => void
  onDocumentPosted?: (updated: StockDocumentDto) => void
}

function formatVnd(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 2,
  }).format(num)
}

function formatQuantity(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(num)
}

function formatDateTime(isoString?: string | null): string {
  if (!isoString) return '—'
  try {
    const d = new Date(isoString)
    if (isNaN(d.getTime())) return isoString
    return new Intl.DateTimeFormat('vi-VN', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(d)
  } catch {
    return isoString
  }
}

export const IssueDetailModal: React.FC<IssueDetailModalProps> = ({
  isOpen,
  branchId,
  documentId,
  onClose,
  onDocumentPosted,
}) => {
  const [document, setDocument] = useState<StockDocumentDto | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [isPosting, setIsPosting] = useState(false)
  const [postError, setPostError] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen && branchId && documentId) {
      setIsLoading(true)
      setError(null)
      setPostError(null)
      issueApi
        .getIssueDocumentById(branchId, documentId)
        .then((data) => {
          setDocument(data)
        })
        .catch((err: unknown) => {
          const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
          setError(
            apiErr.response?.data?.detail ||
              apiErr.response?.data?.message ||
              'Không thể tải chi tiết phiếu xuất kho.'
          )
        })
        .finally(() => {
          setIsLoading(false)
        })
    } else {
      setDocument(null)
    }
  }, [isOpen, branchId, documentId])

  const handlePostDocument = async () => {
    if (!document || !branchId || isPosting) return
    setIsPosting(true)
    setPostError(null)
    try {
      const updated = await issueApi.postIssueDocument(branchId, document.id)
      setDocument(updated)
      if (onDocumentPosted) {
        onDocumentPosted(updated)
      }
    } catch (err: unknown) {
      const apiErr = err as {
        response?: {
          data?: {
            title?: string
            detail?: string
            message?: string
            type?: string
          }
        }
      }
      const problemType = apiErr.response?.data?.type
      const detail = apiErr.response?.data?.detail
      const message = apiErr.response?.data?.message

      if (problemType === 'urn:mtms:error:insufficient-stock') {
        setPostError(
          detail ||
            'Tồn kho hiện tại không đủ để đáp ứng phiếu xuất này. Không thể xuất âm.'
        )
      } else {
        setPostError(detail || message || 'Không thể chốt ghi sổ phiếu xuất kho.')
      }
    } finally {
      setIsPosting(false)
    }
  }

  const renderStatusBadge = (status: StockDocumentStatus) => {
    switch (status) {
      case 'POSTED':
        return (
          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-semibold gap-1">
            <CheckCircle2 className="w-3 h-3" />
            Đã ghi sổ kho (POSTED)
          </Badge>
        )
      case 'DRAFT':
        return (
          <Badge className="bg-amber-100 text-amber-800 border-amber-300 font-semibold gap-1">
            <Clock className="w-3 h-3" />
            Bản nháp (DRAFT)
          </Badge>
        )
      case 'SUBMITTED':
        return (
          <Badge className="bg-blue-100 text-blue-800 border-blue-300 font-semibold gap-1">
            <UserCheck className="w-3 h-3" />
            Đã trình duyệt (SUBMITTED)
          </Badge>
        )
      case 'CANCELLED':
        return (
          <Badge className="bg-slate-100 text-slate-700 border-slate-300 font-semibold gap-1">
            <X className="w-3 h-3" />
            Đã hủy (CANCELLED)
          </Badge>
        )
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const renderTypeBadge = (docType: StockDocumentType) => {
    switch (docType) {
      case 'CONSUMPTION_ISSUE':
        return (
          <Badge variant="outline" className="bg-emerald-50 text-emerald-800 border-emerald-300">
            Xuất quầy bar pha chế
          </Badge>
        )
      case 'SCRAP_DISPOSAL':
        return (
          <Badge variant="outline" className="bg-rose-50 text-rose-800 border-rose-300">
            Tiêu hủy hao hụt
          </Badge>
        )
      case 'ADJUSTMENT':
        return (
          <Badge variant="outline" className="bg-slate-50 text-slate-800 border-slate-300">
            Điều chỉnh kiểm kê âm
          </Badge>
        )
      default:
        return <Badge variant="outline">{docType}</Badge>
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 10 }}
          transition={{ duration: 0.18 }}
          className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-4xl overflow-hidden my-auto flex flex-col max-h-[92vh]"
        >
          {/* Header */}
          <div className="bg-[#304638] text-white px-6 py-4 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-emerald-800/60 rounded-xl ring-1 ring-emerald-500/30">
                <FileText className="w-5 h-5 text-emerald-300" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                  <span>Phiếu xuất kho: {document?.documentNumber || '...'}</span>
                  {document && renderStatusBadge(document.status)}
                </h3>
                <p className="text-xs text-emerald-200/80">
                  {document?.reason || 'Chi tiết giao dịch xuất kho và định giá vốn Moving WAC'}
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-emerald-200 hover:text-white hover:bg-emerald-800/50 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body Content */}
          <div className="flex-1 overflow-y-auto p-6 space-y-5">
            {isLoading && (
              <div className="py-12 flex flex-col items-center justify-center text-slate-500 space-y-2">
                <Loader2 className="w-8 h-8 animate-spin text-emerald-700" />
                <span className="text-xs">Đang tải dữ liệu phiếu xuất kho...</span>
              </div>
            )}

            {error && (
              <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center gap-3 text-rose-800 text-xs">
                <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {postError && (
              <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center gap-3 text-rose-800 text-xs">
                <AlertTriangle className="w-5 h-5 text-rose-600 shrink-0" />
                <span>{postError}</span>
              </div>
            )}

            {document && !isLoading && (
              <>
                {/* Meta summary cards */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="bg-slate-50 border border-slate-200 rounded-xl p-3">
                    <span className="text-[11px] font-medium text-slate-500 block mb-1">
                      Ngày xuất kho
                    </span>
                    <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 font-mono">
                      <Calendar className="w-3.5 h-3.5 text-slate-400" />
                      {document.documentDate}
                    </span>
                  </div>

                  <div className="bg-slate-50 border border-slate-200 rounded-xl p-3">
                    <span className="text-[11px] font-medium text-slate-500 block mb-1">
                      Loại phiếu xuất
                    </span>
                    <div>{renderTypeBadge(document.documentType)}</div>
                  </div>

                  <div className="bg-slate-50 border border-slate-200 rounded-xl p-3">
                    <span className="text-[11px] font-medium text-slate-500 block mb-1">
                      Tổng lượng xuất
                    </span>
                    <span className="text-xs font-bold text-slate-800 font-mono">
                      {formatQuantity(document.totalQuantity)} (
                      {document.totalLines || document.lines?.length || 0} mặt hàng)
                    </span>
                  </div>

                  <div className="bg-emerald-50/70 border border-emerald-200 rounded-xl p-3">
                    <span className="text-[11px] font-medium text-emerald-800 block mb-1">
                      Tổng giá vốn xuất (WAC)
                    </span>
                    <span className="text-xs font-bold font-mono text-emerald-900">
                      {formatVnd(document.totalBookedCost)}
                    </span>
                  </div>
                </div>

                {/* Audit & Posting Details */}
                <div className="bg-slate-50/80 rounded-xl p-3 border border-slate-200 text-xs text-slate-600 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <UserCheck className="w-4 h-4 text-slate-400" />
                    <span>
                      Người lập: <strong>{document.createdByName || 'Hệ thống'}</strong>
                    </span>
                    {document.createdAt && (
                      <span className="text-slate-400">({formatDateTime(document.createdAt)})</span>
                    )}
                  </div>

                  {document.postedAt && (
                    <div className="flex items-center gap-2 text-emerald-800">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                      <span>
                        Chốt ghi sổ lúc: <strong>{formatDateTime(document.postedAt)}</strong>
                      </span>
                    </div>
                  )}
                </div>

                {/* Lines Table */}
                <div className="border border-slate-200 rounded-xl overflow-hidden shadow-xs">
                  <div className="bg-slate-100/90 px-4 py-2.5 border-b border-slate-200 flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                      Chi tiết mặt hàng & Snapshot Giá vốn Moving WAC
                    </span>
                    <Badge variant="outline" className="text-[10px] bg-slate-200 text-slate-700">
                      {document.lines?.length || 0} dòng hàng
                    </Badge>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                        <tr>
                          <th className="py-2.5 px-3 w-12 text-center">STT</th>
                          <th className="py-2.5 px-3">Mặt hàng</th>
                          <th className="py-2.5 px-3 w-20">ĐVT</th>
                          <th className="py-2.5 px-3 text-right">Lượng xuất</th>
                          <th className="py-2.5 px-3 text-right">Đơn giá WAC snapshot</th>
                          <th className="py-2.5 px-3 text-right">Thành tiền vốn (WAC)</th>
                          <th className="py-2.5 px-3">Ghi chú</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {document.lines && document.lines.length > 0 ? (
                          document.lines.map((line, idx) => (
                            <tr key={line.id || idx} className="hover:bg-slate-50/70">
                              <td className="py-2.5 px-3 text-center font-mono text-slate-400">
                                {line.lineNo || idx + 1}
                              </td>
                              <td className="py-2.5 px-3">
                                <div className="font-medium text-slate-900">
                                  {line.itemName || 'Mặt hàng'}
                                </div>
                                <div className="text-[11px] font-mono text-slate-500">
                                  {line.itemCode}
                                  {line.lotCode && ` • Lô: ${line.lotCode}`}
                                </div>
                              </td>
                              <td className="py-2.5 px-3 text-slate-600">
                                {line.unitName || line.unitCode || '—'}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-800">
                                {formatQuantity(line.issueQuantity)}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono text-emerald-800">
                                {formatVnd(line.bookedUnitCost)}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono font-bold text-emerald-900 bg-emerald-50/30">
                                {formatVnd(line.bookedTotal)}
                              </td>
                              <td className="py-2.5 px-3 text-slate-500 text-[11px]">
                                {line.notes || '—'}
                              </td>
                            </tr>
                          ))
                        ) : (
                          <tr>
                            <td colSpan={7} className="py-6 text-center text-slate-400">
                              Không có dữ liệu dòng hàng.
                            </td>
                          </tr>
                        )}
                      </tbody>
                      <tfoot className="bg-slate-50 font-bold border-t border-slate-200">
                        <tr>
                          <td colSpan={3} className="py-2.5 px-3 text-slate-700">
                            Tổng cộng
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono text-slate-900">
                            {formatQuantity(document.totalQuantity)}
                          </td>
                          <td></td>
                          <td className="py-2.5 px-3 text-right font-mono text-emerald-900 text-xs">
                            {formatVnd(document.totalBookedCost)}
                          </td>
                          <td></td>
                        </tr>
                      </tfoot>
                    </table>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Footer Actions */}
          <div className="bg-slate-50 px-6 py-4 border-t border-slate-200 flex items-center justify-between shrink-0">
            <div>
              {document?.status === 'DRAFT' && (
                <span className="text-xs text-amber-700 flex items-center gap-1.5 font-medium">
                  <Clock className="w-4 h-4" />
                  Phiếu này chưa trừ tồn kho và chưa chốt giá vốn.
                </span>
              )}
            </div>

            <div className="flex items-center gap-3">
              <Button type="button" variant="outline" onClick={onClose} className="text-xs">
                Đóng
              </Button>

              {document?.status === 'DRAFT' && (
                <Button
                  type="button"
                  onClick={handlePostDocument}
                  disabled={isPosting}
                  className="bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold gap-2 shadow-sm"
                >
                  {isPosting && <Loader2 className="h-4 w-4 animate-spin" />}
                  <span>Chốt ghi sổ phiếu này (Post Issue)</span>
                </Button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
