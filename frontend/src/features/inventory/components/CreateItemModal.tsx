import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, PackagePlus, AlertCircle, Loader2, ShieldAlert } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { catalogApi } from '../api/catalogApi'
import type {
  CatalogItemDto,
  CreateCatalogItemCommand,
  ItemKind,
  UnitDto,
} from '../types/catalog.types'

interface CreateItemModalProps {
  isOpen: boolean
  entityId: string
  units: UnitDto[]
  onClose: () => void
  onSuccess: (item: CatalogItemDto) => void
}

const ITEM_KIND_OPTIONS: Array<{ value: ItemKind; label: string }> = [
  { value: 'INGREDIENT', label: 'Nguyên vật liệu (Pha chế / Chế biến)' },
  { value: 'CONSUMABLE', label: 'Vật tư tiêu hao (Ly, ống hút, túi...)' },
  { value: 'EQUIPMENT', label: 'Thiết bị & Dụng cụ quầy' },
  { value: 'OTHER', label: 'Khác' },
]

export const CreateItemModal: React.FC<CreateItemModalProps> = ({
  isOpen,
  entityId,
  units,
  onClose,
  onSuccess,
}) => {
  const [itemCode, setItemCode] = useState('')
  const [name, setName] = useState('')
  const [baseUnitId, setBaseUnitId] = useState('')
  const [itemKind, setItemKind] = useState<ItemKind>('INGREDIENT')
  const [lotTrackingRequired, setLotTrackingRequired] = useState(false)
  const [expirationTrackingRequired, setExpirationTrackingRequired] = useState(false)

  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!isOpen) return null

  // Invariant enforcement: Expiration requires Lot Tracking
  const handleExpirationChange = (checked: boolean) => {
    setExpirationTrackingRequired(checked)
    if (checked) {
      setLotTrackingRequired(true)
    }
  }

  const handleLotChange = (checked: boolean) => {
    setLotTrackingRequired(checked)
    if (!checked) {
      setExpirationTrackingRequired(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    const normalizedCode = itemCode.trim().toUpperCase()
    if (!normalizedCode) {
      setError('Mã mặt hàng không được để trống.')
      return
    }
    const codeRegex = /^[A-Z0-9_-]{3,50}$/
    if (!codeRegex.test(normalizedCode)) {
      setError('Mã mặt hàng chỉ gồm 3-50 ký tự chữ in hoa, chữ số, gạch nối (-) hoặc gạch dưới (_).')
      return
    }

    if (!name.trim()) {
      setError('Tên mặt hàng không được để trống.')
      return
    }

    if (!baseUnitId) {
      setError('Vui lòng chọn đơn vị tính chuẩn (Base Unit).')
      return
    }

    // Double-check invariant before sending
    if (expirationTrackingRequired && !lotTrackingRequired) {
      setError('Theo dõi hạn sử dụng bắt buộc phải theo dõi số lô.')
      return
    }

    const payload: CreateCatalogItemCommand = {
      itemCode: normalizedCode,
      name: name.trim(),
      baseUnitId,
      itemKind,
      lotTrackingRequired,
      expirationTrackingRequired,
    }

    setIsLoading(true)
    try {
      const created = await catalogApi.createCatalogItem(entityId, payload)
      onSuccess(created)
      onClose()
      // Reset form
      setItemCode('')
      setName('')
      setBaseUnitId('')
      setItemKind('INGREDIENT')
      setLotTrackingRequired(false)
      setExpirationTrackingRequired(false)
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
        'Không thể tạo mặt hàng. Vui lòng kiểm tra lại thông tin.'
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
          exit={{ opacity: 0, scale: 0.95, y: 10 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-2xl z-10 border border-slate-100"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
            <div className="flex items-center space-x-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-[#304638]">
                <PackagePlus className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-slate-900">
                  Thêm Mặt Hàng / Nguyên Vật Liệu
                </h3>
                <p className="text-xs text-slate-500 font-mono">
                  Đơn vị tính gốc và kiểm soát tồn kho
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

            <div className="grid grid-cols-2 gap-4">
              {/* Item Code */}
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Mã mặt hàng <span className="text-red-500">*</span>
                </label>
                <Input
                  value={itemCode}
                  onChange={(e) => setItemCode(e.target.value.toUpperCase())}
                  placeholder="VD: ING-MILK-OT-380G"
                  className="font-mono text-sm uppercase"
                  required
                />
                <p className="mt-1 text-[11px] text-slate-400">Chữ in hoa, số, gạch ngang</p>
              </div>

              {/* Item Kind */}
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Phân loại <span className="text-red-500">*</span>
                </label>
                <select
                  value={itemKind}
                  onChange={(e) => setItemKind(e.target.value as ItemKind)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  {ITEM_KIND_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Name */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Tên mặt hàng <span className="text-red-500">*</span>
              </label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="VD: Sữa đặc có đường Ông Thọ đỏ lon 380g"
                className="text-sm"
                required
              />
            </div>

            {/* Base Unit */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Đơn vị tính chuẩn (Base Unit) <span className="text-red-500">*</span>
              </label>
              <select
                value={baseUnitId}
                onChange={(e) => setBaseUnitId(e.target.value)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                required
              >
                <option value="">-- Chọn đơn vị đo lường gốc --</option>
                {units.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.unitName} ({u.unitCode}) - {u.dimension}
                  </option>
                ))}
              </select>
              <p className="mt-1 text-[11px] text-slate-400">
                Đơn vị gốc dùng để lưu trữ tồn kho và tính giá vốn Moving WAC.
              </p>
            </div>

            {/* Invariant Tracking Checkboxes */}
            <div className="rounded-xl border border-slate-200 bg-slate-50/60 p-4 space-y-3">
              <div className="flex items-center gap-2">
                <ShieldAlert className="h-4 w-4 text-emerald-700" />
                <span className="text-xs font-semibold text-slate-800">
                  Ràng buộc kiểm soát tồn kho (Inventory Tracking Invariant)
                </span>
              </div>

              <div className="space-y-2">
                <label className="flex items-center space-x-2.5 cursor-pointer text-xs text-slate-700">
                  <input
                    type="checkbox"
                    checked={lotTrackingRequired}
                    onChange={(e) => handleLotChange(e.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 text-[#304638] focus:ring-[#304638]"
                  />
                  <span>
                    Bắt buộc ghi nhận <strong>Số lô (Lot Code)</strong> khi nhập kho
                  </span>
                </label>

                <label className="flex items-center space-x-2.5 cursor-pointer text-xs text-slate-700">
                  <input
                    type="checkbox"
                    checked={expirationTrackingRequired}
                    onChange={(e) => handleExpirationChange(e.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 text-[#304638] focus:ring-[#304638]"
                  />
                  <span>
                    Bắt buộc theo dõi <strong>Hạn sử dụng (Expiration Date)</strong>
                  </span>
                </label>
              </div>

              <p className="text-[11px] text-slate-500 italic">
                * Quy định bất biến: Theo dõi HSD bắt buộc phải theo dõi Số lô. Không cho phép xuất
                kho lô đã quá hạn.
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
                  'Lưu mặt hàng'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
