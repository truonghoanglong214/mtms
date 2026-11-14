import React, { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'motion/react'
import {
  Coffee,
  Lock,
  User,
  AlertCircle,
  Eye,
  EyeOff,
  ShieldCheck,
  Building2,
  TrendingUp,
  PackageCheck
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
    <div className="min-h-screen flex w-full bg-background selection:bg-emerald-500/20">
      {/* ======================================================== */}
      {/* CỘT TRÁI (52%): Visual Brand & Operational Showcase     */}
      {/* ======================================================== */}
      <div className="hidden lg:flex lg:w-[52%] xl:w-[55%] relative flex-col justify-between p-12 xl:p-16 bg-slate-950 text-slate-100 overflow-hidden border-r border-slate-800/80">
        {/* Background ambient lighting */}
        <div className="absolute -top-32 -left-32 w-96 h-96 bg-emerald-600/15 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 bg-emerald-800/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute inset-0 bg-[radial-gradient(#1e293b_1px,transparent_1px)] [background-size:24px_24px] opacity-40 pointer-events-none" />

        {/* Top bar: Brand Logo & Tagline */}
        <div className="relative z-10 flex items-center space-x-3">
          <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/20 shadow-inner">
            <Coffee className="w-5 h-5" />
          </div>
          <div>
            <span className="font-bold text-lg tracking-tight text-white">
              MTMS Operations
            </span>
            <span className="text-[10px] font-mono block text-emerald-400 tracking-wider uppercase">
              Milk Tea Management System v1.1
            </span>
          </div>
        </div>

        {/* Center: Editorial Heading & Value Pillars */}
        <div className="relative z-10 my-auto py-12 max-w-xl space-y-8">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-950/80 border border-emerald-800/50 text-[11px] font-medium text-emerald-300">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
              <span>Kiểm soát tài chính & vận hành đa chi nhánh</span>
            </div>
            <h1 className="text-3xl xl:text-4xl font-extrabold tracking-tight text-white leading-tight">
              Chính xác từng con số. <br />
              <span className="text-emerald-400">Minh bạch từng cốc trà.</span>
            </h1>
            <p className="text-sm text-slate-400 leading-relaxed max-w-lg">
              Hệ thống kế toán quản trị chuyên sâu dành riêng cho chuỗi trà sữa: hạch toán chi nhánh độc lập, tính giá vốn bình quân di động (Moving WAC) và kiểm soát dòng tiền đa kênh.
            </p>
          </div>

          {/* 3 Pillar Cards */}
          <div className="grid grid-cols-1 gap-3.5 pt-2">
            <div className="flex items-start space-x-3.5 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-sm">
              <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 shrink-0 mt-0.5">
                <Building2 className="w-4 h-4" />
              </div>
              <div className="space-y-0.5">
                <h4 className="text-xs font-semibold text-slate-200">
                  Phân định pháp nhân & chi nhánh độc lập
                </h4>
                <p className="text-[11px] text-slate-400 leading-normal">
                  Mỗi chi nhánh sở hữu quỹ tiền mặt, tài khoản ngân hàng và nhân sự riêng biệt, ngăn chặn thất thoát chéo.
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-3.5 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-sm">
              <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 shrink-0 mt-0.5">
                <PackageCheck className="w-4 h-4" />
              </div>
              <div className="space-y-0.5">
                <h4 className="text-xs font-semibold text-slate-200">
                  Tồn kho Moving WAC & COGS chuẩn mực
                </h4>
                <p className="text-[11px] text-slate-400 leading-normal">
                  Tự động cập nhật giá vốn xuất kho theo từng đợt nhập nguyên vật liệu, phản ánh chính xác chi phí đồ uống.
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-3.5 p-3.5 rounded-xl bg-slate-900/60 border border-slate-800/80 backdrop-blur-sm">
              <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400 shrink-0 mt-0.5">
                <TrendingUp className="w-4 h-4" />
              </div>
              <div className="space-y-0.5">
                <h4 className="text-xs font-semibold text-slate-200">
                  Tách bạch doanh thu NET & tiền về ngân hàng
                </h4>
                <p className="text-[11px] text-slate-400 leading-normal">
                  Ghi nhận doanh thu app theo ngày bán hàng thực tế; đối soát độc lập với tiền chuyển khoản từ đơn vị vận chuyển.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Bottom footer: Policy Guards */}
        <div className="relative z-10 pt-6 border-t border-slate-900 flex items-center justify-between text-[11px] text-slate-400">
          <span className="flex items-center gap-1.5 font-medium">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            Bảo mật phiên làm việc bằng Dual JWT & Stateless Filter
          </span>
          <span className="font-mono text-slate-500">NO POS · STRICT SAFEGUARD</span>
        </div>
      </div>

      {/* ======================================================== */}
      {/* CỘT PHẢI (48%): Form Đăng nhập Tối giản & Tinh tế         */}
      {/* ======================================================== */}
      <div className="flex-1 flex flex-col justify-between p-6 sm:p-12 lg:p-16">
        {/* Mobile Header (hiển thị khi màn hình nhỏ) */}
        <div className="lg:hidden flex items-center space-x-2.5 mb-8">
          <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-emerald-600/10 text-emerald-600 ring-1 ring-emerald-600/20">
            <Coffee className="w-4.5 h-4.5" />
          </div>
          <div>
            <span className="font-bold text-base tracking-tight text-foreground">
              MTMS Operations
            </span>
            <span className="text-[10px] text-muted-foreground block">
              Quản trị Chuỗi Trà Sữa
            </span>
          </div>
        </div>

        {/* Center Container: Login Form */}
        <div className="w-full max-w-sm mx-auto my-auto space-y-7">
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, ease: 'easeOut' }}
            className="space-y-2 text-left"
          >
            <h2 className="text-2xl font-bold tracking-tight text-foreground">
              Đăng nhập hệ thống
            </h2>
            <p className="text-xs text-muted-foreground leading-relaxed">
              Nhập tên tài khoản quản trị để truy cập phân hệ điều hành MTMS.
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
                  className="rounded-lg bg-destructive/10 border border-destructive/20 p-3 text-destructive flex items-start gap-2.5 text-xs font-medium"
                >
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span className="leading-tight">{errorMessage}</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Login Identifier Field */}
            <div className="space-y-1.5">
              <label
                htmlFor="loginIdentifier"
                className="text-xs font-medium text-foreground block"
              >
                Tên đăng nhập hoặc Email
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
                  className="pl-9 text-sm h-10 bg-background border-input focus-visible:ring-1 focus-visible:ring-emerald-500 focus-visible:border-emerald-500"
                  error={Boolean(errorMessage)}
                />
              </div>
            </div>

            {/* Password Field */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="password"
                  className="text-xs font-medium text-foreground block"
                >
                  Mật khẩu
                </label>
              </div>
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
                  className="pl-9 pr-9 text-sm h-10 bg-background border-input focus-visible:ring-1 focus-visible:ring-emerald-500 focus-visible:border-emerald-500"
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
            <motion.div whileHover={{ scale: 1.005 }} whileTap={{ scale: 0.985 }}>
              <Button
                type="submit"
                disabled={isLoading}
                className="w-full h-10 text-sm font-semibold tracking-wide bg-emerald-600 hover:bg-emerald-500 text-white shadow-sm transition-all focus-visible:ring-2 focus-visible:ring-emerald-500"
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

          {/* Scope notice */}
          <div className="pt-4 text-center">
            <p className="text-[11px] text-muted-foreground/80 leading-relaxed">
              Dành riêng cho Quản trị viên & Quản lý cơ sở.
              <br />
              Mọi lượt truy cập đều được lưu vết trong hệ thống kiểm toán (Audit Log).
            </p>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="w-full max-w-sm mx-auto text-center pt-8 text-[11px] text-muted-foreground">
          <span>MTMS © 2026 · Hạch toán Quản trị Chuỗi Trà Sữa</span>
        </div>
      </div>
    </div>
  )
}
