import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import { X, UserPlus, AlertCircle, Loader2 } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { organizationApi } from '../organizationApi'
import type { UserDto } from '../types'

interface CreateUserModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: (user: UserDto) => void
}

export const CreateUserModal: React.FC<CreateUserModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [loginIdentifier, setLoginIdentifier] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (loginIdentifier.trim().length < 3) {
      setError('Tên đăng nhập phải có ít nhất 3 ký tự')
      return
    }
    if (displayName.trim().length < 2) {
      setError('Tên hiển thị phải có ít nhất 2 ký tự')
      return
    }
    if (password.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự')
      return
    }

    setIsLoading(true)
    try {
      const created = await organizationApi.createUser({
        loginIdentifier: loginIdentifier.trim().toLowerCase(),
        displayName: displayName.trim(),
        password,
      })
      onSuccess(created)
      onClose()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tạo tài khoản. Vui lòng kiểm tra lại thông tin.'
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
                <UserPlus className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-stone-100">Thêm Người dùng mới</h3>
                <p className="text-xs text-stone-400">Khởi tạo tài khoản đăng nhập vào hệ thống MTMS</p>
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
                Tên đăng nhập <span className="text-emerald-400 font-mono">*</span>
              </label>
              <Input
                value={loginIdentifier}
                onChange={(e) => setLoginIdentifier(e.target.value.toLowerCase())}
                placeholder="VD: manager_q1, finance_hcm, cashier_q12"
                className="font-mono bg-stone-950 border-stone-800 text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
                required
                maxLength={200}
              />
              <p className="mt-1 text-[11px] text-stone-400">Chỉ gồm chữ cái, số, dấu gạch dưới, gạch ngang và dấu chấm.</p>
            </div>

            <div>
              <label className="block text-xs font-medium text-stone-300 mb-1.5">
                Họ và tên hiển thị <span className="text-emerald-400 font-mono">*</span>
              </label>
              <Input
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="VD: Nguyễn Văn A (Quản lý Q1)"
                className="bg-stone-950 border-stone-800 text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
                required
                maxLength={160}
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-stone-300 mb-1.5">
                Mật khẩu khởi tạo <span className="text-emerald-400 font-mono">*</span>
              </label>
              <Input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Tối thiểu 8 ký tự"
                className="font-mono bg-stone-950 border-stone-800 text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
                required
                minLength={8}
                maxLength={120}
              />
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
                  'Tạo Người dùng'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
