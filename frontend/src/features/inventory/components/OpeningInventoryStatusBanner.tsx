import React from 'react'
import {
  Lock,
  CheckCircle2,
  AlertTriangle,
  Calendar,
  UserCheck,
  Package,
  Layers,
  Coins,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import type { BranchOpeningStatusDto } from '../types/opening.types'

interface OpeningInventoryStatusBannerProps {
  status: BranchOpeningStatusDto | null
  isLoading: boolean
  onOpenWizard?: () => void
}

function formatVnd(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(num)
}

function formatQuantity(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(num)
}

export const OpeningInventoryStatusBanner: React.FC<OpeningInventoryStatusBannerProps> = ({
  status,
  isLoading,
  onOpenWizard,
}) => {
  if (isLoading) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm animate-pulse">
        <div className="h-6 w-48 bg-slate-200 rounded mb-4" />
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="h-16 bg-slate-100 rounded-xl" />
          <div className="h-16 bg-slate-100 rounded-xl" />
          <div className="h-16 bg-slate-100 rounded-xl" />
          <div className="h-16 bg-slate-100 rounded-xl" />
        </div>
      </div>
    )
  }

  if (!status) return null

  const isCommitted = status.isOpeningCommitted
  const record = status.openingRecord

  if (isCommitted && record) {
    return (
      <div className="rounded-2xl border border-emerald-300 bg-gradient-to-r from-emerald-50/90 via-white to-slate-50 p-6 shadow-sm">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 pb-4 border-b border-emerald-100">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-800 text-white shadow-sm shrink-0">
              <Lock className="h-5 w-5" />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-900">
                  Tồn kho đầu kỳ đã được thiết lập & Khóa sổ
                </h3>
                <Badge className="bg-emerald-700 text-white hover:bg-emerald-800 text-xs gap-1">
                  <CheckCircle2 className="h-3 w-3" />
                  ĐÃ KHÓA SỔ MỞ KỲ
                </Badge>
              </div>
              <p className="text-xs text-slate-600 mt-0.5">
                Chi nhánh <strong>{status.branchName}</strong> đã hoàn tất mở sổ số dư ban đầu.
                Mọi nghiệp vụ tiếp theo sẽ căn cứ theo giá vốn bình quân Moving WAC.
              </p>
            </div>
          </div>

          <Badge variant="outline" className="border-emerald-600 text-emerald-900 bg-emerald-50/80 font-mono text-xs">
            POST-ONCE GUARD
          </Badge>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-4">
          <div className="bg-white/80 backdrop-blur-sm rounded-xl p-3.5 border border-emerald-200/60 shadow-xs">
            <div className="flex items-center gap-2 text-slate-500 text-xs mb-1">
              <Calendar className="h-3.5 w-3.5 text-emerald-700" />
              <span>Ngày mở sổ kinh doanh</span>
            </div>
            <div className="text-sm font-bold text-slate-900 font-mono">
              {record.openingDate}
            </div>
          </div>

          <div className="bg-white/80 backdrop-blur-sm rounded-xl p-3.5 border border-emerald-200/60 shadow-xs">
            <div className="flex items-center gap-2 text-slate-500 text-xs mb-1">
              <Package className="h-3.5 w-3.5 text-emerald-700" />
              <span>Tổng số mặt hàng</span>
            </div>
            <div className="text-sm font-bold text-slate-900">
              {record.totalItems} <span className="text-xs font-normal text-slate-500">mặt hàng</span>
            </div>
          </div>

          <div className="bg-white/80 backdrop-blur-sm rounded-xl p-3.5 border border-emerald-200/60 shadow-xs">
            <div className="flex items-center gap-2 text-slate-500 text-xs mb-1">
              <Layers className="h-3.5 w-3.5 text-emerald-700" />
              <span>Tổng lượng hàng tồn</span>
            </div>
            <div className="text-sm font-bold text-slate-900 font-mono">
              {formatQuantity(record.totalQuantity)}
            </div>
          </div>

          <div className="bg-white/80 backdrop-blur-sm rounded-xl p-3.5 border border-emerald-200/60 shadow-xs">
            <div className="flex items-center gap-2 text-slate-500 text-xs mb-1">
              <Coins className="h-3.5 w-3.5 text-emerald-700" />
              <span>Tổng giá trị vốn đầu kỳ</span>
            </div>
            <div className="text-sm font-bold text-emerald-800 font-mono">
              {formatVnd(record.totalValue)}
            </div>
          </div>
        </div>

        {/* Approval Traceability Audit Row */}
        <div className="mt-4 pt-3 border-t border-emerald-100 flex flex-col sm:flex-row items-start sm:items-center justify-between text-xs text-slate-500 gap-2">
          <div className="flex items-center gap-2">
            <UserCheck className="h-4 w-4 text-emerald-700" />
            <span>
              Người ký duyệt:{' '}
              <strong className="text-slate-800">
                {record.approvedByName || record.approvedBy}
              </strong>
            </span>
            <span className="text-slate-300">•</span>
            <span>
              Thời điểm duyệt:{' '}
              <span className="font-mono">{new Date(record.approvedAt).toLocaleString('vi-VN')}</span>
            </span>
          </div>

          {record.note && (
            <div className="text-slate-600 italic">
              Ghi chú: &ldquo;{record.note}&rdquo;
            </div>
          )}
        </div>
      </div>
    )
  }

  // Not committed yet state
  return (
    <div className="rounded-2xl border border-amber-300 bg-gradient-to-r from-amber-50/80 via-white to-slate-50 p-6 shadow-sm">
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-start gap-3.5">
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-600 text-white shadow-sm shrink-0 mt-0.5">
            <AlertTriangle className="h-6 w-6" />
          </span>
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <h3 className="text-base font-bold text-slate-900">
                Chi nhánh chưa thiết lập Tồn kho đầu kỳ (Opening Cutover)
              </h3>
              <Badge variant="outline" className="border-amber-500 text-amber-800 bg-amber-50 font-mono text-xs">
                CHƯA MỞ SỔ
              </Badge>
            </div>
            <p className="text-xs text-slate-600 max-w-2xl leading-relaxed">
              Chi nhánh <strong>{status.branchName}</strong> hiện chưa có số dư vật tư ban đầu và chưa gán đơn giá vốn
              bình quân gia quyền di động (Moving WAC). Vui lòng nhập danh mục kiểm kê thực tế để kích hoạt sổ kho.
            </p>
            <div className="flex items-center gap-2 pt-1 text-[11px] text-amber-800 font-medium">
              <ShieldAlert className="h-3.5 w-3.5 text-amber-600" />
              <span>Quy tắc: Mở sổ chỉ được thực hiện 01 lần duy nhất cho mỗi chi nhánh.</span>
            </div>
          </div>
        </div>

        {onOpenWizard && (
          <Button
            onClick={onOpenWizard}
            className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-sm shrink-0 px-4 py-2.5 h-auto rounded-xl"
          >
            <span>Khởi tạo tồn kho đầu kỳ</span>
            <ArrowRight className="h-4 w-4" />
          </Button>
        )}
      </div>
    </div>
  )
}
export default OpeningInventoryStatusBanner
