import React, { useState, useMemo } from 'react'
import { motion } from 'motion/react'
import {
  Truck,
  Plus,
  Search,
  Filter,
  RefreshCw,
  AlertCircle,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { CreateSupplierModal } from './CreateSupplierModal'
import type { SupplierDto } from '../types/catalog.types'

interface SuppliersTabProps {
  entityId: string
  suppliers: SupplierDto[]
  isLoading: boolean
  error: string | null
  onRefresh: () => void
  onSupplierCreated: (supplier: SupplierDto) => void
}

export const SuppliersTab: React.FC<SuppliersTabProps> = ({
  entityId,
  suppliers,
  isLoading,
  error,
  onRefresh,
  onSupplierCreated,
}) => {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  const filteredSuppliers = useMemo(() => {
    return suppliers.filter((s) => {
      const matchSearch =
        search.trim() === '' ||
        s.supplierCode.toLowerCase().includes(search.toLowerCase()) ||
        s.name.toLowerCase().includes(search.toLowerCase()) ||
        (s.contactNote && s.contactNote.toLowerCase().includes(search.toLowerCase()))

      const matchStatus = statusFilter === 'ALL' || s.status === statusFilter

      return matchSearch && matchStatus
    })
  }, [suppliers, search, statusFilter])

  return (
    <div className="space-y-4">
      {/* Filter & Action Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm">
        <div className="flex flex-1 flex-wrap items-center gap-2.5">
          {/* Search */}
          <div className="relative min-w-[240px] max-w-sm flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã, tên NCC hoặc liên hệ..."
              className="pl-9 h-9 text-xs"
            />
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-slate-400" />
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="h-9 rounded-md border border-input bg-background px-2.5 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hợp tác</option>
              <option value="INACTIVE">Tạm dừng</option>
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
            <span>Thêm nhà cung cấp</span>
          </Button>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-700 flex items-start gap-3">
          <AlertCircle className="h-4 w-4 mt-0.5 text-red-600 shrink-0" />
          <div>
            <p className="font-semibold">Lỗi tải danh mục nhà cung cấp</p>
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
                <th className="py-3 px-4">Mã NCC</th>
                <th className="py-3 px-4">Tên nhà cung cấp</th>
                <th className="py-3 px-4">Thông tin liên hệ & Ghi chú</th>
                <th className="py-3 px-4 text-center">Trạng thái</th>
                <th className="py-3 px-4 text-right">Ngày tạo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading && suppliers.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <RefreshCw className="h-6 w-6 animate-spin text-slate-400" />
                      <p>Đang tải danh sách nhà cung cấp...</p>
                    </div>
                  </td>
                </tr>
              ) : filteredSuppliers.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <Truck className="h-8 w-8 text-slate-300 stroke-[1.5]" />
                      <p className="text-sm font-medium text-slate-600">
                        Chưa có nhà cung cấp nào
                      </p>
                      <p className="text-xs text-slate-400">
                        Nhấn &quot;Thêm nhà cung cấp&quot; để khai báo đối tác cung ứng cho pháp nhân.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredSuppliers.map((supplier) => (
                  <motion.tr
                    key={supplier.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="hover:bg-slate-50/60 transition-colors"
                  >
                    <td className="py-3 px-4 font-mono font-semibold text-slate-900">
                      {supplier.supplierCode}
                    </td>
                    <td className="py-3 px-4 font-medium text-slate-800">
                      {supplier.name}
                    </td>
                    <td className="py-3 px-4 text-slate-600 max-w-xs truncate">
                      {supplier.contactNote || '---'}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <Badge
                        variant={supplier.status === 'ACTIVE' ? 'default' : 'secondary'}
                        className="text-[11px]"
                      >
                        {supplier.status === 'ACTIVE' ? 'Đang hợp tác' : 'Tạm dừng'}
                      </Badge>
                    </td>
                    <td className="py-3 px-4 text-right text-slate-500 font-mono text-[11px]">
                      {supplier.createdAt ? supplier.createdAt.split('T')[0] : '---'}
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="px-4 py-2.5 bg-slate-50/70 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
          <span>
            Hiển thị <strong>{filteredSuppliers.length}</strong> / <strong>{suppliers.length}</strong> nhà cung cấp
          </span>
          <span className="font-mono text-[11px] text-slate-400">
            MTMS Suppliers Master
          </span>
        </div>
      </div>

      {/* Create Modal */}
      <CreateSupplierModal
        isOpen={isCreateModalOpen}
        entityId={entityId}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(created) => {
          onSupplierCreated(created)
          setIsCreateModalOpen(false)
        }}
      />
    </div>
  )
}
