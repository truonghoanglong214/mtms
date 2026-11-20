import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Plus,
  Trash2,
  AlertCircle,
  Loader2,
  Calendar,
  Receipt,
  AlertTriangle,
  Info,
  CheckCircle2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { receiptApi } from '../api/receiptApi'
import { catalogApi } from '../api/catalogApi'
import type {
  PurchaseReceiptDto,
  PaymentStatus,
  CreateReceiptCommand,
  CreateReceiptLineCommand,
} from '../types/receipt.types'
import type {
  CatalogItemDto,
  SupplierDto,
  SupplierPackConversionDto,
} from '../types/catalog.types'

interface CreateReceiptModalProps {
  isOpen: boolean
  entityId: string
  branchId: string
  branchName?: string
  suppliers: SupplierDto[]
  items: CatalogItemDto[]
  onClose: () => void
  onSuccess: (created: PurchaseReceiptDto) => void
}

interface LineRowState {
  id: string
  itemId: string
  selectedPackConversionId: string // '' for base unit
  packName: string | null
  declaredPackQuantity: string
  conversionSnapshot: string
  receivedBaseQuantity: string
  baseUnitCost: string
  lineTotal: string
  lotCode: string
  expiresOn: string
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

function getDefaultDueDate(receivedDateStr: string): string {
  try {
    const d = new Date(receivedDateStr)
    if (isNaN(d.getTime())) return ''
    d.setDate(d.getDate() + 30)
    return d.toISOString().split('T')[0]
  } catch {
    return ''
  }
}

export const CreateReceiptModal: React.FC<CreateReceiptModalProps> = ({
  isOpen,
  entityId,
  branchId,
  branchName,
  suppliers,
  items,
  onClose,
  onSuccess,
}) => {
  const today = useMemo(() => new Date().toISOString().split('T')[0], [])

  // Header State
  const [supplierId, setSupplierId] = useState('')
  const [receivedOn, setReceivedOn] = useState(today)
  const [invoiceNumber, setInvoiceNumber] = useState('')
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatus>('UNPAID')
  const [dueDate, setDueDate] = useState(() => getDefaultDueDate(today))
  const [notes, setNotes] = useState('')
  const [autoPost, setAutoPost] = useState(true)

  // Dynamic lines state
  const [lines, setLines] = useState<LineRowState[]>([])
  const [packConversionsCache, setPackConversionsCache] = useState<
    Record<string, SupplierPackConversionDto[]>
  >({})

  // Form submission state
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  // Reset or initialize on open
  useEffect(() => {
    if (isOpen) {
      setSupplierId(suppliers.length > 0 ? suppliers[0].id : '')
      setReceivedOn(today)
      setInvoiceNumber('')
      setPaymentStatus('UNPAID')
      setDueDate(getDefaultDueDate(today))
      setNotes('')
      setAutoPost(true)
      setFormError(null)

      // Start with 1 default empty row
      setLines([
        {
          id: Math.random().toString(36).substring(2, 9),
          itemId: '',
          selectedPackConversionId: '',
          packName: null,
          declaredPackQuantity: '',
          conversionSnapshot: '1.000',
          receivedBaseQuantity: '',
          baseUnitCost: '',
          lineTotal: '0.00',
          lotCode: '',
          expiresOn: '',
        },
      ])
    }
  }, [isOpen, suppliers, today])

  // Update due date when receivedOn changes and paymentStatus is UNPAID
  const handleReceivedDateChange = (newDate: string) => {
    setReceivedOn(newDate)
    if (paymentStatus === 'UNPAID') {
      setDueDate(getDefaultDueDate(newDate))
    }
  }

  // Fetch pack conversions for item if not cached
  const ensurePackConversions = useCallback(
    async (itemId: string) => {
      if (!itemId || !entityId || packConversionsCache[itemId]) return
      try {
        const data = await catalogApi.getPackConversions(entityId, itemId)
        setPackConversionsCache((prev) => ({ ...prev, [itemId]: data }))
      } catch (err) {
        console.error('Failed to load pack conversions for item:', itemId, err)
      }
    },
    [entityId, packConversionsCache]
  )

  // Row manipulation handlers
  const handleAddLine = () => {
    setLines((prev) => [
      ...prev,
      {
        id: Math.random().toString(36).substring(2, 9),
        itemId: '',
        selectedPackConversionId: '',
        packName: null,
        declaredPackQuantity: '',
        conversionSnapshot: '1.000',
        receivedBaseQuantity: '',
        baseUnitCost: '',
        lineTotal: '0.00',
        lotCode: '',
        expiresOn: '',
      },
    ])
  }

  const handleRemoveLine = (index: number) => {
    setLines((prev) => prev.filter((_, idx) => idx !== index))
  }

  const handleItemSelect = (index: number, selectedItemId: string) => {
    ensurePackConversions(selectedItemId)
    setLines((prev) => {
      const next = [...prev]
      next[index] = {
        ...next[index],
        itemId: selectedItemId,
        selectedPackConversionId: '',
        packName: null,
        declaredPackQuantity: '',
        conversionSnapshot: '1.000',
        receivedBaseQuantity: '',
        baseUnitCost: '',
        lineTotal: '0.00',
        lotCode: '',
        expiresOn: '',
      }
      return next
    })
  }

  const handlePackConversionSelect = (
    index: number,
    conversionId: string,
    itemConversions: SupplierPackConversionDto[]
  ) => {
    setLines((prev) => {
      const next = [...prev]
      const row = next[index]
      if (!conversionId) {
        // Base unit directly
        const baseQty = row.declaredPackQuantity || row.receivedBaseQuantity || ''
        const unitCostNum = parseFloat(row.baseUnitCost) || 0
        const baseQtyNum = parseFloat(baseQty) || 0
        const total = (baseQtyNum * unitCostNum).toFixed(2)

        next[index] = {
          ...row,
          selectedPackConversionId: '',
          packName: null,
          conversionSnapshot: '1.000',
          receivedBaseQuantity: baseQty,
          lineTotal: isNaN(parseFloat(total)) ? '0.00' : total,
        }
      } else {
        const conv = itemConversions.find((c) => c.id === conversionId)
        if (conv) {
          const ratio = parseFloat(conv.baseUnitsPerPack) || 1
          const packQtyNum = parseFloat(row.declaredPackQuantity) || 0
          const computedBase = (packQtyNum * ratio).toFixed(3)
          const unitCostNum = parseFloat(row.baseUnitCost) || 0
          const total = (parseFloat(computedBase) * unitCostNum).toFixed(2)

          next[index] = {
            ...row,
            selectedPackConversionId: conv.id,
            packName: conv.packName,
            conversionSnapshot: conv.baseUnitsPerPack,
            receivedBaseQuantity: packQtyNum > 0 ? computedBase : '',
            lineTotal: isNaN(parseFloat(total)) ? '0.00' : total,
          }
        }
      }
      return next
    })
  }

  const handlePackQuantityChange = (index: number, packQtyStr: string) => {
    setLines((prev) => {
      const next = [...prev]
      const row = next[index]
      const ratio = parseFloat(row.conversionSnapshot) || 1
      const packQtyNum = parseFloat(packQtyStr) || 0
      const computedBase = (packQtyNum * ratio).toFixed(3)
      const unitCostNum = parseFloat(row.baseUnitCost) || 0
      const total = (parseFloat(computedBase) * unitCostNum).toFixed(2)

      next[index] = {
        ...row,
        declaredPackQuantity: packQtyStr,
        receivedBaseQuantity: packQtyStr.trim() ? computedBase : '',
        lineTotal: isNaN(parseFloat(total)) ? '0.00' : total,
      }
      return next
    })
  }

  const handleBaseQuantityChange = (index: number, baseQtyStr: string) => {
    setLines((prev) => {
      const next = [...prev]
      const row = next[index]
      const baseQtyNum = parseFloat(baseQtyStr) || 0
      const unitCostNum = parseFloat(row.baseUnitCost) || 0
      const total = (baseQtyNum * unitCostNum).toFixed(2)

      next[index] = {
        ...row,
        receivedBaseQuantity: baseQtyStr,
        lineTotal: isNaN(parseFloat(total)) ? '0.00' : total,
      }
      return next
    })
  }

  const handleBaseUnitCostChange = (index: number, unitCostStr: string) => {
    setLines((prev) => {
      const next = [...prev]
      const row = next[index]
      const baseQtyNum = parseFloat(row.receivedBaseQuantity) || 0
      const unitCostNum = parseFloat(unitCostStr) || 0
      const total = (baseQtyNum * unitCostNum).toFixed(2)

      next[index] = {
        ...row,
        baseUnitCost: unitCostStr,
        lineTotal: isNaN(parseFloat(total)) ? '0.00' : total,
      }
      return next
    })
  }

  const handleLotCodeChange = (index: number, lotCode: string) => {
    setLines((prev) => {
      const next = [...prev]
      next[index] = { ...next[index], lotCode }
      return next
    })
  }

  const handleExpiresOnChange = (index: number, expiresOn: string) => {
    setLines((prev) => {
      const next = [...prev]
      next[index] = { ...next[index], expiresOn }
      return next
    })
  }

  // Calculated summary
  const invoiceTotalNumber = useMemo(() => {
    return lines.reduce((acc, row) => {
      const val = parseFloat(row.lineTotal)
      return acc + (isNaN(val) ? 0 : val)
    }, 0)
  }, [lines])

  // Form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (lines.length === 0) {
      setFormError('Vui lòng thêm ít nhất một dòng hàng nhập kho.')
      return
    }

    // Validate rows
    for (let i = 0; i < lines.length; i++) {
      const row = lines[i]
      const lineNo = i + 1

      if (!row.itemId) {
        setFormError(`Dòng #${lineNo}: Vui lòng chọn mặt hàng.`)
        return
      }

      const baseQty = parseFloat(row.receivedBaseQuantity)
      if (isNaN(baseQty) || baseQty <= 0) {
        setFormError(
          `Dòng #${lineNo}: Số lượng nhập phải lớn hơn 0 (đơn vị gốc).`
        )
        return
      }

      const cost = parseFloat(row.baseUnitCost)
      if (isNaN(cost) || cost < 0) {
        setFormError(`Dòng #${lineNo}: Đơn giá vốn không hợp lệ.`)
        return
      }

      const itemMeta = items.find((it) => it.id === row.itemId)
      if (itemMeta?.lotTrackingRequired && !row.lotCode.trim()) {
        setFormError(
          `Dòng #${lineNo}: Mặt hàng "${itemMeta.name}" yêu cầu bắt buộc phải có Số lô (Lot Code).`
        )
        return
      }
    }

    setIsSubmitting(true)
    try {
      const commandLines: CreateReceiptLineCommand[] = lines.map((r) => ({
        itemId: r.itemId,
        packName: r.packName || null,
        declaredPackQuantity: r.declaredPackQuantity
          ? parseFloat(r.declaredPackQuantity).toFixed(3)
          : null,
        conversionSnapshot: parseFloat(r.conversionSnapshot || '1').toFixed(6),
        receivedBaseQuantity: parseFloat(r.receivedBaseQuantity).toFixed(3),
        baseUnitCost: parseFloat(r.baseUnitCost).toFixed(6),
        lineTotal: parseFloat(r.lineTotal).toFixed(2),
        lotCode: r.lotCode.trim() ? r.lotCode.trim() : null,
        expiresOn: r.expiresOn ? r.expiresOn : null,
      }))

      const payload: CreateReceiptCommand = {
        supplierId: supplierId || null,
        receivedOn,
        invoiceNumber: invoiceNumber.trim() || null,
        paymentStatus,
        dueDate: paymentStatus === 'UNPAID' && dueDate ? dueDate : null,
        notes: notes.trim() || null,
        autoPost,
        lines: commandLines,
      }

      const created = await receiptApi.createReceipt(branchId, payload)
      onSuccess(created)
      onClose()
    } catch (err: unknown) {
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setFormError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tạo phiếu nhập kho. Vui lòng kiểm tra lại thông tin.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto bg-black/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.98, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: 10 }}
          transition={{ duration: 0.18 }}
          className="relative w-full max-w-5xl rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden my-6 flex flex-col max-h-[92vh]"
        >
          {/* Modal Header */}
          <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50/70 shrink-0">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#304638] text-white shadow-xs">
                <Receipt className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-slate-900">
                    Lập Phiếu Nhập Kho Mua Hàng
                  </h2>
                  <Badge variant="secondary" className="font-mono text-xs">
                    P0-INV-03 / BR-INV-05
                  </Badge>
                  <Badge
                    variant="outline"
                    className="border-emerald-600 text-emerald-800 bg-emerald-50 text-xs font-mono"
                  >
                    TC-008
                  </Badge>
                </div>
                <p className="text-xs text-slate-500">
                  Chi nhánh: <span className="font-semibold text-slate-700">{branchName || branchId}</span> · Nhập hàng trực tiếp, tự động quy đổi quy cách và ghi nhận công nợ.
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Form Content */}
          <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
            {/* Error Message */}
            {formError && (
              <div className="flex items-center gap-2.5 rounded-xl border border-red-200 bg-red-50 p-3.5 text-xs text-red-800">
                <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
                <span>{formError}</span>
              </div>
            )}

            {/* Section 1: Header Information */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 bg-slate-50/50 p-4 rounded-xl border border-slate-200">
              {/* Supplier Selection */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Nhà cung cấp <span className="text-red-500">*</span>
                </label>
                <select
                  value={supplierId}
                  onChange={(e) => setSupplierId(e.target.value)}
                  className="w-full text-xs rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-700"
                >
                  <option value="">-- Chọn Nhà Cung Cấp --</option>
                  {suppliers.map((s) => (
                    <option key={s.id} value={s.id}>
                      [{s.supplierCode}] {s.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Received Date */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Ngày nhận hàng <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Input
                    type="date"
                    value={receivedOn}
                    onChange={(e) => handleReceivedDateChange(e.target.value)}
                    className="text-xs pr-8"
                    required
                  />
                  <Calendar className="absolute right-2.5 top-2.5 h-4 w-4 text-slate-400 pointer-events-none" />
                </div>
              </div>

              {/* Invoice Number */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Số hóa đơn / Chứng từ NCC
                </label>
                <Input
                  type="text"
                  placeholder="Ví dụ: HD-VNM-9988"
                  value={invoiceNumber}
                  onChange={(e) => setInvoiceNumber(e.target.value)}
                  className="text-xs"
                />
              </div>

              {/* Payment Mode Selection */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Hình thức thanh toán <span className="text-red-500">*</span>
                </label>
                <select
                  value={paymentStatus}
                  onChange={(e) => {
                    const nextVal = e.target.value as PaymentStatus
                    setPaymentStatus(nextVal)
                    if (nextVal === 'UNPAID' && !dueDate) {
                      setDueDate(getDefaultDueDate(receivedOn))
                    }
                  }}
                  className="w-full text-xs rounded-lg border border-slate-300 bg-white px-3 py-2 font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-700"
                >
                  <option value="UNPAID">Chưa thanh toán (Ghi nợ NCC) - TC-008</option>
                  <option value="PAID">Đã thanh toán ngay (Tiền mặt / Chuyển khoản)</option>
                </select>
              </div>

              {/* Due Date (If Unpaid) */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Hạn thanh toán công nợ
                </label>
                <Input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  disabled={paymentStatus !== 'UNPAID'}
                  className="text-xs disabled:opacity-50"
                />
              </div>

              {/* Notes */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Ghi chú phiếu nhập
                </label>
                <Input
                  type="text"
                  placeholder="Ghi chú đợt giao hàng..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="text-xs"
                />
              </div>
            </div>

            {/* TC-008 Notice Banner */}
            {paymentStatus === 'UNPAID' ? (
              <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50/80 p-3.5 text-xs text-amber-900">
                <Info className="h-4 w-4 shrink-0 text-amber-600 mt-0.5" />
                <div>
                  <span className="font-semibold text-amber-950">
                    Quy tắc kế toán TC-008 (Ghi nhận công nợ):
                  </span>{' '}
                  Khi chọn &ldquo;Chưa thanh toán (Ghi nợ)&rdquo;, hệ thống sẽ tự động ghi nhận công nợ NCC vào sổ cái và tăng tồn kho. Quỹ tiền mặt/ngân hàng của quán sẽ{' '}
                  <strong className="underline">KHÔNG</strong> bị trừ và chi phí giá vốn (COGS) trong kỳ{' '}
                  <strong className="underline">KHÔNG</strong> bị ảnh hưởng cho đến khi lập phiếu chi trả nợ.
                </div>
              </div>
            ) : (
              <div className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50/80 p-3.5 text-xs text-emerald-900">
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600 mt-0.5" />
                <div>
                  <span className="font-semibold text-emerald-950">
                    Thanh toán ngay:
                  </span>{' '}
                  Phiếu nhập được đánh dấu đã thanh toán toàn bộ. Hệ thống không tạo bản ghi nợ nhà cung cấp.
                </div>
              </div>
            )}

            {/* Section 2: Line Items Dynamic Table */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <span>Chi tiết mặt hàng nhập kho</span>
                    <Badge variant="outline" className="text-xs">
                      {lines.length} dòng
                    </Badge>
                  </h3>
                  <p className="text-xs text-slate-500">
                    Hỗ trợ chọn quy cách thùng/bao (TC-023) để tự động quy đổi sang số lượng đơn vị gốc.
                  </p>
                </div>

                <Button
                  type="button"
                  onClick={handleAddLine}
                  variant="outline"
                  size="sm"
                  className="text-xs border-emerald-700 text-emerald-900 hover:bg-emerald-50 gap-1.5"
                >
                  <Plus className="h-3.5 w-3.5" />
                  <span>Thêm dòng hàng</span>
                </Button>
              </div>

              <div className="border border-slate-200 rounded-xl overflow-hidden shadow-2xs">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-100/80 text-slate-700 font-semibold border-b border-slate-200">
                      <tr>
                        <th className="py-2.5 px-3 w-10 text-center">#</th>
                        <th className="py-2.5 px-3 min-w-[220px]">Mặt hàng</th>
                        <th className="py-2.5 px-3 min-w-[150px]">Quy cách (TC-023)</th>
                        <th className="py-2.5 px-3 min-w-[100px]">Số lượng đóng gói</th>
                        <th className="py-2.5 px-3 min-w-[110px]">SL Đơn vị gốc</th>
                        <th className="py-2.5 px-3 min-w-[120px]">Đơn giá vốn gốc (₫)</th>
                        <th className="py-2.5 px-3 min-w-[130px] text-right">Thành tiền (₫)</th>
                        <th className="py-2.5 px-3 min-w-[130px]">Số lô (Lot)</th>
                        <th className="py-2.5 px-3 min-w-[120px]">Hạn dùng</th>
                        <th className="py-2.5 px-3 w-12 text-center">Xóa</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200 bg-white">
                      {lines.map((row, idx) => {
                        const selectedItemMeta = items.find((i) => i.id === row.itemId)
                        const itemConversions = row.itemId
                          ? packConversionsCache[row.itemId] || []
                          : []
                        const isPackSelected = Boolean(row.selectedPackConversionId)
                        const isLotRequired = selectedItemMeta?.lotTrackingRequired

                        return (
                          <tr key={row.id} className="hover:bg-slate-50/60">
                            {/* Line No */}
                            <td className="py-2 px-3 text-center text-slate-500 font-mono">
                              {idx + 1}
                            </td>

                            {/* Item Select */}
                            <td className="py-2 px-3">
                              <select
                                value={row.itemId}
                                onChange={(e) => handleItemSelect(idx, e.target.value)}
                                className="w-full text-xs rounded-md border border-slate-300 bg-white px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-emerald-700 font-medium text-slate-800"
                              >
                                <option value="">-- Chọn mặt hàng --</option>
                                {items.map((it) => (
                                  <option key={it.id} value={it.id}>
                                    [{it.itemCode}] {it.name} ({it.baseUnitName})
                                  </option>
                                ))}
                              </select>
                              {selectedItemMeta && (
                                <div className="mt-1 flex items-center gap-1.5 text-[10px] text-slate-500">
                                  <span>Đơn vị gốc: <strong className="text-slate-700">{selectedItemMeta.baseUnitName}</strong></span>
                                  {isLotRequired && (
                                    <span className="text-amber-700 font-semibold flex items-center gap-0.5">
                                      • <AlertTriangle className="h-2.5 w-2.5" /> Bắt buộc Lô
                                    </span>
                                  )}
                                </div>
                              )}
                            </td>

                            {/* Pack Conversion Select */}
                            <td className="py-2 px-3">
                              <select
                                value={row.selectedPackConversionId}
                                onChange={(e) =>
                                  handlePackConversionSelect(
                                    idx,
                                    e.target.value,
                                    itemConversions
                                  )
                                }
                                disabled={!row.itemId}
                                className="w-full text-xs rounded-md border border-slate-300 bg-white px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-emerald-700 disabled:opacity-50"
                              >
                                <option value="">Đơn vị gốc ({selectedItemMeta?.baseUnitName || 'gốc'})</option>
                                {itemConversions.map((conv) => (
                                  <option key={conv.id} value={conv.id}>
                                    {conv.packName} (x{conv.baseUnitsPerPack})
                                  </option>
                                ))}
                              </select>
                            </td>

                            {/* Pack Quantity */}
                            <td className="py-2 px-3">
                              <Input
                                type="number"
                                step="any"
                                min="0"
                                placeholder="0"
                                value={row.declaredPackQuantity}
                                onChange={(e) => handlePackQuantityChange(idx, e.target.value)}
                                disabled={!isPackSelected}
                                className="text-xs h-8 disabled:bg-slate-100 disabled:text-slate-400"
                              />
                            </td>

                            {/* Base Quantity */}
                            <td className="py-2 px-3">
                              <Input
                                type="number"
                                step="any"
                                min="0"
                                placeholder="0"
                                value={row.receivedBaseQuantity}
                                onChange={(e) => handleBaseQuantityChange(idx, e.target.value)}
                                readOnly={isPackSelected}
                                className={`text-xs h-8 font-mono font-semibold ${
                                  isPackSelected
                                    ? 'bg-emerald-50/70 border-emerald-300 text-emerald-900 cursor-not-allowed'
                                    : ''
                                }`}
                              />
                            </td>

                            {/* Base Unit Cost */}
                            <td className="py-2 px-3">
                              <Input
                                type="number"
                                step="any"
                                min="0"
                                placeholder="0"
                                value={row.baseUnitCost}
                                onChange={(e) => handleBaseUnitCostChange(idx, e.target.value)}
                                className="text-xs h-8 font-mono text-slate-800"
                              />
                            </td>

                            {/* Line Total */}
                            <td className="py-2 px-3 text-right font-mono font-bold text-slate-900">
                              {formatVnd(row.lineTotal)}
                            </td>

                            {/* Lot Code */}
                            <td className="py-2 px-3">
                              <Input
                                type="text"
                                placeholder={isLotRequired ? 'Mã lô (Bắt buộc)' : 'Mã lô'}
                                value={row.lotCode}
                                onChange={(e) => handleLotCodeChange(idx, e.target.value)}
                                className={`text-xs h-8 ${
                                  isLotRequired && !row.lotCode.trim()
                                    ? 'border-amber-400 bg-amber-50/30'
                                    : ''
                                }`}
                              />
                            </td>

                            {/* Expires On */}
                            <td className="py-2 px-3">
                              <Input
                                type="date"
                                value={row.expiresOn}
                                onChange={(e) => handleExpiresOnChange(idx, e.target.value)}
                                className="text-xs h-8"
                              />
                            </td>

                            {/* Delete Line */}
                            <td className="py-2 px-3 text-center">
                              <button
                                type="button"
                                onClick={() => handleRemoveLine(idx)}
                                disabled={lines.length === 1}
                                className="text-slate-400 hover:text-red-600 disabled:opacity-30 disabled:hover:text-slate-400 p-1"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>

                {/* Table Footer with Summary */}
                <div className="bg-slate-50 border-t border-slate-200 px-4 py-3 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs">
                  <div className="flex items-center gap-4 text-slate-600">
                    <span>
                      Tổng cộng: <strong>{lines.length}</strong> dòng hàng
                    </span>
                    <span>•</span>
                    <label className="flex items-center gap-2 cursor-pointer select-none">
                      <input
                        type="checkbox"
                        checked={autoPost}
                        onChange={(e) => setAutoPost(e.target.checked)}
                        className="rounded border-slate-300 text-emerald-700 focus:ring-emerald-700"
                      />
                      <span className="font-medium text-slate-800">
                        Chốt ghi sổ ngay (Auto-post: tăng tồn kho & công nợ ngay khi lưu)
                      </span>
                    </label>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-slate-500 font-medium">Tổng tiền phiếu nhập:</span>
                    <span className="text-base font-bold font-mono text-emerald-900 bg-emerald-100/70 px-3 py-1 rounded-lg">
                      {formatVnd(invoiceTotalNumber)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
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
                disabled={isSubmitting}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-sm px-5"
              >
                {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                <span>
                  {autoPost ? 'Lưu & Chốt ghi sổ phiếu nhập' : 'Lưu bản nháp (Draft)'}
                </span>
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
