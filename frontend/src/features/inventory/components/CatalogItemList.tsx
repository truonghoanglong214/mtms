import React, { useState, useMemo } from 'react'
import { motion } from 'motion/react'
import {
  Search,
  Plus,
  Filter,
  AlertCircle,
  Package,
  Layers,
  Edit,
  RefreshCw,
  Calendar,
  Barcode,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { CreateItemModal } from './CreateItemModal'
import { EditItemModal } from './EditItemModal'
import type {
  CatalogItemDto,
  ItemKind,
  ItemStatus,
  UnitDto,
} from '../types/catalog.types'

interface CatalogItemListProps {
  entityId: string
  items: CatalogItemDto[]
  units: UnitDto[]
  isLoading: boolean
  error: string | null
  onRefresh: () => void
  onItemCreated: (newItem: CatalogItemDto) => void
  onItemUpdated: (updatedItem: CatalogItemDto) => void
  onSelectForConversions?: (item: CatalogItemDto) => void
}

const KIND_BADGE_MAP: Record<ItemKind, { label: string; className: string }> = {
  INGREDIENT: {
    label: 'Nguyên vật liệu',
    className: 'bg-emerald-50 text-emerald-800 border-emerald-200',
  },
  CONSUMABLE: {
    label: 'Vật tư tiêu hao',
    className: 'bg-blue-50 text-blue-800 border-blue-200',
  },
  EQUIPMENT: {
    label: 'Thiết bị & CCDC',
    className: 'bg-amber-50 text-amber-800 border-amber-200',
  },
  OTHER: {
    label: 'Khác',
    className: 'bg-slate-100 text-slate-700 border-slate-200',
  },
}

const STATUS_BADGE_MAP: Record<ItemStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
  ACTIVE: { label: 'Đang dùng', variant: 'default' },
  INACTIVE: { label: 'Tạm ngưng', variant: 'secondary' },
  ARCHIVED: { label: 'Lưu trữ', variant: 'outline' },
}

export const CatalogItemList: React.FC<CatalogItemListProps> = ({
  entityId,
  items,
  units,
  isLoading,
  error,
  onRefresh,
  onItemCreated,
  onItemUpdated,
  onSelectForConversions,
}) => {
  const [search, setSearch] = useState('')
  const [kindFilter, setKindFilter] = useState<string>('ALL')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<CatalogItemDto | null>(null)

  // Filter items locally or via params
  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      const matchSearch =
        search.trim() === '' ||
        item.itemCode.toLowerCase().includes(search.toLowerCase()) ||
        item.name.toLowerCase().includes(search.toLowerCase())

      const matchKind = kindFilter === 'ALL' || item.itemKind === kindFilter
      const matchStatus = statusFilter === 'ALL' || item.status === statusFilter

      return matchSearch && matchKind && matchStatus
    })
  }, [items, search, kindFilter, statusFilter])

  return (
    <div className="space-y-4">
      {/* Action / Filter Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm">
        {/* Search & Select Filters */}
        <div className="flex flex-1 flex-wrap items-center gap-2.5">
          <div className="relative min-w-[220px] max-w-sm flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã hàng hoặc tên..."
              className="pl-9 h-9 text-xs"
            />
          </div>

          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-slate-400" />
            <select
              value={kindFilter}
              onChange={(e) => setKindFilter(e.target.value)}
              className="h-9 rounded-md border border-input bg-background px-2.5 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            >
              <option value="ALL">Tất cả phân loại</option>
              <option value="INGREDIENT">Nguyên vật liệu</option>
              <option value="CONSUMABLE">Vật tư tiêu hao</option>
              <option value="EQUIPMENT">Thiết bị & Dụng cụ</option>
              <option value="OTHER">Khác</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="h-9 rounded-md border border-input bg-background px-2.5 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Tạm ngưng</option>
              <option value="ARCHIVED">Lưu trữ</option>
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
            <span>Thêm mặt hàng</span>
          </Button>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-700 flex items-start gap-3">
          <AlertCircle className="h-4 w-4 mt-0.5 text-red-600 shrink-0" />
          <div>
            <p className="font-semibold">Lỗi tải danh mục mặt hàng</p>
            <p className="mt-0.5">{error}</p>
          </div>
        </div>
      )}

      {/* Table Card */}
      <div className="rounded-xl border border-slate-200/80 bg-white shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50/75 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
              <tr>
                <th className="py-3 px-4">Mã hàng</th>
                <th className="py-3 px-4">Tên mặt hàng</th>
                <th className="py-3 px-4">Phân loại</th>
                <th className="py-3 px-4">Đơn vị gốc</th>
                <th className="py-3 px-4 text-center">Kiểm soát Lô & HSD</th>
                <th className="py-3 px-4 text-center">Quy cách NCC</th>
                <th className="py-3 px-4 text-center">Trạng thái</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading && items.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <RefreshCw className="h-6 w-6 animate-spin text-slate-400" />
                      <p>Đang tải danh mục mặt hàng...</p>
                    </div>
                  </td>
                </tr>
              ) : filteredItems.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <Package className="h-8 w-8 text-slate-300 stroke-[1.5]" />
                      <p className="text-sm font-medium text-slate-600">
                        Không có mặt hàng nào phù hợp bộ lọc
                      </p>
                      <p className="text-xs text-slate-400">
                        Bấm &quot;Thêm mặt hàng&quot; để khai báo nguyên vật liệu hoặc vật tư mới.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredItems.map((item) => {
                  const kindInfo = KIND_BADGE_MAP[item.itemKind] || KIND_BADGE_MAP.OTHER
                  const statusInfo = STATUS_BADGE_MAP[item.status] || STATUS_BADGE_MAP.ACTIVE

                  return (
                    <motion.tr
                      key={item.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="hover:bg-slate-50/60 transition-colors"
                    >
                      {/* Code */}
                      <td className="py-3 px-4 font-mono font-semibold text-slate-800">
                        {item.itemCode}
                      </td>

                      {/* Name */}
                      <td className="py-3 px-4">
                        <div className="font-medium text-slate-900">{item.name}</div>
                      </td>

                      {/* Kind */}
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-medium border ${kindInfo.className}`}
                        >
                          {kindInfo.label}
                        </span>
                      </td>

                      {/* Base Unit */}
                      <td className="py-3 px-4">
                        <Badge variant="secondary" className="font-mono text-xs">
                          {item.baseUnitName} ({item.baseUnitCode})
                        </Badge>
                      </td>

                      {/* Lot & Expiration Invariant Badges */}
                      <td className="py-3 px-4 text-center">
                        <div className="inline-flex items-center gap-1.5">
                          {item.lotTrackingRequired ? (
                            <span
                              title="Bắt buộc Số Lô"
                              className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200"
                            >
                              <Barcode className="h-3 w-3" />
                              Lô
                            </span>
                          ) : (
                            <span
                              title="Không theo dõi số lô"
                              className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] text-slate-400 bg-slate-50 border border-slate-200"
                            >
                              Không Lô
                            </span>
                          )}

                          {item.expirationTrackingRequired ? (
                            <span
                              title="Bắt buộc Hạn Sử Dụng"
                              className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-200"
                            >
                              <Calendar className="h-3 w-3" />
                              HSD
                            </span>
                          ) : (
                            <span
                              title="Không theo dõi HSD"
                              className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] text-slate-400 bg-slate-50 border border-slate-200"
                            >
                              Không HSD
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Pack Conversions Count */}
                      <td className="py-3 px-4 text-center">
                        <button
                          type="button"
                          onClick={() => onSelectForConversions?.(item)}
                          className="inline-flex items-center gap-1 text-[11px] text-emerald-700 hover:text-emerald-900 font-medium hover:underline"
                        >
                          <Layers className="h-3.5 w-3.5" />
                          <span>{item.packConversionCount ?? 0} quy cách</span>
                        </button>
                      </td>

                      {/* Status */}
                      <td className="py-3 px-4 text-center">
                        <Badge variant={statusInfo.variant} className="text-[11px]">
                          {statusInfo.label}
                        </Badge>
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setEditingItem(item)}
                            className="h-8 px-2 text-slate-600 hover:text-slate-900"
                            title="Sửa mặt hàng"
                          >
                            <Edit className="h-3.5 w-3.5 mr-1" />
                            <span>Sửa</span>
                          </Button>
                        </div>
                      </td>
                    </motion.tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Footer Summary */}
        <div className="px-4 py-2.5 bg-slate-50/70 border-t border-slate-200 flex items-center justify-between text-xs text-slate-500">
          <span>
            Hiển thị <strong>{filteredItems.length}</strong> / <strong>{items.length}</strong> mặt hàng
          </span>
          <span className="font-mono text-[11px] text-slate-400">
            MTMS Catalog & Moving WAC Baseline
          </span>
        </div>
      </div>

      {/* Create Modal */}
      <CreateItemModal
        isOpen={isCreateModalOpen}
        entityId={entityId}
        units={units}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(newItem) => {
          onItemCreated(newItem)
          setIsCreateModalOpen(false)
        }}
      />

      {/* Edit Modal */}
      <EditItemModal
        isOpen={Boolean(editingItem)}
        entityId={entityId}
        item={editingItem}
        onClose={() => setEditingItem(null)}
        onSuccess={(updated) => {
          onItemUpdated(updated)
          setEditingItem(null)
        }}
      />
    </div>
  )
}
