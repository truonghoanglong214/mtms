import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Coffee,
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
    <header className="h-16 border-b bg-card/95 backdrop-blur-sm flex items-center justify-between px-4 sm:px-6 sticky top-0 z-20">
      {/* Left side: Mobile Toggle + Brand Logo */}
      <div className="flex items-center space-x-3">
        <button
          type="button"
          onClick={onToggleMobileSidebar}
          className="lg:hidden p-2 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent focus:outline-none focus:ring-2 focus:ring-ring"
          aria-label="Mở menu điều hướng"
        >
          <Menu className="h-5 w-5" />
        </button>

        <div className="flex items-center space-x-2.5">
          <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 ring-1 ring-amber-500/20">
            <Coffee className="h-4.5 w-4.5" />
          </div>
          <div className="hidden sm:block">
            <span className="font-bold text-base tracking-tight text-foreground">
              MTMS Operations
            </span>
          </div>
        </div>

        {/* Branch Selector Dropdown */}
        <div className="relative" ref={branchMenuRef}>
          <button
            type="button"
            onClick={() => setIsBranchMenuOpen(!isBranchMenuOpen)}
            className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg border border-border bg-background hover:bg-accent text-xs font-medium text-foreground transition-colors"
          >
            <Building2 className="h-3.5 w-3.5 text-muted-foreground" />
            <span className="max-w-[140px] sm:max-w-[200px] truncate">
              {activeBranchName || 'Tất cả chi nhánh'}
            </span>
            <ChevronDown className="h-3 w-3 text-muted-foreground ml-0.5" />
          </button>

          {isBranchMenuOpen && (
            <div className="absolute left-0 mt-1.5 w-60 rounded-xl border border-border bg-popover p-1.5 shadow-lg z-50 text-xs">
              <div className="px-2 py-1.5 font-semibold text-muted-foreground border-b border-border/60 mb-1">
                Chọn chi nhánh làm việc
              </div>
              <button
                type="button"
                onClick={() => handleSelectBranch(null)}
                className="w-full flex items-center justify-between px-2.5 py-2 rounded-lg hover:bg-accent text-left font-medium transition-colors"
              >
                <span>Tất cả chi nhánh (Consolidated)</span>
                {activeBranchId === null && (
                  <Check className="h-3.5 w-3.5 text-primary" />
                )}
              </button>

              {branches.map((b) => (
                <button
                  key={b.id}
                  type="button"
                  onClick={() => handleSelectBranch(b)}
                  className="w-full flex items-center justify-between px-2.5 py-2 rounded-lg hover:bg-accent text-left font-medium transition-colors"
                >
                  <span className="truncate">{b.code} - {b.name}</span>
                  {activeBranchId === b.id && (
                    <Check className="h-3.5 w-3.5 text-primary" />
                  )}
                </button>
              ))}

              {branches.length === 0 && (
                <div className="px-2.5 py-2 text-muted-foreground text-[11px] italic">
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
          className="flex items-center gap-1.5 py-1 px-2.5 text-[11px] font-semibold tracking-wide"
        >
          <ShieldAlert className="h-3.5 w-3.5 shrink-0" />
          <span>TẠM TÍNH / PROVISIONAL</span>
        </Badge>

        {/* User Profile Menu */}
        <div className="relative" ref={userMenuRef}>
          <button
            type="button"
            onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
            className="flex items-center space-x-2 p-1.5 rounded-lg hover:bg-accent transition-colors focus:outline-none focus:ring-2 focus:ring-ring"
            aria-label="Menu tài khoản"
          >
            <div className="w-8 h-8 rounded-full bg-primary/10 text-primary font-bold text-xs flex items-center justify-center ring-1 ring-primary/20">
              {user?.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="hidden md:flex flex-col text-left">
              <span className="text-xs font-semibold text-foreground leading-tight">
                {user?.displayName || 'Người dùng'}
              </span>
              <span className="text-[10px] text-muted-foreground">
                @{user?.loginIdentifier || 'account'}
              </span>
            </div>
            <ChevronDown className="h-3 w-3 text-muted-foreground hidden md:block" />
          </button>

          {isUserMenuOpen && (
            <div className="absolute right-0 mt-1.5 w-56 rounded-xl border border-border bg-popover p-1.5 shadow-lg z-50 text-xs">
              <div className="px-3 py-2 border-b border-border/60 mb-1">
                <p className="font-semibold text-foreground">{user?.displayName}</p>
                <p className="text-[11px] text-muted-foreground truncate">
                  @{user?.loginIdentifier}
                </p>
                <div className="mt-1">
                  <Badge variant="outline" className="text-[10px] py-0 px-1.5">
                    Trạng thái: {user?.status || 'ACTIVE'}
                  </Badge>
                </div>
              </div>

              <button
                type="button"
                onClick={handleLogout}
                className="w-full flex items-center space-x-2 px-3 py-2 text-destructive hover:bg-destructive/10 rounded-lg font-medium transition-colors"
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
