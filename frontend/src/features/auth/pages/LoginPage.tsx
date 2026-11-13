import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'motion/react'
import { Coffee, Lock, User, AlertCircle, Eye, EyeOff, ShieldCheck } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { authApi } from '../authApi'
import { useAuthStore } from '../authStore'
import type { ApiProblemDetail } from '../types'
import axios from 'axios'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useAuthStore((state) => state.setAuth)

  const [loginIdentifier, setLoginIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/'

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!loginIdentifier.trim()) {
      setErrorMessage('Vui lòng nhập tên đăng nhập hoặc email')
      return
    }

    if (!password) {
      setErrorMessage('Vui lòng nhập mật khẩu')
      return
    }

    setIsLoading(true)
    setErrorMessage(null)

    try {
      const response = await authApi.login({
        loginIdentifier: loginIdentifier.trim(),
        password,
      })

      setAuth(response)
      navigate(from, { replace: true })
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const problem = err.response?.data as ApiProblemDetail | undefined
        if (err.response?.status === 401) {
          setErrorMessage(problem?.detail || 'Tên đăng nhập hoặc mật khẩu không chính xác')
        } else if (err.response?.status === 403) {
          setErrorMessage(problem?.detail || 'Tài khoản của bạn đã bị tạm khóa. Vui lòng liên hệ quản trị viên.')
        } else if (err.response?.status === 400 && problem?.fieldErrors) {
          const firstField = Object.values(problem.fieldErrors)[0]
          setErrorMessage(firstField || 'Dữ liệu đăng nhập không hợp lệ')
        } else if (err.code === 'ERR_NETWORK') {
          setErrorMessage('Không thể kết nối đến máy chủ backend (Port 8080). Vui lòng kiểm tra dịch vụ.')
        } else {
          setErrorMessage(problem?.detail || 'Đã có lỗi xảy ra trong quá trình đăng nhập')
        }
      } else {
        setErrorMessage('Lỗi không xác định. Vui lòng thử lại.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-b from-background via-muted/30 to-background p-4 sm:p-6 lg:p-8">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, ease: 'easeOut' }}
        className="w-full max-w-md"
      >
        {/* Brand Card */}
        <div className="bg-card border border-border shadow-lg rounded-2xl p-8 backdrop-blur-sm relative overflow-hidden">
          {/* Subtle top brand bar */}
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-amber-600 via-emerald-600 to-amber-700" />

          {/* Header & Logo */}
          <div className="text-center space-y-2 mb-8">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400 mb-2 ring-1 ring-amber-500/20">
              <Coffee className="w-6 h-6" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              MTMS Operations
            </h1>
            <p className="text-xs text-muted-foreground max-w-xs mx-auto">
              Hệ thống Quản trị Vận hành & Tài chính Chuỗi Trà Sữa
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Error Banner */}
            <AnimatePresence mode="wait">
              {errorMessage && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.2 }}
                  className="rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-destructive flex items-start gap-2.5 text-xs font-medium"
                >
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span className="leading-tight">{errorMessage}</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Login Identifier */}
            <div className="space-y-1.5">
              <label
                htmlFor="loginIdentifier"
                className="text-xs font-semibold text-foreground flex items-center justify-between"
              >
                <span>Tên đăng nhập hoặc Email</span>
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-muted-foreground absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  id="loginIdentifier"
                  name="loginIdentifier"
                  type="text"
                  autoComplete="username"
                  placeholder="admin"
                  value={loginIdentifier}
                  onChange={(e) => setLoginIdentifier(e.target.value)}
                  disabled={isLoading}
                  className="pl-9 text-sm"
                  error={Boolean(errorMessage)}
                />
              </div>
            </div>

            {/* Password */}
            <div className="space-y-1.5">
              <label
                htmlFor="password"
                className="text-xs font-semibold text-foreground flex items-center justify-between"
              >
                <span>Mật khẩu</span>
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-muted-foreground absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="••••••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={isLoading}
                  className="pl-9 pr-9 text-sm"
                  error={Boolean(errorMessage)}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors p-0.5 rounded focus:outline-none focus:ring-1 focus:ring-ring"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <EyeOff className="w-4 h-4" />
                  ) : (
                    <Eye className="w-4 h-4" />
                  )}
                </button>
              </div>
            </div>

            {/* Submit Button */}
            <motion.div whileHover={{ scale: 1.005 }} whileTap={{ scale: 0.99 }}>
              <Button
                type="submit"
                disabled={isLoading}
                className="w-full h-11 text-sm font-semibold tracking-wide bg-primary hover:bg-primary/95 text-primary-foreground shadow-sm transition-all"
              >
                {isLoading ? (
                  <span className="flex items-center gap-2">
                    <span className="h-4 w-4 border-2 border-primary-foreground border-t-transparent rounded-full animate-spin" />
                    Đang xác thực...
                  </span>
                ) : (
                  'Đăng nhập vào hệ thống'
                )}
              </Button>
            </motion.div>
          </form>

          {/* System Policy & Boundaries */}
          <div className="mt-8 pt-6 border-t border-border/60 text-center space-y-2">
            <div className="inline-flex items-center gap-1.5 text-[11px] font-medium text-muted-foreground">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
              <span>Chính sách kiểm soát phân quyền đa chi nhánh</span>
            </div>
            <p className="text-[10px] text-muted-foreground/80 leading-relaxed">
              Dành riêng cho Quản trị viên & Chủ cơ sở kinh doanh.
              <br />
              Hệ thống không phục vụ thu ngân hoặc order tại quầy (NO POS).
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  )
}
