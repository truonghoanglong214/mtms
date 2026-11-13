import { NavLink } from 'react-router-dom'
import { motion, AnimatePresence } from 'motion/react'
import {
  LayoutDashboard,
  DollarSign,
  Users,
  Package,
  Layers,
  FileCheck2,
  PieChart,
  Building,
  X,
  ShieldCheck
} from 'lucide-react'
import { cn } from '@/shared/lib/utils'

interface SidebarProps {
  isMobileOpen: boolean
  onCloseMobile: () => void
}

const navItems = [
  { name: 'Tổng quan điều hành', path: '/', icon: LayoutDashboard },
  { name: 'Doanh thu ca & ngày', path: '/revenue', icon: DollarSign },
  { name: 'Chi phí & Giờ làm việc', path: '/workforce', icon: Users },
  { name: 'Quản lý Kho & WAC', path: '/inventory', icon: Package },
  { name: 'Sổ quỹ & Dòng tiền', path: '/finance', icon: Layers },
  { name: 'Chốt sổ kỳ kinh doanh', path: '/closing', icon: FileCheck2 },
  { name: 'Báo cáo P&L quản trị', path: '/reporting', icon: PieChart },
]

export function Sidebar({ isMobileOpen, onCloseMobile }: SidebarProps) {
  const NavContent = () => (
    <div className="flex flex-col h-full">
      {/* Module Title */}
      <div className="p-4 border-b border-border/70 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <Building className="h-4 w-4 text-primary" />
          <h2 className="text-xs font-bold uppercase text-foreground tracking-wider">
            Phân hệ Quản trị MTMS
          </h2>
        </div>
        <button
          type="button"
          onClick={onCloseMobile}
          className="lg:hidden p-1 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent"
          aria-label="Đóng menu"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Navigation Links */}
      <nav className="p-3 space-y-1.5 flex-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            onClick={onCloseMobile}
            className={({ isActive }) =>
              cn(
                'group flex items-center space-x-3 px-3 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150',
                isActive
                  ? 'bg-primary text-primary-foreground shadow-sm shadow-primary/20'
                  : 'text-muted-foreground hover:bg-accent hover:text-foreground'
              )
            }
          >
            <item.icon className="h-4 w-4 shrink-0 transition-transform group-hover:scale-105" />
            <span className="truncate">{item.name}</span>
          </NavLink>
        ))}
      </nav>

      {/* Footer Info & Guards */}
      <div className="p-4 border-t border-border/70 bg-muted/20 space-y-1.5 text-[11px] text-muted-foreground">
        <div className="flex items-center gap-1.5 font-semibold text-foreground">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
          <span>MTMS Foundation v1.1</span>
        </div>
        <p className="text-[10px] leading-tight">
          Hạch toán độc lập từng chi nhánh · Không POS bán lẻ.
        </p>
      </div>
    </div>
  )

  return (
    <>
      {/* Desktop Sidebar (Permanent) */}
      <aside className="hidden lg:flex w-64 border-r border-border bg-card flex-col shrink-0 min-h-[calc(100vh-4rem)]">
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
              className="fixed inset-0 bg-background/80 backdrop-blur-sm"
            />

            {/* Slide-out Panel */}
            <motion.div
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 250 }}
              className="fixed top-0 bottom-0 left-0 w-72 max-w-[85vw] bg-card border-r border-border shadow-2xl z-50 flex flex-col"
            >
              <NavContent />
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  )
}
