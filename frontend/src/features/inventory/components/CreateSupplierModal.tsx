import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, Truck, AlertCircle, Loader2 } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { catalogApi } from '../api/catalogApi'
import type { SupplierDto, CreateSupplierCommand, SupplierStatus } from '../types/catalog.types'

interface CreateSupplierModalProps {
  isOpen: boolean
  entityId: string
  onClose: () => void
  onSuccess: (supplier: SupplierDto) => void
}

export const CreateSupplierModal: React.FC<CreateSupplierModalProps> = ({
  isOpen,
  entityId,
  onClose,
  onSuccess,
}) => {
  const [supplierCode, setSupplierCode] = useState('')
  const [name, setName] = useState('')
  const [contactNote, setContactNote] = useState('')
  const [status, setStatus] = useState<SupplierStatus>('ACTIVE')

  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    const normalizedCode = supplierCode.trim().toUpperCase()
    if (!normalizedCode) {
      setError('Mã nhà cung cấp không được để trống (VD: SUP-VINAMILK).')
      return
    }
    if (!name.trim()) {
      setError('Tên nhà cung cấp không được để trống.')
      return
    }

    const payload: CreateSupplierCommand = {
      supplierCode: normalizedCode,
      name: name.trim(),
      contactNote: contactNote.trim() ? contactNote.trim() : null,
      status,
    }

    setIsLoading(true)
    try {
      const created = await catalogApi.createSupplier(entityId, payload)
      onSuccess(created)
      onClose()
      setSupplierCode('')
      setName('')
      setContactNote('')
      setStatus('ACTIVE')
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
        'Không thể tạo nhà cung cấp. Vui lòng kiểm tra lại.'
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
                <Truck className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-slate-900">
                  Thêm Nhà Cung Cấp Mới
                </h3>
                <p className="text-xs text-slate-500 font-mono">
                  Quản lý đối tác cung ứng & quy cách bao bì theo pháp nhân
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
              {/* Supplier Code */}
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Mã nhà cung cấp <span className="text-red-500">*</span>
                </label>
                <Input
                  value={supplierCode}
                  onChange={(e) => setSupplierCode(e.target.value.toUpperCase())}
                  placeholder="VD: SUP-VINAMILK"
                  className="font-mono text-sm uppercase"
                  required
                />
              </div>

              {/* Status */}
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Trạng thái <span className="text-red-500">*</span>
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as SupplierStatus)}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="ACTIVE">Đang hợp tác (ACTIVE)</option>
                  <option value="INACTIVE">Tạm dừng (INACTIVE)</option>
                </select>
              </div>
            </div>

            {/* Name */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Tên nhà cung cấp <span className="text-red-500">*</span>
              </label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="VD: Công ty Cổ phần Sữa Việt Nam (Vinamilk)"
                className="text-sm"
                required
              />
            </div>

            {/* Contact Note */}
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Thông tin liên hệ & Ghi chú
              </label>
              <textarea
                value={contactNote}
                onChange={(e) => setContactNote(e.target.value)}
                placeholder="Hotline, Email, Người phụ trách kinh doanh, Địa chỉ kho giao hàng..."
                rows={3}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
              />
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
                  'Lưu nhà cung cấp'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
