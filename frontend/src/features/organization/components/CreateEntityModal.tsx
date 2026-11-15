import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, Building2, AlertCircle, Loader2 } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { organizationApi } from '../organizationApi'
import type { BusinessEntityDto } from '../types'

interface CreateEntityModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (entity: BusinessEntityDto) => void
}

export const CreateEntityModal: React.FC<CreateEntityModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [entityCode, setEntityCode] = useState('')
  const [registeredName, setRegisteredName] = useState('')
  const [taxCode, setTaxCode] = useState('')
  const [status, setStatus] = useState<'ACTIVE' | 'INACTIVE'>('ACTIVE')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!entityCode.trim()) {
      setError('Mã pháp nhân không được để trống')
      return
    }
    if (!registeredName.trim()) {
      setError('Tên đăng ký kinh doanh không được để trống')
      return
    }

    setIsLoading(true)
    try {
      const created = await organizationApi.createEntity({
        entityCode: entityCode.trim().toUpperCase(),
        registeredName: registeredName.trim(),
        taxCode: taxCode.trim(),
        status,
      })
      onSuccess(created)
      onClose()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tạo pháp nhân. Vui lòng kiểm tra lại thông tin.'
      )
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
          className="fixed inset-0 bg-stone-900/60 backdrop-blur-sm"
        />

        {/* Modal Window */}
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-lg rounded-xl border border-stone-800 bg-stone-900 p-6 shadow-2xl z-10 text-stone-100"
        >
          <div className="flex items-center justify-between pb-4 border-b border-stone-800">
            <div className="flex items-center space-x-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-950/80 border border-emerald-500/20 text-emerald-400">
                <Building2 className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-stone-100">Thêm Pháp nhân mới</h3>
                <p className="text-xs text-stone-400">Đăng ký pháp nhân doanh nghiệp hoặc hộ kinh doanh</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="rounded-lg p-1 text-stone-400 hover:bg-stone-800 hover:text-stone-200 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {error && (
            <div className="mt-4 flex items-center space-x-2 rounded-lg border border-red-500/30 bg-red-950/40 p-3 text-xs text-red-300">
              <AlertCircle className="h-4 w-4 shrink-0 text-red-400" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-4 space-y-4">
            <div>
              <label className="block text-xs font-medium text-stone-300 mb-1.5">
                Mã Pháp nhân <span className="text-emerald-400 font-mono">*</span>
              </label>
              <Input
                value={entityCode}
                onChange={(e) => setEntityCode(e.target.value.toUpperCase())}
                placeholder="VD: LTT_VN, LTT_HN, HKD_BA_TRIEU"
                className="font-mono uppercase bg-stone-950 border-stone-800 text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
                required
                maxLength={40}
              />
              <p className="mt-1 text-[11px] text-stone-400">Mã định danh duy nhất của pháp nhân trong hệ thống.</p>
            </div>

            <div>
              <label className="block text-xs font-medium text-stone-300 mb-1.5">
                Tên Đăng ký Doanh nghiệp <span className="text-emerald-400 font-mono">*</span>
              </label>
              <Input
                value={registeredName}
                onChange={(e) => setRegisteredName(e.target.value)}
                placeholder="VD: Công ty Cổ phần Lục Trà Tinh Hoa"
                className="bg-stone-950 border-stone-800 text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
                required
                maxLength={160}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-stone-300 mb-1.5">
                  Mã số thuế
                </label>
                <Input
                  value={taxCode}
                  onChange={(e) => setTaxCode(e.target.value)}
                  placeholder="0109999999"
                  className="font-mono bg-stone-950 border-stone-800 text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
                  maxLength={40}
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-stone-300 mb-1.5">
                  Trạng thái
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as 'ACTIVE' | 'INACTIVE')}
                  className="w-full h-10 rounded-md border border-stone-800 bg-stone-950 px-3 text-xs text-stone-100 focus:outline-none focus:border-emerald-500"
                >
                  <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                  <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
                </select>
              </div>
            </div>

            <div className="mt-6 flex justify-end space-x-2 pt-4 border-t border-stone-800">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isLoading}
                className="border-stone-800 text-stone-300 hover:bg-stone-800"
              >
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={isLoading}
                className="bg-emerald-600 text-white hover:bg-emerald-500 min-w-[110px]"
              >
                {isLoading ? (
                  <span className="flex items-center space-x-2">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Đang lưu...</span>
                  </span>
                ) : (
                  'Tạo Pháp nhân'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
