import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, Edit3, AlertCircle, Loader2, ShieldAlert } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { catalogApi } from '../api/catalogApi'
import type {
  CatalogItemDto,
  UpdateCatalogItemCommand,
  ItemKind,
  ItemStatus,
} from '../types/catalog.types'

interface EditItemModalProps {
  isOpen: boolean
  entityId: string
  item: CatalogItemDto | null
  onClose: () => void
  onSuccess: (updated: CatalogItemDto) => void
}

const ITEM_KIND_OPTIONS: Array<{ value: ItemKind; label: string }> = [
  { value: 'INGREDIENT', label: 'Nguyên vật liệu (Pha chế / Chế biến)' },
  { value: 'CONSUMABLE', label: 'Vật tư tiêu hao (Ly, ống hút, túi...)' },
  { value: 'EQUIPMENT', label: 'Thiết bị & Dụng cụ quầy' },
  { value: 'OTHER', label: 'Khác' },
]

const ITEM_STATUS_OPTIONS: Array<{ value: ItemStatus; label: string }> = [
  { value: 'ACTIVE', label: 'Đang hoạt động (ACTIVE)' },
  { value: 'INACTIVE', label: 'Tạm ngưng (INACTIVE)' },
  { value: 'ARCHIVED', label: 'Lưu trữ / Ngừng sử dụng (ARCHIVED)' },
]

export const EditItemModal: React.FC<EditItemModalProps> = ({
  isOpen,
  entityId,
  item,
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('')
  const [itemKind, setItemKind] = useState<ItemKind>('INGREDIENT')
  const [status, setStatus] = useState<ItemStatus>('ACTIVE')
  const [lotTrackingRequired, setLotTrackingRequired] = useState(false)
  const [expirationTrackingRequired, setExpirationTrackingRequired] = useState(false)

  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (item) {
      setName(item.name)
      setItemKind(item.itemKind)
      setStatus(item.status)
      setLotTrackingRequired(item.lotTrackingRequired)
      setExpirationTrackingRequired(item.expirationTrackingRequired)
      setError(null)
    }
  }, [item])

  if (!isOpen || !item) return null

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

    if (!name.trim()) {
      setError('Tên mặt hàng không được để trống.')
      return
    }

    if (expirationTrackingRequired && !lotTrackingRequired) {
      setError('Theo dõi hạn sử dụng bắt buộc phải theo dõi số lô.')
      return
    }

    const payload: UpdateCatalogItemCommand = {
      name: name.trim(),
      itemKind,
      lotTrackingRequired,
      expirationTrackingRequired,
      status,
    }

    setIsLoading(true)
    try {
      const updated = await catalogApi.updateCatalogItem(entityId, item.id, payload)
      onSuccess(updated)
      onClose()
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
        'Không thể cập nhật mặt hàng. Vui lòng kiểm tra lại thông tin.'
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
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50 text-amber-700">
                <Edit3 className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-slate-900">
                  Cập Nhật Mặt Hàng
                </h3>
                <p className="text-xs text-slate-500 font-mono">
                  Mã hàng: {item.itemCode} (Đơn vị gốc: {item.baseUnitName} - {item.baseUnitCode})
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

            {/* Name */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Tên mặt hàng <span className="text-red-500">*</span>
              </label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="VD: Sữa đặc Ông Thọ đỏ"
                className="text-sm"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
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

              {/* Status */}
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Trạng thái <span className="text-red-500">*</span>
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as ItemStatus)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  {ITEM_STATUS_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Invariant Tracking Checkboxes */}
            <div className="rounded-xl border border-slate-200 bg-slate-50/60 p-4 space-y-3">
              <div className="flex items-center gap-2">
                <ShieldAlert className="h-4 w-4 text-emerald-700" />
                <span className="text-xs font-semibold text-slate-800">
                  Ràng buộc kiểm soát tồn kho
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
                    Bắt buộc ghi nhận <strong>Số lô (Lot Code)</strong>
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
                  'Lưu thay đổi'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
