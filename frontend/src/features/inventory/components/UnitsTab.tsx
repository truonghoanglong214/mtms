import React, { useState, useMemo } from 'react'
import { motion } from 'motion/react'
import {
  Scale,
  Plus,
  Search,
  Filter,
  RefreshCw,
  AlertCircle,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { CreateUnitModal } from './CreateUnitModal'
import type { UnitDto, UnitDimension } from '../types/catalog.types'

interface UnitsTabProps {
  units: UnitDto[]
  isLoading: boolean
  error: string | null
  onRefresh: () => void
  onUnitCreated: (unit: UnitDto) => void
}

const DIMENSION_CONFIG: Record<
  UnitDimension,
  { label: string; className: string }
> = {
  MASS: {
    label: 'Khối lượng (MASS)',
    className: 'bg-emerald-50 text-emerald-800 border-emerald-200',
  },
  VOLUME: {
    label: 'Thể tích (VOLUME)',
    className: 'bg-blue-50 text-blue-800 border-blue-200',
  },
  COUNT: {
    label: 'Số đếm (COUNT)',
    className: 'bg-purple-50 text-purple-800 border-purple-200',
  },
  OTHER: {
    label: 'Khác (OTHER)',
    className: 'bg-slate-100 text-slate-700 border-slate-200',
  },
}

export const UnitsTab: React.FC<UnitsTabProps> = ({
  units,
  isLoading,
  error,
  onRefresh,
  onUnitCreated,
}) => {
  const [search, setSearch] = useState('')
  const [dimensionFilter, setDimensionFilter] = useState<string>('ALL')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  const filteredUnits = useMemo(() => {
    return units.filter((u) => {
      const matchSearch =
        search.trim() === '' ||
        u.unitCode.toLowerCase().includes(search.toLowerCase()) ||
        u.unitName.toLowerCase().includes(search.toLowerCase())

      const matchDimension = dimensionFilter === 'ALL' || u.dimension === dimensionFilter

      return matchSearch && matchDimension
    })
  }, [units, search, dimensionFilter])

  return (
    <div className="space-y-4">
      {/* Action / Filter Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm">
        <div className="flex flex-1 flex-wrap items-center gap-2.5">
          {/* Search */}
          <div className="relative min-w-[240px] max-w-sm flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã đơn vị, tên đơn vị..."
              className="pl-9 h-9 text-xs"
            />
          </div>

          {/* Dimension Filter */}
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-slate-400" />
            <select
              value={dimensionFilter}
              onChange={(e) => setDimensionFilter(e.target.value)}
              className="h-9 rounded-md border border-input bg-background px-2.5 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            >
              <option value="ALL">Tất cả chiều đo lường</option>
              <option value="MASS">Khối lượng (MASS)</option>
              <option value="VOLUME">Thể tích (VOLUME)</option>
              <option value="COUNT">Số đếm (COUNT)</option>
              <option value="OTHER">Khác (OTHER)</option>
            </select>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={onRefresh}
            disabled={isLoading}
            className="h-9 text-xs gap-1.5"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Tải lại</span>
          </Button>

          <Button
            size="sm"
            onClick={() => setIsCreateModalOpen(true)}
            className="h-9 bg-[#304638] hover:bg-[#25372C] text-white text-xs font-medium gap-1.5 shadow-sm"
          >
            <Plus className="h-4 w-4" />
            <span>Thêm đơn vị chuẩn</span>
          </Button>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-700 flex items-start gap-3">
          <AlertCircle className="h-4 w-4 mt-0.5 text-red-600 shrink-0" />
          <div>
            <p className="font-semibold">Lỗi tải danh mục đơn vị tính</p>
            <p className="mt-0.5">{error}</p>
          </div>
        </div>
      )}

      {/* Data Table */}
      <div className="rounded-xl border border-slate-200/80 bg-white shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50/75 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
              <tr>
                <th className="py-3 px-4">Mã đơn vị</th>
                <th className="py-3 px-4">Tên hiển thị</th>
                <th className="py-3 px-4">Chiều không gian đo lường</th>
                <th className="py-3 px-4 text-right">Phạm vi áp dụng</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading && units.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <RefreshCw className="h-6 w-6 animate-spin text-slate-400" />
                      <p>Đang tải đơn vị tính chuẩn...</p>
                    </div>
                  </td>
                </tr>
              ) : filteredUnits.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <Scale className="h-8 w-8 text-slate-300 stroke-[1.5]" />
                      <p className="text-sm font-medium text-slate-600">
                        Chưa có đơn vị tính nào phù hợp
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredUnits.map((unit) => {
                  const dimConfig =
                    DIMENSION_CONFIG[unit.dimension] || DIMENSION_CONFIG.OTHER

                  return (
                    <motion.tr
                      key={unit.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="hover:bg-slate-50/60 transition-colors"
                    >
                      <td className="py-3 px-4 font-mono font-bold text-slate-900">
                        <Badge variant="outline" className="font-mono text-xs">
                          {unit.unitCode}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 font-medium text-slate-800">
                        {unit.unitName}
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium border ${dimConfig.className}`}
                        >
                          {dimConfig.label}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-right text-slate-500 font-mono text-[11px]">
                        Toàn hệ thống (Canonical)
                      </td>
                    </motion.tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="px-4 py-2.5 bg-slate-50/70 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
          <span>
            Hiển thị <strong>{filteredUnits.length}</strong> / <strong>{units.length}</strong> đơn vị chuẩn
          </span>
          <span className="font-mono text-[11px] text-slate-400">
            Canonical Measurement Standards
          </span>
        </div>
      </div>

      {/* Create Modal */}
      <CreateUnitModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(created) => {
          onUnitCreated(created)
          setIsCreateModalOpen(false)
        }}
      />
    </div>
  )
}
