import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'motion/react'
import {
  Lock,
  User,
  AlertCircle,
  Eye,
  EyeOff,
  Building2,
  ShieldCheck
} from 'lucide-react'
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
    <div className="relative min-h-screen w-full flex flex-col justify-between overflow-hidden bg-stone-950 text-stone-100 selection:bg-emerald-600/30">
      {/* ======================================================== */}
      {/* 1. LỚP NỀN FULL-BLEED: ẢNH THỰC TẾ LỤC TRÀ TINH HOA      */}
      {/* ======================================================== */}
      <div className="absolute inset-0 z-0">
        <img
          src="/assets/luc-tra-tinh-hoa/store-counter-thumb.webp"
          alt="Không gian quầy pha chế Lục Trà Tinh Hoa"
          className="w-full h-full object-cover object-center filter brightness-[0.45] contrast-[1.08] scale-100 transition-transform duration-1000 ease-out hover:scale-[1.02]"
        />
        {/* Lớp phủ chuyển màu trung tính sẫm tối ưu tương phản chuẩn WCAG AA */}
        <div className="absolute inset-0 bg-gradient-to-t from-stone-950 via-stone-950/70 to-stone-950/60 backdrop-blur-[2px]" />
      </div>

      {/* ======================================================== */}
      {/* 2. TOP BAR: THƯƠNG HIỆU & MẠNG LƯỚI CHI NHÁNH            */}
      {/* ======================================================== */}
      <header className="relative z-10 w-full max-w-7xl mx-auto px-6 py-6 flex items-center justify-between">
        <div className="flex items-center space-x-3.5">
          <img
            src="/assets/luc-tra-tinh-hoa/brand-logo-thumb.webp"
            alt="Logo Lục Trà Tinh Hoa"
            className="w-11 h-11 rounded-full object-cover ring-2 ring-emerald-500/40 shadow-lg bg-stone-900"
          />
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-lg tracking-tight text-white">
                Lục Trà Tinh Hoa
              </span>
              <span className="px-2 py-0.5 rounded text-[10px] font-mono tracking-wider uppercase bg-emerald-950 text-emerald-400 border border-emerald-800/60">
                MTMS v1.1
              </span>
            </div>
            <span className="text-[11px] block text-stone-300 font-medium">
              Hệ thống Quản trị Vận hành & Dòng tiền Đa chi nhánh
            </span>
          </div>
        </div>

        {/* Mạng lưới chi nhánh (Ẩn trên mobile nhỏ) */}
        <div className="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-stone-900/80 border border-stone-800/80 text-xs text-stone-300 backdrop-blur-md">
          <Building2 className="w-3.5 h-3.5 text-emerald-400" />
          <span className="font-mono text-[11px] text-stone-300">
            Nguyễn Văn Quá · Nguyễn Gia Trí · Cao Thắng · Lê Văn Lương
          </span>
        </div>
      </header>

      {/* ======================================================== */}
      {/* 3. CENTER: FLOATING AUTHENTICATION CARD                 */}
      {/* ======================================================== */}
      <main className="relative z-10 w-full max-w-md mx-auto my-auto px-4 py-8">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, ease: 'easeOut' }}
          className="rounded-2xl bg-stone-900/90 border border-stone-800/90 p-8 shadow-2xl backdrop-blur-xl space-y-6"
        >
          {/* Card Header */}
          <div className="space-y-2 text-left">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-950/80 border border-emerald-800/60 text-[10px] font-mono text-emerald-300 uppercase tracking-wider">
              <ShieldCheck className="w-3 h-3 text-emerald-400" />
              <span>Kế toán Quản trị Nội bộ</span>
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-white">
              Đăng nhập hệ thống
            </h1>
            <p className="text-xs text-stone-400 leading-relaxed">
              Dành riêng cho Ban Quản trị và Quản lý cơ sở Lục Trà Tinh Hoa.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Alert Error Banner */}
            <AnimatePresence mode="wait">
              {errorMessage && (
                <motion.div
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  transition={{ duration: 0.15 }}
                  className="rounded-lg bg-red-950/80 border border-red-800/80 p-3 text-red-200 flex items-start gap-2.5 text-xs font-medium"
                >
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-400" />
                  <span className="leading-tight">{errorMessage}</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Login Identifier Field */}
            <div className="space-y-1.5">
              <label
                htmlFor="loginIdentifier"
                className="text-xs font-medium text-stone-300 block"
              >
                Tên đăng nhập hoặc Email
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-stone-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  id="loginIdentifier"
                  name="loginIdentifier"
                  type="text"
                  autoComplete="username"
                  placeholder="admin hoặc manager_q1"
                  value={loginIdentifier}
                  onChange={(e) => setLoginIdentifier(e.target.value)}
                  disabled={isLoading}
                  className="pl-9 text-sm h-10 bg-stone-950/80 border-stone-700/80 text-white placeholder:text-stone-500 focus-visible:ring-1 focus-visible:ring-emerald-500 focus-visible:border-emerald-500"
                  error={Boolean(errorMessage)}
                />
              </div>
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="password"
                  className="text-xs font-medium text-stone-300 block"
                >
                  Mật khẩu
                </label>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-stone-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="••••••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={isLoading}
                  className="pl-9 pr-9 text-sm h-10 bg-stone-950/80 border-stone-700/80 text-white placeholder:text-stone-500 focus-visible:ring-1 focus-visible:ring-emerald-500 focus-visible:border-emerald-500"
                  error={Boolean(errorMessage)}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-stone-400 hover:text-white transition-colors p-0.5 rounded focus:outline-none focus:ring-1 focus:ring-emerald-500"
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

            {/* Submit Button with Motion Micro-interaction */}
            <motion.div whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.985 }}>
              <Button
                type="submit"
                disabled={isLoading}
                className="w-full h-10 text-sm font-semibold tracking-wide bg-emerald-600 hover:bg-emerald-500 text-white shadow-md transition-all focus-visible:ring-2 focus-visible:ring-emerald-500 cursor-pointer"
              >
                {isLoading ? (
                  <span className="flex items-center gap-2">
                    <span className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Đang xác thực...
                  </span>
                ) : (
                  'Đăng nhập'
                )}
              </Button>
            </motion.div>
          </form>

          {/* Gợi ý tài khoản demo khi kiểm thử */}
          <div className="pt-2 text-center border-t border-stone-800">
            <p className="text-[11px] text-stone-400 leading-relaxed">
              Tài khoản mẫu: <span className="font-mono text-emerald-400">admin</span> / <span className="font-mono text-stone-300">admin123</span> (Tổng quản trị)
              <br />
              <span className="font-mono text-emerald-400">manager_q1</span> / <span className="font-mono text-stone-300">manager123</span> (Quản lý chi nhánh)
            </p>
          </div>
        </motion.div>
      </main>

      {/* ======================================================== */}
      {/* 4. FOOTER: RANH GIỚI BẤT BIẾN & BẢO MẬT                 */}
      {/* ======================================================== */}
      <footer className="relative z-10 w-full max-w-7xl mx-auto px-6 py-5 flex flex-col sm:flex-row items-center justify-between text-[11px] text-stone-400 gap-2 border-t border-stone-800/80">
        <div className="flex items-center gap-3">
          <span>Chuỗi Cửa Hàng Lục Trà Tinh Hoa · MTMS v1.1 © 2026</span>
          <span className="hidden sm:inline text-stone-600">|</span>
          <span className="font-mono text-stone-400">
            Hạch toán độc lập theo chi nhánh · Doanh thu tổng hợp ngày
          </span>
        </div>
        <span className="font-mono text-emerald-400 font-semibold px-2 py-0.5 rounded bg-stone-900 border border-stone-800">
          NO POS
        </span>
      </footer>
    </div>
  )
}
