import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, Scale, AlertCircle, Loader2 } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { catalogApi } from '../api/catalogApi'
import type { UnitDto, CreateUnitCommand, UnitDimension } from '../types/catalog.types'

interface CreateUnitModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (unit: UnitDto) => void
}

const DIMENSION_OPTIONS: Array<{ value: UnitDimension; label: string; description: string }> = [
  { value: 'MASS', label: 'Khối lượng (MASS)', description: 'Kg, g, mg...' },
  { value: 'VOLUME', label: 'Thể tích (VOLUME)', description: 'Lít, ml...' },
  { value: 'COUNT', label: 'Số đếm (COUNT)', description: 'Lon, hộp, chai, cái, gói...' },
  { value: 'OTHER', label: 'Khác (OTHER)', description: 'Mét, cuộn, bộ...' },
]

export const CreateUnitModal: React.FC<CreateUnitModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [unitCode, setUnitCode] = useState('')
  const [unitName, setUnitName] = useState('')
  const [dimension, setDimension] = useState<UnitDimension>('COUNT')

  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    const normalizedCode = unitCode.trim().toUpperCase()
    if (!normalizedCode) {
      setError('Mã đơn vị không được để trống (VD: KG, L, CAN, BOX).')
      return
    }
    if (!unitName.trim()) {
      setError('Tên đơn vị đo lường không được để trống.')
      return
    }

    const payload: CreateUnitCommand = {
      unitCode: normalizedCode,
      unitName: unitName.trim(),
      dimension,
    }

    setIsLoading(true)
    try {
      const created = await catalogApi.createUnit(payload)
      onSuccess(created)
      onClose()
      setUnitCode('')
      setUnitName('')
      setDimension('COUNT')
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
        'Không thể tạo đơn vị đo lường chuẩn. Vui lòng kiểm tra quyền hệ thống.'
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
          className="relative w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl z-10 border border-slate-100"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4 bg-slate-50/50">
            <div className="flex items-center space-x-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-[#304638]">
                <Scale className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-slate-900">
                  Thêm Đơn Vị Tính Chuẩn
                </h3>
                <p className="text-xs text-slate-500 font-mono">
                  Canonical UOM & Physical Dimension
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

            {/* Code */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Mã đơn vị tính <span className="text-red-500">*</span>
              </label>
              <Input
                value={unitCode}
                onChange={(e) => setUnitCode(e.target.value.toUpperCase())}
                placeholder="VD: BOX, CAN, KG, ML"
                className="font-mono text-sm uppercase"
                required
              />
            </div>

            {/* Name */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Tên hiển thị <span className="text-red-500">*</span>
              </label>
              <Input
                value={unitName}
                onChange={(e) => setUnitName(e.target.value)}
                placeholder="VD: Hộp / Lon / Chai / Thùng"
                className="text-sm"
                required
              />
            </div>

            {/* Dimension */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Chiều không gian vật lý (Dimension) <span className="text-red-500">*</span>
              </label>
              <select
                value={dimension}
                onChange={(e) => setDimension(e.target.value as UnitDimension)}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
              >
                {DIMENSION_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label} ({opt.description})
                  </option>
                ))}
              </select>
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
                  'Lưu đơn vị'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
