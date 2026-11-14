import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import { motion, AnimatePresence } from 'motion/react'
import {
  LayoutDashboard,
  Receipt,
  Users,
  Package,
  Wallet,
  FileCheck2,
  PieChart,
  X,
  ShieldCheck,
  ChevronLeft,
  ChevronRight
} from 'lucide-react'
import { cn } from '@/shared/lib/utils'

interface SidebarProps {
  isMobileOpen: boolean
  onCloseMobile: () => void
}

const navItems = [
  { name: 'Tổng quan điều hành', path: '/', icon: LayoutDashboard },
  { name: 'Doanh thu ca & ngày', path: '/revenue', icon: Receipt },
  { name: 'Chi phí & Giờ làm việc', path: '/workforce', icon: Users },
  { name: 'Quản lý Kho & WAC', path: '/inventory', icon: Package },
  { name: 'Sổ quỹ & Dòng tiền', path: '/finance', icon: Wallet },
  { name: 'Chốt sổ kỳ kinh doanh', path: '/closing', icon: FileCheck2 },
  { name: 'Báo cáo P&L quản trị', path: '/reporting', icon: PieChart },
]

export function Sidebar({ isMobileOpen, onCloseMobile }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState(false)

  const NavContent = ({ mobile = false }: { mobile?: boolean }) => {
    const collapsed = !mobile && isCollapsed

    return (
      <div className="flex flex-col h-full bg-[#304638] text-[#F7F3EC] select-none">
        {/* Module Title / Header */}
        <div className="h-[68px] px-4 border-b border-[#3E5747] flex items-center justify-between shrink-0">
          <div className={cn('flex items-center space-x-3 overflow-hidden', collapsed && 'justify-center w-full')}>
            <img
              src="/assets/luc-tra-tinh-hoa/brand-logo-thumb.webp"
              alt="Logo"
              className="w-9 h-9 rounded-full object-cover ring-1 ring-[#819B78]/40 shrink-0 bg-[#25372C]"
            />
            {!collapsed && (
              <div className="truncate">
                <span className="font-bold text-sm tracking-tight text-white block truncate">
                  Lục Trà Tinh Hoa
                </span>
                <span className="text-[10px] font-mono text-[#819B78] uppercase tracking-wider block">
                  MTMS Operations
                </span>
              </div>
            )}
          </div>
          {mobile && (
            <button
              type="button"
              onClick={onCloseMobile}
              className="p-1.5 rounded-lg text-[#C8CEC4] hover:text-white hover:bg-[#3E5747] transition-colors"
              aria-label="Đóng menu"
            >
              <X className="h-5 w-5" />
            </button>
          )}
        </div>

        {/* Navigation Links */}
        <nav className="p-3 space-y-1.5 flex-1 overflow-y-auto">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={mobile ? onCloseMobile : undefined}
              title={collapsed ? item.name : undefined}
              className={({ isActive }) =>
                cn(
                  'group flex items-center space-x-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all duration-150',
                  collapsed && 'justify-center px-2',
                  isActive
                    ? 'bg-[#B96849] text-white shadow-sm font-semibold'
                    : 'text-[#C8CEC4] hover:bg-[#3E5747] hover:text-white'
                )
              }
            >
              <item.icon className="h-4.5 w-4.5 shrink-0 transition-transform group-hover:scale-105" />
              {!collapsed && <span className="truncate">{item.name}</span>}
            </NavLink>
          ))}
        </nav>

        {/* Footer Info & Collapse Toggle */}
        <div className="p-3 border-t border-[#3E5747] bg-[#25372C]/60 space-y-2 shrink-0">
          {!collapsed && (
            <div className="px-2 pt-1 space-y-1 text-[11px] text-[#C8CEC4]">
              <div className="flex items-center gap-1.5 font-semibold text-white">
                <ShieldCheck className="w-3.5 h-3.5 text-[#819B78]" />
                <span>MTMS Foundation v1.1</span>
              </div>
              <p className="text-[10px] text-[#C8CEC4]/80 leading-tight">
                Hạch toán chi nhánh độc lập · Không POS.
              </p>
            </div>
          )}

          {!mobile && (
            <button
              type="button"
              onClick={() => setIsCollapsed(!isCollapsed)}
              className={cn(
                'w-full flex items-center gap-2 p-2 rounded-xl text-[#C8CEC4] hover:text-white hover:bg-[#3E5747] text-xs font-medium transition-colors',
                collapsed ? 'justify-center' : 'justify-between px-3'
              )}
              aria-label={isCollapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'}
            >
              {!collapsed && <span>Thu gọn thanh menu</span>}
              {isCollapsed ? (
                <ChevronRight className="h-4 w-4" />
              ) : (
                <ChevronLeft className="h-4 w-4" />
              )}
            </button>
          )}
        </div>
      </div>
    )
  }

  return (
    <>
      {/* Desktop Sidebar (Permanent) */}
      <aside
        className={cn(
          'hidden lg:flex border-r border-[#3E5747] flex-col shrink-0 min-h-[calc(100vh-68px)] transition-all duration-200 ease-out z-20',
          isCollapsed ? 'w-[72px]' : 'w-[248px]'
        )}
      >
        <NavContent />
      </aside>

      {/* Mobile Drawer (Slide-over with Backdrop) */}
      <AnimatePresence>
        {isMobileOpen && (
          <div className="fixed inset-0 z-50 lg:hidden">
            {/* Backdrop */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={onCloseMobile}
              className="fixed inset-0 bg-black/50 backdrop-blur-sm"
            />

            {/* Slide-out Panel */}
            <motion.div
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 250 }}
              className="fixed top-0 bottom-0 left-0 w-72 max-w-[85vw] bg-[#304638] border-r border-[#3E5747] shadow-2xl z-50 flex flex-col"
            >
              <NavContent mobile />
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  )
}
