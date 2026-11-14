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
  ShieldCheck,
  CheckCircle2
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
    <div className="min-h-screen flex w-full bg-[#F7F3EC] text-[#352D26] selection:bg-[#B96849]/20 selection:text-[#B96849]">
      {/* ======================================================== */}
      {/* CỘT TRÁI (46% - 48%): Forest-green Brand Panel          */}
      {/* ======================================================== */}
      <div className="hidden lg:flex lg:w-[46%] xl:w-[44%] relative flex-col justify-between p-12 xl:p-16 bg-[#304638] text-[#F7F3EC] overflow-hidden border-r border-[#304638]/40">
        {/* Họa tiết vector lá trà & đường nét tối giản (Minimal tea graphics) */}
        <div className="absolute inset-0 pointer-events-none opacity-[0.07]">
          <svg className="w-full h-full" viewBox="0 0 400 400" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M0 200 C 100 100, 200 300, 400 200" stroke="#F7F3EC" strokeWidth="1.5" />
            <path d="M0 260 C 150 160, 250 360, 400 260" stroke="#F7F3EC" strokeWidth="1.5" />
            <path d="M50 0 C 150 150, 150 250, 50 400" stroke="#F7F3EC" strokeWidth="1.5" />
            <circle cx="320" cy="80" r="48" stroke="#F7F3EC" strokeWidth="1.5" />
          </svg>
        </div>

        {/* Top Header: Logo chính thức Lục Trà Tinh Hoa & Tên hệ thống */}
        <div className="relative z-10 flex items-center space-x-3.5">
          <img
            src="/assets/luc-tra-tinh-hoa/brand-logo-thumb.webp"
            alt="Logo Lục Trà Tinh Hoa"
            className="w-12 h-12 rounded-full object-cover ring-2 ring-[#819B78]/40 shadow-md bg-[#25372C]"
          />
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-lg tracking-tight text-white font-sans">
                Lục Trà Tinh Hoa
              </span>
              <span className="px-2 py-0.5 rounded text-[10px] font-mono tracking-wider uppercase bg-[#202E25] text-[#819B78] border border-[#819B78]/30">
                MTMS v1.1
              </span>
            </div>
            <span className="text-[11px] block text-[#C8CEC4] font-medium">
              Hệ thống Quản trị Vận hành & Dòng tiền Đa chi nhánh
            </span>
          </div>
        </div>

        {/* Center: Khối giới thiệu nghiệp vụ & Mạng lưới chi nhánh */}
        <div className="relative z-10 my-auto py-10 max-w-md space-y-6">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#202E25] border border-[#819B78]/30 text-[11px] font-medium text-[#819B78]">
              <ShieldCheck className="w-3.5 h-3.5 text-[#819B78] shrink-0" />
              <span>Kế toán Quản trị Nội bộ & An toàn Dữ liệu</span>
            </div>
            <h1 className="text-3xl xl:text-4xl font-bold tracking-tight text-white leading-tight font-sans">
              Hạch toán quản trị chuẩn mực, kiểm soát dòng tiền độc lập.
            </h1>
            <p className="text-sm text-[#C8CEC4] leading-relaxed font-normal">
              Nền tảng kiểm soát tài chính chuyên sâu dành cho Ban Quản trị và Quản lý cơ sở: hạch toán chi nhánh độc lập, đối soát doanh thu ứng dụng giao hàng và tính giá vốn bình quân di động (WAC).
            </p>
          </div>

          {/* Mạng lưới chi nhánh đang kết nối */}
          <div className="p-4 rounded-2xl bg-[#25372C]/90 border border-[#819B78]/25 shadow-lg backdrop-blur-sm space-y-3">
            <div className="flex items-center justify-between text-xs border-b border-[#819B78]/20 pb-2">
              <span className="font-semibold text-white flex items-center gap-2">
                <Building2 className="w-4 h-4 text-[#819B78]" />
                Mạng lưới Chi nhánh Lục Trà Tinh Hoa
              </span>
              <span className="text-[10px] font-mono text-[#819B78] bg-[#202E25] px-2 py-0.5 rounded border border-[#819B78]/30">
                4 Cơ sở Active
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2.5 text-[12px] text-[#E5DED4]">
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#819B78] shrink-0" />
                <span className="truncate">768 Nguyễn Văn Quá (Q12)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#819B78] shrink-0" />
                <span className="truncate">Nguyễn Gia Trí (Bình Thạnh)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#819B78] shrink-0" />
                <span className="truncate">Cao Thắng (Quận 10)</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#819B78] shrink-0" />
                <span className="truncate">Lê Văn Lương (Quận 7)</span>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom Footer: Ranh giới bất biến */}
        <div className="relative z-10 pt-4 border-t border-[#3E5747] flex items-center justify-between text-[11px] text-[#C8CEC4]">
          <span className="font-mono">
            Hạch toán chi nhánh độc lập · Doanh thu tổng hợp ngày
          </span>
          <span className="font-mono text-[#819B78] font-semibold px-2 py-0.5 rounded bg-[#202E25] border border-[#819B78]/30">
            NO POS
          </span>
        </div>
      </div>

      {/* ======================================================== */}
      {/* CỘT PHẢI (54% - 56%): Warm-white Login Surface           */}
      {/* ======================================================== */}
      <div className="flex-1 flex flex-col justify-between p-6 sm:p-12 lg:p-16 bg-[#FFFCF8]">
        {/* Mobile Header (Màn hình nhỏ < 1024px) */}
        <div className="lg:hidden flex items-center space-x-3 mb-8 pb-4 border-b border-[#E5DED4]">
          <img
            src="/assets/luc-tra-tinh-hoa/brand-logo-thumb.webp"
            alt="Logo Lục Trà Tinh Hoa"
            className="w-10 h-10 rounded-full object-cover ring-1 ring-[#819B78]/30"
          />
          <div>
            <span className="font-bold text-base tracking-tight text-[#352D26] block">
              Lục Trà Tinh Hoa
            </span>
            <span className="text-[11px] text-[#786C60] font-medium block">
              Hệ thống Quản trị MTMS
            </span>
          </div>
        </div>

        {/* Form Container */}
        <div className="w-full max-w-sm mx-auto my-auto space-y-6">
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="space-y-1.5 text-left"
          >
            <h2 className="text-2xl font-bold tracking-tight text-[#352D26]">
              Đăng nhập hệ thống
            </h2>
            <p className="text-xs text-[#786C60] leading-relaxed">
              Nhập tài khoản quản trị để truy cập phân hệ điều hành Lục Trà Tinh Hoa.
            </p>
          </motion.div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Alert Error Banner */}
            <AnimatePresence mode="wait">
              {errorMessage && (
                <motion.div
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  transition={{ duration: 0.15 }}
                  className="rounded-xl bg-red-50 border border-red-200 p-3 text-red-700 flex items-start gap-2.5 text-xs font-medium"
                >
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-600" />
                  <span className="leading-tight">{errorMessage}</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Login Identifier Field */}
            <div className="space-y-1.5">
              <label
                htmlFor="loginIdentifier"
                className="text-xs font-semibold text-[#352D26] block"
              >
                Tên đăng nhập hoặc Email
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-[#786C60] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  id="loginIdentifier"
                  name="loginIdentifier"
                  type="text"
                  autoComplete="username"
                  placeholder="Nhập tên đăng nhập hoặc email"
                  value={loginIdentifier}
                  onChange={(e) => setLoginIdentifier(e.target.value)}
                  disabled={isLoading}
                  className="pl-9 text-sm h-11 bg-white border-[#E5DED4] text-[#352D26] placeholder:text-[#786C60]/60 focus-visible:ring-1 focus-visible:ring-[#B96849] focus-visible:border-[#B96849] rounded-xl"
                  error={Boolean(errorMessage)}
                />
              </div>
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="password"
                  className="text-xs font-semibold text-[#352D26] block"
                >
                  Mật khẩu
                </label>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-[#786C60] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="••••••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={isLoading}
                  className="pl-9 pr-9 text-sm h-11 bg-white border-[#E5DED4] text-[#352D26] placeholder:text-[#786C60]/60 focus-visible:ring-1 focus-visible:ring-[#B96849] focus-visible:border-[#B96849] rounded-xl"
                  error={Boolean(errorMessage)}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#786C60] hover:text-[#352D26] transition-colors p-1 rounded focus:outline-none focus:ring-1 focus:ring-[#B96849]"
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

            {/* Submit Button với Terracotta Accent và vi hiệu ứng Motion */}
            <motion.div whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.985 }}>
              <Button
                type="submit"
                disabled={isLoading}
                className="w-full h-11 text-sm font-semibold tracking-wide bg-[#B96849] hover:bg-[#A55639] text-white shadow-sm transition-all focus-visible:ring-2 focus-visible:ring-[#B96849] rounded-xl cursor-pointer"
              >
                {isLoading ? (
                  <span className="flex items-center gap-2">
                    <span className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Đang xác thực...
                  </span>
                ) : (
                  'Đăng nhập vào hệ thống'
                )}
              </Button>
            </motion.div>
          </form>

          {/* Chính sách truy cập */}
          <div className="pt-2 text-center border-t border-[#E5DED4]/60">
            <p className="text-[11px] text-[#786C60] leading-relaxed">
              Hệ thống dành riêng cho tài khoản được cấp quyền bởi Ban Quản trị.
              <br />
              Mọi lượt truy cập đều được ghi vết bảo mật trong Audit Log.
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="w-full max-w-sm mx-auto text-center pt-8 text-[11px] text-[#786C60]">
          <span>Lục Trà Tinh Hoa · MTMS v1.1 © 2026</span>
        </div>
      </div>
    </div>
  )
}
