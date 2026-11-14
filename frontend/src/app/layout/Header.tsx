import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ShieldAlert,
  Menu,
  LogOut,
  ChevronDown,
  Building2,
  Check
} from 'lucide-react'
import { Badge } from '@/shared/components/ui/badge'
import { useAuthStore } from '@/features/auth/authStore'
import { authApi } from '@/features/auth/authApi'
import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'

interface BranchSummary {
  id: string
  code: string
  name: string
  status: string
}

interface HeaderProps {
  onToggleMobileSidebar: () => void
}

export function Header({ onToggleMobileSidebar }: HeaderProps) {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const activeBranchName = useAuthStore((state) => state.activeBranchName)
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)
  const [isBranchMenuOpen, setIsBranchMenuOpen] = useState(false)
  const [branches, setBranches] = useState<BranchSummary[]>([])

  const userMenuRef = useRef<HTMLDivElement>(null)
  const branchMenuRef = useRef<HTMLDivElement>(null)

  // Fetch branches for switcher if user is logged in
  useEffect(() => {
    let isMounted = true
    apiClient
      .get<ApiResponse<BranchSummary[]>>('/organizations/branches')
      .then((res) => {
        if (isMounted && res.data?.data) {
          setBranches(res.data.data)
        }
      })
      .catch(() => {
        // Silently fallback if branches cannot be fetched yet
      })
    return () => {
      isMounted = false
    }
  }, [])

  // Close menus on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false)
      }
      if (branchMenuRef.current && !branchMenuRef.current.contains(event.target as Node)) {
        setIsBranchMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleLogout = async () => {
    await authApi.logout()
    logout()
    navigate('/login', { replace: true })
  }

  const handleSelectBranch = (branch: BranchSummary | null) => {
    if (branch) {
      setActiveBranch(branch.id, `${branch.code} - ${branch.name}`)
    } else {
      setActiveBranch(null, 'Tất cả chi nhánh')
    }
    setIsBranchMenuOpen(false)
  }

  return (
    <header className="h-[68px] border-b border-[#E5DED4] bg-[#FFFCF8]/95 backdrop-blur-sm flex items-center justify-between px-4 sm:px-6 sticky top-0 z-30 select-none">
      {/* Left side: Mobile Toggle + Brand Logo */}
      <div className="flex items-center space-x-3">
        <button
          type="button"
          onClick={onToggleMobileSidebar}
          className="lg:hidden p-2 rounded-lg text-[#786C60] hover:text-[#352D26] hover:bg-[#F7F3EC] focus:outline-none focus:ring-2 focus:ring-[#B96849]"
          aria-label="Mở menu điều hướng"
        >
          <Menu className="h-5 w-5" />
        </button>

        <div className="flex items-center space-x-2.5">
          <img
            src="/assets/luc-tra-tinh-hoa/brand-logo-thumb.webp"
            alt="Logo"
            className="w-8 h-8 rounded-full object-cover ring-1 ring-[#819B78]/40 shadow-sm"
          />
          <div className="hidden sm:block">
            <span className="font-bold text-base tracking-tight text-[#352D26]">
              Lục Trà Tinh Hoa
            </span>
          </div>
        </div>

        {/* Branch Selector Dropdown */}
        <div className="relative" ref={branchMenuRef}>
          <button
            type="button"
            onClick={() => setIsBranchMenuOpen(!isBranchMenuOpen)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-[#E5DED4] bg-white hover:bg-[#F7F3EC] text-xs font-semibold text-[#352D26] transition-colors shadow-2xs"
          >
            <Building2 className="h-3.5 w-3.5 text-[#786C60]" />
            <span className="max-w-[140px] sm:max-w-[220px] truncate">
              {activeBranchName || 'Tất cả chi nhánh'}
            </span>
            <ChevronDown className="h-3 w-3 text-[#786C60] ml-0.5" />
          </button>

          {isBranchMenuOpen && (
            <div className="absolute left-0 mt-1.5 w-64 rounded-2xl border border-[#E5DED4] bg-[#FFFCF8] p-1.5 shadow-xl z-50 text-xs">
              <div className="px-3 py-2 font-semibold text-[#786C60] border-b border-[#E5DED4] mb-1">
                Chọn chi nhánh làm việc
              </div>
              <button
                type="button"
                onClick={() => handleSelectBranch(null)}
                className="w-full flex items-center justify-between px-3 py-2 rounded-xl hover:bg-[#F7F3EC] text-left font-medium transition-colors text-[#352D26]"
              >
                <span>Tất cả chi nhánh (Consolidated)</span>
                {activeBranchId === null && (
                  <Check className="h-3.5 w-3.5 text-[#B96849]" />
                )}
              </button>

              {branches.map((b) => (
                <button
                  key={b.id}
                  type="button"
                  onClick={() => handleSelectBranch(b)}
                  className="w-full flex items-center justify-between px-3 py-2 rounded-xl hover:bg-[#F7F3EC] text-left font-medium transition-colors text-[#352D26]"
                >
                  <span className="truncate">{b.code} - {b.name}</span>
                  {activeBranchId === b.id && (
                    <Check className="h-3.5 w-3.5 text-[#B96849]" />
                  )}
                </button>
              ))}

              {branches.length === 0 && (
                <div className="px-3 py-2 text-[#786C60] text-[11px] italic">
                  Chưa có chi nhánh nào được kích hoạt
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Right side: Provisional Badge + User Profile Dropdown */}
      <div className="flex items-center space-x-3 sm:space-x-4">
        {/* Provisional Guard Badge */}
        <Badge
          variant="provisional"
          className="flex items-center gap-1.5 py-1 px-3 text-[11px] font-semibold tracking-wide rounded-full"
        >
          <ShieldAlert className="h-3.5 w-3.5 shrink-0" />
          <span>TẠM TÍNH / PROVISIONAL</span>
        </Badge>

        {/* User Profile Menu */}
        <div className="relative" ref={userMenuRef}>
          <button
            type="button"
            onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
            className="flex items-center space-x-2 p-1.5 rounded-xl hover:bg-[#F7F3EC] transition-colors focus:outline-none focus:ring-2 focus:ring-[#B96849]"
            aria-label="Menu tài khoản"
          >
            <div className="w-8 h-8 rounded-full bg-[#304638] text-white font-bold text-xs flex items-center justify-center ring-1 ring-[#819B78]/40 shadow-xs">
              {user?.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="hidden md:flex flex-col text-left">
              <span className="text-xs font-semibold text-[#352D26] leading-tight">
                {user?.displayName || 'Người dùng'}
              </span>
              <span className="text-[10px] text-[#786C60]">
                @{user?.loginIdentifier || 'account'}
              </span>
            </div>
            <ChevronDown className="h-3 w-3 text-[#786C60] hidden md:block" />
          </button>

          {isUserMenuOpen && (
            <div className="absolute right-0 mt-1.5 w-60 rounded-2xl border border-[#E5DED4] bg-[#FFFCF8] p-2 shadow-xl z-50 text-xs">
              <div className="px-3 py-2 border-b border-[#E5DED4] mb-1">
                <p className="font-semibold text-[#352D26]">{user?.displayName}</p>
                <p className="text-[11px] text-[#786C60] truncate">
                  @{user?.loginIdentifier}
                </p>
                <div className="mt-1.5">
                  <Badge variant="outline" className="text-[10px] py-0 px-2 rounded-full border-[#E5DED4] text-[#786C60]">
                    Trạng thái: {user?.status || 'ACTIVE'}
                  </Badge>
                </div>
              </div>

              <button
                type="button"
                onClick={handleLogout}
                className="w-full flex items-center space-x-2 px-3 py-2 text-[#B7423E] hover:bg-red-50 rounded-xl font-medium transition-colors"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span>Đăng xuất phiên làm việc</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
