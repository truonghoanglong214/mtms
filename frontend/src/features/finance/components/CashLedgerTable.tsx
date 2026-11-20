import React, { useState } from 'react'
import {
  ArrowDownLeft,
  ArrowUpRight,
  FileText,
  Download,
  Loader2,
  Wallet,
  Landmark,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
} from 'lucide-react'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import {
  formatVnd,
} from '../api/financeApi'
import { evidenceApi } from '@/features/evidence/evidenceApi'
import {
  SOURCE_TYPE_LABELS,
  type CashLedgerEntryDto,
} from '../types/finance.types'

interface CashLedgerTableProps {
  entries: CashLedgerEntryDto[]
  isLoading?: boolean
  currentPage?: number
  totalPages?: number
  totalItems?: number
  pageSize?: number
  onPageChange?: (page: number) => void
  onPageSizeChange?: (size: number) => void
}

export const CashLedgerTable: React.FC<CashLedgerTableProps> = ({
  entries,
  isLoading = false,
  currentPage = 0,
  totalPages = 1,
  totalItems = 0,
  pageSize = 15,
  onPageChange,
  onPageSizeChange,
}) => {
  const [downloadingFileId, setDownloadingFileId] = useState<string | null>(null)
  const [evidenceError, setEvidenceError] = useState<string | null>(null)

  const handleDownloadEvidence = async (fileId: string) => {
    try {
      setDownloadingFileId(fileId)
      setEvidenceError(null)
      const tokenRes = await evidenceApi.getEvidenceDownloadToken(fileId)
      const downloadUrl = evidenceApi.getEvidenceContentUrl(fileId, tokenRes.token)
      window.open(downloadUrl, '_blank', 'noopener,noreferrer')
    } catch (err: unknown) {
      console.error('Failed to get evidence token:', err)
      setEvidenceError('Không thể tải chứng từ. Vui lòng thử lại.')
      setTimeout(() => setEvidenceError(null), 4000)
    } finally {
      setDownloadingFileId(null)
    }
  }

  return (
    <div className="space-y-3">
      {evidenceError && (
        <div className="p-3 text-xs bg-rose-50 text-rose-700 border border-rose-200 rounded-xl flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0 text-rose-600" />
          <span>{evidenceError}</span>
        </div>
      )}

      {/* Table Container */}
      <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50/80 border-b border-slate-200 text-xs font-semibold text-slate-600 uppercase tracking-wider">
              <tr>
                <th scope="col" className="px-4 py-3.5 whitespace-nowrap">
                  Ngày kinh doanh
                </th>
                <th scope="col" className="px-4 py-3.5 whitespace-nowrap">
                  Tài khoản
                </th>
                <th scope="col" className="px-4 py-3.5 whitespace-nowrap">
                  Hướng dòng tiền
                </th>
                <th scope="col" className="px-4 py-3.5 whitespace-nowrap">
                  Danh mục thu/chi
                </th>
                <th scope="col" className="px-4 py-3.5">
                  Nghiệp vụ & Diễn giải
                </th>
                <th scope="col" className="px-4 py-3.5 text-right whitespace-nowrap">
                  Số tiền phát sinh
                </th>
                <th scope="col" className="px-4 py-3.5 text-right whitespace-nowrap">
                  Số dư sau GD
                </th>
                <th scope="col" className="px-4 py-3.5 text-center whitespace-nowrap">
                  Chứng từ
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-16 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <Loader2 className="h-6 w-6 animate-spin text-[#B96849]" />
                      <span className="text-xs">Đang tải dữ liệu sổ nhật ký thu chi...</span>
                    </div>
                  </td>
                </tr>
              ) : entries.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-16 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center gap-2 max-w-sm mx-auto">
                      <div className="h-12 w-12 rounded-2xl bg-slate-100 flex items-center justify-center text-slate-400">
                        <FileText className="h-6 w-6" />
                      </div>
                      <p className="font-semibold text-slate-700 text-sm">
                        Chưa có bút toán thu chi nào
                      </p>
                      <p className="text-xs text-slate-500 text-center leading-relaxed">
                        Không tìm thấy giao dịch nào phù hợp với bộ lọc hiện tại. Bút toán sẽ tự động phát sinh khi xác nhận doanh thu, thanh toán nợ NCC, hoặc chuyển quỹ nội bộ.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                entries.map((entry) => {
                  const isInflow = entry.entryType === 'INFLOW'
                  const isReversed = entry.status === 'REVERSED'
                  const numericSigned = typeof entry.signedAmount === 'string'
                    ? parseFloat(entry.signedAmount) || 0
                    : entry.signedAmount || 0
                  const numericRunning = typeof entry.runningBalance === 'string'
                    ? parseFloat(entry.runningBalance) || 0
                    : entry.runningBalance || 0

                  return (
                    <tr
                      key={entry.id}
                      className={`hover:bg-slate-50/70 transition-colors ${
                        isReversed ? 'bg-slate-50/50 opacity-70' : ''
                      }`}
                    >
                      {/* Ngày kinh doanh & giờ ghi sổ */}
                      <td className="px-4 py-3 whitespace-nowrap font-mono text-xs text-slate-700">
                        <div className="font-semibold text-slate-900">
                          {entry.businessDate}
                        </div>
                        {entry.postedAt && (
                          <div className="text-[10px] text-slate-400">
                            {new Date(entry.postedAt).toLocaleTimeString('vi-VN', {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </div>
                        )}
                      </td>

                      {/* Tài khoản */}
                      <td className="px-4 py-3 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          {entry.accountKind === 'BANK' ? (
                            <Landmark className="w-3.5 h-3.5 text-sky-600 shrink-0" />
                          ) : (
                            <Wallet className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                          )}
                          <span className="font-medium text-xs text-slate-800">
                            {entry.accountName || (entry.accountKind === 'BANK' ? 'Ngân hàng' : 'Tiền mặt két')}
                          </span>
                        </div>
                      </td>

                      {/* Hướng dòng tiền (Pill Thu / Chi) */}
                      <td className="px-4 py-3 whitespace-nowrap">
                        {isInflow ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <ArrowDownLeft className="w-3 h-3 text-emerald-600" />
                            <span>Thu tiền vào</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
                            <ArrowUpRight className="w-3 h-3 text-rose-600" />
                            <span>Chi tiền ra</span>
                          </span>
                        )}
                      </td>

                      {/* Danh mục thu/chi */}
                      <td className="px-4 py-3 whitespace-nowrap">
                        <div>
                          <div className="font-medium text-xs text-slate-900">
                            {entry.categoryName || entry.categoryId}
                          </div>
                          {entry.categoryCode && (
                            <div className="text-[10px] font-mono text-slate-400">
                              {entry.categoryCode}
                            </div>
                          )}
                        </div>
                      </td>

                      {/* Nghiệp vụ nguồn & Diễn giải */}
                      <td className="px-4 py-3">
                        <div className="space-y-0.5 max-w-xs md:max-w-md">
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <Badge
                              variant="secondary"
                              className="text-[10px] font-normal py-0 px-1.5 bg-slate-100 text-slate-600"
                            >
                              {SOURCE_TYPE_LABELS[entry.sourceType] || entry.sourceType}
                            </Badge>

                            {entry.sourceLeg && entry.sourceLeg !== 'MAIN' && (
                              <Badge
                                variant="outline"
                                className="text-[9px] font-mono py-0 px-1 text-slate-500 border-slate-200"
                              >
                                {entry.sourceLeg === 'SOURCE'
                                  ? 'Bên chuyển'
                                  : entry.sourceLeg === 'DESTINATION'
                                    ? 'Bên nhận'
                                    : entry.sourceLeg}
                              </Badge>
                            )}

                            {isReversed && (
                              <Badge
                                variant="destructive"
                                className="text-[10px] py-0 px-1.5 flex items-center gap-1"
                              >
                                <RotateCcw className="w-2.5 h-2.5" />
                                Đã đảo bút toán
                              </Badge>
                            )}
                          </div>

                          {entry.memo && (
                            <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                              {entry.memo}
                            </p>
                          )}
                        </div>
                      </td>

                      {/* Số tiền phát sinh */}
                      <td className="px-4 py-3 text-right whitespace-nowrap">
                        <div
                          className={`font-mono text-xs font-bold ${
                            isInflow
                              ? 'text-emerald-700'
                              : 'text-rose-700'
                          }`}
                        >
                          {isInflow ? '+' : '-'}
                          {formatVnd(Math.abs(numericSigned))}
                        </div>
                      </td>

                      {/* Số dư tức thời sau giao dịch */}
                      <td className="px-4 py-3 text-right whitespace-nowrap">
                        <div className="font-mono text-xs font-semibold text-slate-900 bg-slate-50 px-2 py-0.5 rounded-md inline-block">
                          {formatVnd(numericRunning)}
                        </div>
                      </td>

                      {/* Chứng từ đính kèm */}
                      <td className="px-4 py-3 text-center whitespace-nowrap">
                        {entry.evidenceFileId ? (
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            disabled={downloadingFileId === entry.evidenceFileId}
                            onClick={() => handleDownloadEvidence(entry.evidenceFileId!)}
                            className="h-7 px-2 text-xs text-[#304638] hover:text-[#25372C] hover:bg-emerald-50 rounded-lg inline-flex items-center gap-1 cursor-pointer"
                            title="Tải / Xem chứng từ gốc"
                          >
                            {downloadingFileId === entry.evidenceFileId ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin text-[#B96849]" />
                            ) : (
                              <Download className="w-3.5 h-3.5 text-[#304638]" />
                            )}
                            <span className="text-[11px] font-medium">Biên lai</span>
                          </Button>
                        ) : (
                          <span className="text-slate-300 text-xs">—</span>
                        )}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 border-t border-slate-200 bg-slate-50/60 text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <span>
              Tổng số: <strong className="text-slate-700 font-mono">{totalItems}</strong> bút toán
            </span>
            {onPageSizeChange && (
              <div className="flex items-center gap-1 pl-3 border-l border-slate-200">
                <span>Hiển thị:</span>
                <select
                  value={pageSize}
                  onChange={(e) => onPageSizeChange(Number(e.target.value))}
                  className="rounded-md border border-slate-200 bg-white px-2 py-0.5 text-xs text-slate-700 font-mono focus:outline-none focus:ring-1 focus:ring-[#304638]"
                >
                  <option value={10}>10</option>
                  <option value={15}>15</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                </select>
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            <span className="font-mono text-slate-600">
              Trang {currentPage + 1} / {Math.max(1, totalPages)}
            </span>
            <div className="flex items-center gap-1">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={currentPage <= 0 || isLoading}
                onClick={() => onPageChange && onPageChange(currentPage - 1)}
                className="h-7 w-7 p-0 cursor-pointer"
                title="Trang trước"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={currentPage >= totalPages - 1 || isLoading}
                onClick={() => onPageChange && onPageChange(currentPage + 1)}
                className="h-7 w-7 p-0 cursor-pointer"
                title="Trang sau"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
