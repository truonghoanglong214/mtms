import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion } from 'motion/react'
import {
  Layers,
  Plus,
  Search,
  Filter,
  Calendar,
  RefreshCw,
  AlertCircle,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { catalogApi } from '../api/catalogApi'
import { CreatePackConversionModal } from './CreatePackConversionModal'
import { PackConversionCalculatorCard } from './PackConversionCalculatorCard'
import type {
  CatalogItemDto,
  SupplierDto,
  SupplierPackConversionDto,
} from '../types/catalog.types'

interface PackConversionsTabProps {
  entityId: string
  items: CatalogItemDto[]
  suppliers: SupplierDto[]
  initialSelectedItemId?: string
}

export const PackConversionsTab: React.FC<PackConversionsTabProps> = ({
  entityId,
  items,
  suppliers,
  initialSelectedItemId,
}) => {
  const [selectedItemId, setSelectedItemId] = useState<string>(initialSelectedItemId || '')
  const [conversions, setConversions] = useState<SupplierPackConversionDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [search, setSearch] = useState('')

  // Load conversions
  const fetchConversions = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      if (selectedItemId) {
        // Fetch for specific item
        const data = await catalogApi.getPackConversions(entityId, selectedItemId)
        setConversions(data)
      } else {
        // Fetch for all items in entity (fan out)
        const allPromises = items.map((item) =>
          catalogApi.getPackConversions(entityId, item.id).catch(() => [])
        )
        const results = await Promise.all(allPromises)
        setConversions(results.flat())
      }
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách quy cách đóng gói.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [entityId, selectedItemId, items])

  useEffect(() => {
    fetchConversions()
  }, [fetchConversions])

  // Filter conversions
  const filteredConversions = useMemo(() => {
    return conversions.filter((c) => {
      const s = search.toLowerCase()
      const matchSearch =
        search.trim() === '' ||
        (c.itemCode && c.itemCode.toLowerCase().includes(s)) ||
        (c.itemName && c.itemName.toLowerCase().includes(s)) ||
        (c.supplierName && c.supplierName.toLowerCase().includes(s)) ||
        c.packName.toLowerCase().includes(s)

      return matchSearch
    })
  }, [conversions, search])

  const handleConversionCreated = (created: SupplierPackConversionDto) => {
    setConversions((prev) => [created, ...prev])
  }

  return (
    <div className="space-y-6">
      {/* TC-023 Live Demonstration & Calculator Card */}
      <PackConversionCalculatorCard
        entityId={entityId}
        items={items}
        suppliers={suppliers}
      />

      {/* Header & Filter Bar for Conversions Table */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm">
        <div className="flex flex-1 flex-wrap items-center gap-2.5">
          {/* Item Selector Filter */}
          <div className="flex items-center gap-2 min-w-[240px]">
            <Filter className="h-4 w-4 text-slate-400" />
            <select
              value={selectedItemId}
              onChange={(e) => setSelectedItemId(e.target.value)}
              className="h-9 rounded-md border border-input bg-background px-2.5 py-1 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-ring flex-1"
            >
              <option value="">-- Tất cả mặt hàng --</option>
              {items.map((i) => (
                <option key={i.id} value={i.id}>
                  [{i.itemCode}] {i.name}
                </option>
              ))}
            </select>
          </div>

          {/* Search Filter */}
          <div className="relative min-w-[200px] max-w-sm flex-1">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo quy cách, nhà cung cấp..."
              className="pl-9 h-9 text-xs"
            />
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchConversions}
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
            <span>Thiết lập quy cách mới</span>
          </Button>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-700 flex items-start gap-3">
          <AlertCircle className="h-4 w-4 mt-0.5 text-red-600 shrink-0" />
          <div>
            <p className="font-semibold">Lỗi tải danh sách quy cách</p>
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
                <th className="py-3 px-4">Mặt hàng</th>
                <th className="py-3 px-4">Nhà cung cấp</th>
                <th className="py-3 px-4">Quy cách đóng gói</th>
                <th className="py-3 px-4 text-right">Tỷ lệ quy đổi gốc (Rate)</th>
                <th className="py-3 px-4 text-center">Thời gian hiệu lực</th>
                <th className="py-3 px-4 text-center">Snapshot chứng từ</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading && conversions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <RefreshCw className="h-6 w-6 animate-spin text-slate-400" />
                      <p>Đang tải danh sách quy cách đóng gói...</p>
                    </div>
                  </td>
                </tr>
              ) : filteredConversions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-slate-400">
                    <div className="flex flex-col items-center justify-center space-y-2">
                      <Layers className="h-8 w-8 text-slate-300 stroke-[1.5]" />
                      <p className="text-sm font-medium text-slate-600">
                        Chưa có quy cách đóng gói nào được thiết lập
                      </p>
                      <p className="text-xs text-slate-400">
                        Nhấn &quot;Thiết lập quy cách mới&quot; để cấu hình tỷ lệ quy đổi (VD: 1 Thùng = 24 Lon).
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredConversions.map((conv) => {
                  const targetItem = items.find((i) => i.id === conv.itemId)
                  return (
                    <motion.tr
                      key={conv.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="hover:bg-slate-50/60 transition-colors"
                    >
                      {/* Item */}
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-900 font-mono">
                          {conv.itemCode || targetItem?.itemCode || '---'}
                        </div>
                        <div className="text-[11px] text-slate-500">
                          {conv.itemName || targetItem?.name || ''}
                        </div>
                      </td>

                      {/* Supplier */}
                      <td className="py-3 px-4">
                        <div className="font-medium text-slate-800">
                          {conv.supplierName || 'NCC'}
                        </div>
                      </td>

                      {/* Pack Name */}
                      <td className="py-3 px-4">
                        <span className="font-semibold text-slate-900 bg-slate-100 px-2.5 py-1 rounded-md border border-slate-200">
                          {conv.packName}
                        </span>
                      </td>

                      {/* Multiplier / Rate */}
                      <td className="py-3 px-4 text-right font-mono">
                        <span className="font-bold text-emerald-700 text-sm">
                          × {conv.baseUnitsPerPack}
                        </span>
                        <span className="ml-1 text-[11px] text-slate-500">
                          {targetItem?.baseUnitCode || 'Gốc'}
                        </span>
                      </td>

                      {/* Effective Date */}
                      <td className="py-3 px-4 text-center">
                        <div className="inline-flex items-center gap-1 text-[11px] text-slate-600">
                          <Calendar className="h-3.5 w-3.5 text-slate-400" />
                          <span>
                            {conv.effectiveFrom} →{' '}
                            {conv.effectiveTo ? conv.effectiveTo : 'Hiện tại'}
                          </span>
                        </div>
                      </td>

                      {/* Snapshot Invariant */}
                      <td className="py-3 px-4 text-center">
                        <Badge variant="outline" className="border-emerald-300 text-emerald-800 bg-emerald-50 text-[10px]">
                          Snapshot cố định
                        </Badge>
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
            Hiển thị <strong>{filteredConversions.length}</strong> quy cách đóng gói
          </span>
          <span className="font-mono text-[11px] text-slate-400">
            Supplier Pack Conversions
          </span>
        </div>
      </div>

      {/* Create Modal */}
      <CreatePackConversionModal
        isOpen={isCreateModalOpen}
        entityId={entityId}
        items={items}
        suppliers={suppliers}
        initialItemId={selectedItemId}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(created) => {
          handleConversionCreated(created)
          setIsCreateModalOpen(false)
        }}
      />
    </div>
  )
}
