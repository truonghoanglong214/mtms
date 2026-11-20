import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, Layers, AlertCircle, Loader2 } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { catalogApi } from '../api/catalogApi'
import type {
  CatalogItemDto,
  SupplierDto,
  SupplierPackConversionDto,
  CreatePackConversionCommand,
} from '../types/catalog.types'

interface CreatePackConversionModalProps {
  isOpen: boolean
  entityId: string
  items: CatalogItemDto[]
  suppliers: SupplierDto[]
  initialItemId?: string
  onClose: () => void
  onSuccess: (created: SupplierPackConversionDto) => void
}

export const CreatePackConversionModal: React.FC<CreatePackConversionModalProps> = ({
  isOpen,
  entityId,
  items,
  suppliers,
  initialItemId,
  onClose,
  onSuccess,
}) => {
  const [itemId, setItemId] = useState(initialItemId || '')
  const [supplierId, setSupplierId] = useState('')
  const [packName, setPackName] = useState('')
  const [baseUnitsPerPack, setBaseUnitsPerPack] = useState('')
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [effectiveTo, setEffectiveTo] = useState('')

  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Find selected item to display base unit
  const selectedItem = items.find((i) => i.id === itemId)

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!itemId) {
      setError('Vui lòng chọn mặt hàng.')
      return
    }
    if (!supplierId) {
      setError('Vui lòng chọn nhà cung cấp.')
      return
    }
    if (!packName.trim()) {
      setError('Tên quy cách đóng gói không được để trống (VD: Thùng 24 lon).')
      return
    }

    const multiplier = parseFloat(baseUnitsPerPack)
    if (isNaN(multiplier) || multiplier <= 0) {
      setError('Tỉ lệ quy đổi (Hệ số nhân ra đơn vị gốc) phải là số dương lớn hơn 0.')
      return
    }

    if (!effectiveFrom) {
      setError('Vui lòng chọn ngày bắt đầu hiệu lực.')
      return
    }

    if (effectiveTo && effectiveTo < effectiveFrom) {
      setError('Ngày kết thúc hiệu lực phải lớn hơn hoặc bằng ngày bắt đầu hiệu lực.')
      return
    }

    const payload: CreatePackConversionCommand = {
      supplierId,
      packName: packName.trim(),
      baseUnitsPerPack: multiplier.toFixed(3),
      effectiveFrom,
      effectiveTo: effectiveTo ? effectiveTo : null,
    }

    setIsLoading(true)
    try {
      const created = await catalogApi.createPackConversion(entityId, itemId, payload)
      onSuccess(created)
      onClose()
      // Reset form
      setPackName('')
      setBaseUnitsPerPack('')
      setEffectiveTo('')
    } catch (err: unknown) {
      const apiErr = err as {
        response?: {
          data?: {
            title?: string
            detail?: string
            message?: string
          }
        }
      }
      const msg =
        apiErr.response?.data?.detail ||
        apiErr.response?.data?.title ||
        apiErr.response?.data?.message ||
        'Không thể thiết lập quy cách đóng gói. Vui lòng kiểm tra lại.'
      setError(msg)
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
          className="fixed inset-0 bg-black/60 backdrop-blur-sm"
        />

        {/* Modal Dialog */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 1, y: 0 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-2xl z-10 border border-slate-100"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
            <div className="flex items-center space-x-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-[#304638]">
                <Layers className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-slate-900">
                  Thiết Lập Quy Cách Nhà Cung Cấp
                </h3>
                <p className="text-xs text-slate-500 font-mono">
                  BR-INV-04 / TC-023 · Quy đổi đóng gói & Snapshot nhập kho
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            {error && (
              <div className="flex items-start space-x-2.5 rounded-xl border border-red-200 bg-red-50/80 p-3 text-xs text-red-700">
                <AlertCircle className="h-4 w-4 shrink-0 text-red-600 mt-0.5" />
                <span className="leading-relaxed">{error}</span>
              </div>
            )}

            {/* Target Item */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Mặt hàng áp dụng <span className="text-red-500">*</span>
              </label>
              <select
                value={itemId}
                onChange={(e) => setItemId(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                required
              >
                <option value="">-- Chọn mặt hàng --</option>
                {items.map((i) => (
                  <option key={i.id} value={i.id}>
                    [{i.itemCode}] {i.name} (Gốc: {i.baseUnitName} - {i.baseUnitCode})
                  </option>
                ))}
              </select>
            </div>

            {/* Supplier */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Nhà cung cấp <span className="text-red-500">*</span>
              </label>
              <select
                value={supplierId}
                onChange={(e) => setSupplierId(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                required
              >
                <option value="">-- Chọn nhà cung cấp --</option>
                {suppliers.map((s) => (
                  <option key={s.id} value={s.id}>
                    [{s.supplierCode}] {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              {/* Pack Name */}
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Tên quy cách <span className="text-red-500">*</span>
                </label>
                <Input
                  value={packName}
                  onChange={(e) => setPackName(e.target.value)}
                  placeholder="VD: Thùng 24 lon"
                  className="text-sm"
                  required
                />
              </div>

              {/* Multiplier / baseUnitsPerPack */}
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Số lượng gốc / Quy cách <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Input
                    type="number"
                    step="0.001"
                    min="0.001"
                    value={baseUnitsPerPack}
                    onChange={(e) => setBaseUnitsPerPack(e.target.value)}
                    placeholder="VD: 24.000"
                    className="text-sm font-mono pr-14"
                    required
                  />
                  <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-xs text-slate-400 font-semibold">
                    {selectedItem ? selectedItem.baseUnitCode : 'Gốc'}
                  </div>
                </div>
              </div>
            </div>

            {/* Date Span */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Hiệu lực từ ngày <span className="text-red-500">*</span>
                </label>
                <Input
                  type="date"
                  value={effectiveFrom}
                  onChange={(e) => setEffectiveFrom(e.target.value)}
                  className="text-sm"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Hiệu lực đến ngày (Tuỳ chọn)
                </label>
                <Input
                  type="date"
                  value={effectiveTo}
                  onChange={(e) => setEffectiveTo(e.target.value)}
                  className="text-sm"
                />
              </div>
            </div>

            {/* Invariant Explanation Box */}
            <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 p-3 text-[11px] text-emerald-800 space-y-1">
              <p className="font-semibold">
                * Quy định TC-023 & Bất biến nhập kho (Receipt Snapshot Invariant):
              </p>
              <p>
                Khi lập phiếu nhập kho theo quy cách nhà cung cấp (ví dụ: nhập 2 thùng), hệ thống sẽ
                sử dụng tỉ lệ <code>{baseUnitsPerPack || '24.000'}</code> để tự động tính ra{' '}
                <strong>
                  {baseUnitsPerPack ? (parseFloat(baseUnitsPerPack) * 2).toFixed(3) : '48.000'}{' '}
                  {selectedItem?.baseUnitName || 'đơn vị gốc'}
                </strong>
                . Tỉ lệ quy đổi tại thời điểm nhập sẽ được snapshot cố định vĩnh viễn trong chứng từ.
              </p>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end space-x-3 pt-3 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isLoading}
                className="text-xs"
              >
                Huỷ bỏ
              </Button>
              <Button
                type="submit"
                disabled={isLoading}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-medium"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-3.5 w-3.5 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  'Lưu quy cách'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
