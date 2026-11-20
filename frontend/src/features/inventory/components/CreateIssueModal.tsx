import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Plus,
  Trash2,
  AlertCircle,
  Loader2,
  Calendar,
  AlertTriangle,
  Info,
  Package,
  Layers,
  Coins,
  ShieldAlert,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { issueApi } from '../api/issueApi'
import { openingApi } from '../api/openingApi'
import type {
  StockDocumentDto,
  StockDocumentType,
  CreateStockDocumentCommand,
  CreateStockDocumentLineCommand,
} from '../types/issue.types'
import type { CatalogItemDto } from '../types/catalog.types'
import type { StockBalanceDto } from '../types/opening.types'

interface CreateIssueModalProps {
  isOpen: boolean
  entityId: string
  branchId: string
  branchName?: string
  items: CatalogItemDto[]
  onClose: () => void
  onSuccess: (created: StockDocumentDto) => void
}

interface IssueLineRowState {
  id: string
  itemId: string
  issueQuantity: string
  lotId: string
  notes: string
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

export const CreateIssueModal: React.FC<CreateIssueModalProps> = ({
  isOpen,
  branchId,
  branchName,
  items,
  onClose,
  onSuccess,
}) => {
  const today = useMemo(() => new Date().toISOString().split('T')[0], [])

  // Header State
  const [documentDate, setDocumentDate] = useState(today)
  const [documentType, setDocumentType] = useState<StockDocumentType>('CONSUMPTION_ISSUE')
  const [reason, setReason] = useState('')
  const [autoPost, setAutoPost] = useState(true)

  // Dynamic lines state
  const [lines, setLines] = useState<IssueLineRowState[]>([])

  // Branch Balances (to inspect current on-hand & Moving WAC)
  const [balances, setBalances] = useState<StockBalanceDto[]>([])
  const [isLoadingBalances, setIsLoadingBalances] = useState(false)

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Fetch branch stock balances when modal opens
  const fetchBalances = useCallback(async () => {
    if (!branchId) return
    setIsLoadingBalances(true)
    try {
      const res = await openingApi.getStockBalances(branchId, { size: 500 })
      setBalances(res.items || [])
    } catch (err) {
      console.error('Failed to load branch stock balances for issue validation:', err)
    } finally {
      setIsLoadingBalances(false)
    }
  }, [branchId])

  useEffect(() => {
    if (isOpen && branchId) {
      setDocumentDate(today)
      setDocumentType('CONSUMPTION_ISSUE')
      setReason('')
      setAutoPost(true)
      setError(null)
      fetchBalances()
      // Initial empty row
      setLines([
        {
          id: `line-${Date.now()}-1`,
          itemId: '',
          issueQuantity: '',
          lotId: '',
          notes: '',
        },
      ])
    }
  }, [isOpen, branchId, today, fetchBalances])

  // Map of itemId -> StockBalanceDto
  const balanceMap = useMemo(() => {
    const map = new Map<string, StockBalanceDto>()
    for (const b of balances) {
      map.set(b.itemId, b)
    }
    return map
  }, [balances])

  // Map of itemId -> CatalogItemDto
  const catalogMap = useMemo(() => {
    const map = new Map<string, CatalogItemDto>()
    for (const it of items) {
      map.set(it.id, it)
    }
    return map
  }, [items])

  // Helper row operations
  const handleAddLine = () => {
    setLines((prev) => [
      ...prev,
      {
        id: `line-${Date.now()}-${prev.length + 1}`,
        itemId: '',
        issueQuantity: '',
        lotId: '',
        notes: '',
      },
    ])
  }

  const handleRemoveLine = (id: string) => {
    setLines((prev) => prev.filter((l) => l.id !== id))
  }

  const handleLineChange = (id: string, updates: Partial<IssueLineRowState>) => {
    setLines((prev) =>
      prev.map((line) => (line.id === id ? { ...line, ...updates } : line))
    )
  }

  // Calculate per-line calculations & validations
  const analyzedLines = useMemo(() => {
    return lines.map((line) => {
      const item = catalogMap.get(line.itemId)
      const balance = balanceMap.get(line.itemId)

      const availableStock = balance ? parseFloat(balance.quantity) || 0 : 0
      const currentWac = balance ? parseFloat(balance.avgUnitCost) || 0 : 0
      const unitName = balance?.unitName || item?.baseUnitName || 'Đơn vị'

      const enteredQty = parseFloat(line.issueQuantity) || 0
      const lineTotal = enteredQty > 0 ? enteredQty * currentWac : 0

      // TC-021 Validation: strictly no overdraft allowed!
      let validationError: string | null = null
      if (line.itemId) {
        if (!line.issueQuantity || line.issueQuantity.trim() === '') {
          validationError = 'Vui lòng nhập số lượng xuất'
        } else if (isNaN(enteredQty) || enteredQty <= 0) {
          validationError = 'Số lượng xuất phải lớn hơn 0'
        } else if (enteredQty > availableStock) {
          validationError = `Vượt quá tồn kho khả dụng (Tồn hiện tại: ${formatQuantity(availableStock)} ${unitName}, Yêu cầu: ${formatQuantity(enteredQty)} ${unitName}). Không thể xuất âm!`
        }
      }

      return {
        ...line,
        item,
        balance,
        availableStock,
        currentWac,
        unitName,
        enteredQty,
        lineTotal,
        validationError,
        isOverdraft: enteredQty > availableStock,
      }
    })
  }, [lines, catalogMap, balanceMap])

  // Summary calculations
  const totalIssueQuantity = useMemo(() => {
    return analyzedLines.reduce((acc, row) => acc + (row.enteredQty > 0 ? row.enteredQty : 0), 0)
  }, [analyzedLines])

  const totalBookedCost = useMemo(() => {
    return analyzedLines.reduce((acc, row) => acc + (row.lineTotal > 0 ? row.lineTotal : 0), 0)
  }, [analyzedLines])

  // Form validity check (TC-021 Invariant: Submit is strictly disabled if any line overdrafts)
  const hasErrors = useMemo(() => {
    if (analyzedLines.length === 0) return true
    for (const row of analyzedLines) {
      if (!row.itemId) return true
      if (row.validationError) return true
    }
    return false
  }, [analyzedLines])

  // Submit Handler
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (hasErrors || isSubmitting) return

    setIsSubmitting(true)
    setError(null)

    try {
      const commandLines: CreateStockDocumentLineCommand[] = analyzedLines.map((row) => ({
        itemId: row.itemId,
        issueQuantity: row.enteredQty.toFixed(3),
        lotId: row.lotId ? row.lotId : null,
        notes: row.notes.trim() || null,
      }))

      const payload: CreateStockDocumentCommand = {
        documentDate,
        documentType,
        reason: reason.trim() || undefined,
        autoPost,
        lines: commandLines,
      }

      const created = await issueApi.createIssueDocument(branchId, payload)
      onSuccess(created)
      onClose()
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
      const title = apiErr.response?.data?.title

      if (problemType === 'urn:mtms:error:insufficient-stock') {
        setError(
          detail ||
            'Tồn kho không đủ để đáp ứng yêu cầu xuất kho. Hệ thống đã từ chối giao dịch để ngăn tồn âm (TC-021).'
        )
      } else {
        setError(detail || message || title || 'Đã xảy ra lỗi khi tạo phiếu xuất kho.')
      }
    } finally {
      setIsSubmitting(false)
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
                <Package className="w-5 h-5 text-emerald-300" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                  <span>Lập phiếu xuất kho thủ công (Manual Stock Issue)</span>
                  <Badge variant="outline" className="bg-emerald-950/60 text-emerald-200 border-emerald-700/50 text-[10px]">
                    TC-020 / TC-021 / TC-022
                  </Badge>
                </h3>
                <p className="text-xs text-emerald-200/80">
                  {branchName ? `Chi nhánh: ${branchName}` : 'Quản lý xuất dùng quầy bar pha chế & tiêu hủy hao hụt'}
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-emerald-200 hover:text-white hover:bg-emerald-800/50 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form Content */}
          <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden p-6 gap-5">
            {/* Explanatory Invariant Notice (TC-020 & TC-022) */}
            <div className="bg-emerald-50/70 border border-emerald-200 rounded-xl p-3.5 flex items-start gap-3 text-xs text-emerald-900 shrink-0">
              <Info className="w-4 h-4 text-emerald-700 shrink-0 mt-0.5" />
              <div className="space-y-0.5">
                <div className="font-semibold text-emerald-950">
                  Quy tắc giá vốn & An toàn tồn kho:
                </div>
                <p className="text-emerald-800 leading-relaxed">
                  Đơn giá xuất kho được chốt tự động theo <strong>Giá vốn bình quân di động (Moving WAC)</strong> hiện hành tại chi nhánh (TC-020).
                  Hệ thống khóa chặt và <strong>từ chối tức thì nếu số lượng xuất vượt quá tồn kho khả dụng</strong> để đảm bảo không bao giờ tồn kho âm (TC-021, TC-022).
                </p>
              </div>
            </div>

            {/* Error Alert */}
            {error && (
              <div className="bg-rose-50 border border-rose-200 rounded-xl p-3.5 flex items-start gap-3 text-xs text-rose-800 shrink-0 animate-in fade-in">
                <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <div className="space-y-0.5">
                  <span className="font-semibold text-rose-900">Không thể thực hiện xuất kho:</span>
                  <p className="text-rose-700">{error}</p>
                </div>
              </div>
            )}

            {/* Document Header Controls */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 shrink-0 bg-slate-50/80 p-4 rounded-xl border border-slate-200/80">
              {/* Document Date */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Ngày xuất kho</span>
                  <span className="text-rose-500">*</span>
                </label>
                <Input
                  type="date"
                  value={documentDate}
                  onChange={(e) => setDocumentDate(e.target.value)}
                  required
                  className="bg-white text-xs h-9"
                />
              </div>

              {/* Document Type */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                  <Layers className="w-3.5 h-3.5 text-slate-500" />
                  <span>Loại phiếu xuất</span>
                  <span className="text-rose-500">*</span>
                </label>
                <select
                  value={documentType}
                  onChange={(e) => setDocumentType(e.target.value as StockDocumentType)}
                  className="w-full text-xs h-9 px-3 rounded-lg border border-slate-300 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-600"
                >
                  <option value="CONSUMPTION_ISSUE">Xuất quầy bar pha chế (Consumption Issue)</option>
                  <option value="SCRAP_DISPOSAL">Tiêu hủy hao hụt / hết hạn (Scrap Disposal)</option>
                  <option value="ADJUSTMENT">Điều chỉnh kiểm kê âm (Negative Adjustment)</option>
                </select>
              </div>

              {/* Reason */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Lý do / Mục đích xuất
                </label>
                <Input
                  type="text"
                  placeholder="VD: Xuất quầy bar ca sáng, Trừ hao hụt cuối ca..."
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="bg-white text-xs h-9"
                />
              </div>
            </div>

            {/* Line Items Table */}
            <div className="flex-1 flex flex-col min-h-0 border border-slate-200 rounded-xl overflow-hidden shadow-xs bg-white">
              <div className="bg-slate-100/80 px-4 py-2.5 border-b border-slate-200 flex items-center justify-between shrink-0">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Danh sách mặt hàng xuất kho
                  </span>
                  <Badge variant="outline" className="text-[10px] bg-slate-200 text-slate-700 border-slate-300">
                    {lines.length} dòng
                  </Badge>
                  {isLoadingBalances && (
                    <span className="text-[11px] text-slate-500 flex items-center gap-1">
                      <Loader2 className="w-3 h-3 animate-spin" /> Đang cập nhật tồn khả dụng & WAC...
                    </span>
                  )}
                </div>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={handleAddLine}
                  className="h-7 text-xs gap-1 border-slate-300 text-slate-700 hover:text-emerald-800 hover:border-emerald-300 hover:bg-emerald-50"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Thêm dòng hàng</span>
                </Button>
              </div>

              {/* Scrollable table body */}
              <div className="overflow-y-auto flex-1 p-3 space-y-3">
                {analyzedLines.length === 0 ? (
                  <div className="text-center py-8 text-slate-400 text-xs">
                    Chưa có mặt hàng nào. Nhấn &quot;Thêm dòng hàng&quot; để bắt đầu.
                  </div>
                ) : (
                  analyzedLines.map((line, index) => (
                    <div
                      key={line.id}
                      className={`p-3 rounded-xl border transition-all ${
                        line.isOverdraft
                          ? 'border-rose-300 bg-rose-50/40 shadow-xs'
                          : 'border-slate-200/90 bg-white hover:border-slate-300'
                      }`}
                    >
                      <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-start">
                        {/* Line number */}
                        <div className="md:col-span-1 flex items-center gap-1.5 pt-1.5">
                          <span className="w-6 h-6 rounded-full bg-slate-100 text-slate-600 font-mono text-[11px] font-bold flex items-center justify-center">
                            {index + 1}
                          </span>
                        </div>

                        {/* Item selection */}
                        <div className="md:col-span-4">
                          <label className="block text-[11px] font-medium text-slate-600 mb-1">
                            Mặt hàng nguyên liệu <span className="text-rose-500">*</span>
                          </label>
                          <select
                            value={line.itemId}
                            onChange={(e) => handleLineChange(line.id, { itemId: e.target.value })}
                            required
                            className="w-full text-xs h-9 px-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-600"
                          >
                            <option value="">-- Chọn mặt hàng xuất --</option>
                            {items.map((it) => {
                              const bal = balanceMap.get(it.id)
                              const onHand = bal ? parseFloat(bal.quantity) || 0 : 0
                              return (
                                <option key={it.id} value={it.id}>
                                  {it.itemCode} - {it.name} ({it.baseUnitName}) [Tồn: {formatQuantity(onHand)}]
                                </option>
                              )
                            })}
                          </select>

                          {/* Live Balance & WAC Display */}
                          {line.itemId && (
                            <div className="mt-1.5 flex flex-wrap items-center gap-2 text-[11px]">
                              <span className="inline-flex items-center gap-1 font-medium text-slate-600">
                                <Package className="w-3 h-3 text-slate-400" />
                                Tồn khả dụng:{' '}
                                <strong
                                  className={
                                    line.availableStock <= 0
                                      ? 'text-rose-600 font-mono'
                                      : 'text-slate-800 font-mono'
                                  }
                                >
                                  {formatQuantity(line.availableStock)} {line.unitName}
                                </strong>
                              </span>
                              <span className="text-slate-300">•</span>
                              <span className="inline-flex items-center gap-1 font-medium text-slate-600">
                                <Coins className="w-3 h-3 text-amber-500" />
                                WAC hiện hành:{' '}
                                <strong className="text-emerald-800 font-mono">
                                  {formatVnd(line.currentWac)}/{line.unitName}
                                </strong>
                              </span>
                            </div>
                          )}
                        </div>

                        {/* Issue Quantity */}
                        <div className="md:col-span-3">
                          <label className="block text-[11px] font-medium text-slate-600 mb-1">
                            Số lượng xuất ({line.unitName}) <span className="text-rose-500">*</span>
                          </label>
                          <Input
                            type="number"
                            step="0.001"
                            min="0.001"
                            placeholder="VD: 5.000"
                            value={line.issueQuantity}
                            onChange={(e) => handleLineChange(line.id, { issueQuantity: e.target.value })}
                            required
                            className={`h-9 text-xs font-mono font-medium ${
                              line.isOverdraft
                                ? 'border-rose-400 bg-rose-50 text-rose-900 focus-visible:ring-rose-500'
                                : 'bg-white'
                            }`}
                          />
                        </div>

                        {/* Line Total (WAC valuation) */}
                        <div className="md:col-span-3">
                          <label className="block text-[11px] font-medium text-slate-600 mb-1">
                            Thành tiền giá vốn (WAC)
                          </label>
                          <div className="h-9 px-3 rounded-lg border border-slate-200 bg-slate-50 flex items-center justify-between font-mono text-xs text-emerald-900 font-bold">
                            <span>{formatVnd(line.lineTotal)}</span>
                            <span className="text-[10px] text-slate-400 font-normal">
                              ({formatQuantity(line.enteredQty)} × {formatVnd(line.currentWac)})
                            </span>
                          </div>
                        </div>

                        {/* Remove action */}
                        <div className="md:col-span-1 flex justify-end pt-5">
                          <Button
                            type="button"
                            size="icon"
                            variant="ghost"
                            onClick={() => handleRemoveLine(line.id)}
                            disabled={lines.length <= 1}
                            className="h-8 w-8 text-slate-400 hover:text-rose-600 hover:bg-rose-50"
                            title="Xóa dòng này"
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </div>
                      </div>

                      {/* Notes / Lot code input */}
                      <div className="mt-2 grid grid-cols-1 md:grid-cols-12 gap-3">
                        <div className="md:col-span-1"></div>
                        <div className="md:col-span-10">
                          <Input
                            type="text"
                            placeholder="Ghi chú chi tiết dòng hàng (tùy chọn)..."
                            value={line.notes}
                            onChange={(e) => handleLineChange(line.id, { notes: e.target.value })}
                            className="h-7 text-xs bg-slate-50/60 border-slate-200"
                          />
                        </div>
                      </div>

                      {/* TC-021 Real-time Overdraft Validation Banner */}
                      {line.validationError && (
                        <div className="mt-2.5 flex items-center gap-2 p-2 bg-rose-100 border border-rose-300 rounded-lg text-rose-800 text-xs font-semibold animate-in fade-in">
                          <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                          <span>{line.validationError}</span>
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>

              {/* Table Footer Summary */}
              <div className="bg-slate-50 border-t border-slate-200 px-4 py-3 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs shrink-0">
                <div className="flex items-center gap-4 text-slate-600">
                  <span>
                    Tổng số mặt hàng: <strong>{lines.length}</strong>
                  </span>
                  <span>•</span>
                  <span>
                    Tổng lượng xuất: <strong>{formatQuantity(totalIssueQuantity)}</strong>
                  </span>
                  <span>•</span>
                  <label className="flex items-center gap-2 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={autoPost}
                      onChange={(e) => setAutoPost(e.target.checked)}
                      className="rounded border-slate-300 text-emerald-700 focus:ring-emerald-700"
                    />
                    <span className="font-semibold text-slate-800">
                      Chốt ghi sổ & trừ tồn ngay (Auto-post)
                    </span>
                  </label>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-slate-500 font-medium">Tổng giá vốn xuất kho:</span>
                  <span className="text-sm font-bold font-mono text-emerald-900 bg-emerald-100/80 px-3 py-1 rounded-lg">
                    {formatVnd(totalBookedCost)}
                  </span>
                </div>
              </div>
            </div>

            {/* Bottom Actions */}
            <div className="flex items-center justify-between pt-2 border-t border-slate-200 shrink-0">
              <div className="text-xs text-slate-500">
                {hasErrors && (
                  <span className="text-rose-600 font-medium flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" />
                    Vui lòng khắc phục các lỗi tồn kho / số lượng trước khi tiếp tục
                  </span>
                )}
              </div>

              <div className="flex items-center gap-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={onClose}
                  disabled={isSubmitting}
                  className="text-xs"
                >
                  Hủy bỏ
                </Button>

                <Button
                  type="submit"
                  disabled={hasErrors || isSubmitting}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-sm px-5"
                >
                  {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                  <span>
                    {autoPost ? 'Lưu & Chốt ghi sổ xuất kho' : 'Lưu bản nháp (Draft)'}
                  </span>
                </Button>
              </div>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
