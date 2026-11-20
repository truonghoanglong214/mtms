import React, { useState, useEffect, useCallback } from 'react'
import {
  Search,
  Plus,
  RefreshCw,
  AlertCircle,
  Receipt,
  Eye,
  CheckCircle2,
  Clock,
  ChevronLeft,
  ChevronRight,
  Loader2,
  CreditCard,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { receiptApi } from '../api/receiptApi'
import { CreateReceiptModal } from './CreateReceiptModal'
import { ReceiptDetailModal } from './ReceiptDetailModal'
import type {
  PurchaseReceiptDto,
  ReceiptStatus,
  ReceiptFilterParams,
} from '../types/receipt.types'
import type { CatalogItemDto, SupplierDto } from '../types/catalog.types'

interface ReceiptListTabProps {
  entityId: string
  branchId: string
  branchName?: string
  suppliers: SupplierDto[]
  items: CatalogItemDto[]
}

function formatVnd(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 2,
  }).format(num)
}

export const ReceiptListTab: React.FC<ReceiptListTabProps> = ({
  entityId,
  branchId,
  branchName,
  suppliers,
  items,
}) => {
  const [receipts, setReceipts] = useState<PurchaseReceiptDto[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [supplierFilter, setSupplierFilter] = useState<string>('')
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [page, setPage] = useState(0)
  const pageSize = 15

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [selectedReceiptIdForDetail, setSelectedReceiptIdForDetail] = useState<string | null>(null)

  // Quick post action state
  const [postingId, setPostingId] = useState<string | null>(null)

  const fetchReceipts = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setError(null)
    try {
      const params: ReceiptFilterParams = {
        status: statusFilter !== 'ALL' ? (statusFilter as ReceiptStatus) : undefined,
        supplierId: supplierFilter || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        search: searchTerm.trim() || undefined,
        page,
        size: pageSize,
      }
      const res = await receiptApi.getReceipts(branchId, params)
      setReceipts(res.items || [])
      setTotalItems(res.totalItems || 0)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách phiếu nhập kho.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, statusFilter, supplierFilter, startDate, endDate, searchTerm, page])

  useEffect(() => {
    fetchReceipts()
  }, [fetchReceipts])

  const handlePostReceipt = async (receiptId: string) => {
    if (!branchId) return
    setPostingId(receiptId)
    try {
      await receiptApi.postReceipt(branchId, receiptId)
      await fetchReceipts()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      alert(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể ghi sổ phiếu nhập kho.'
      )
    } finally {
      setPostingId(null)
    }
  }

  const totalPages = Math.ceil(totalItems / pageSize)

  return (
    <div className="space-y-4">
      {/* Control Bar */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        {/* Search & Filters */}
        <div className="flex flex-wrap items-center gap-2 flex-1">
          <div className="relative min-w-[200px] flex-1 max-w-sm">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-slate-400" />
            <Input
              type="text"
              placeholder="Tìm theo số phiếu, hóa đơn..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setPage(0)
              }}
              className="pl-8 text-xs bg-white"
            />
          </div>

          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(0)
            }}
            className="text-xs rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-slate-700 focus:outline-none focus:ring-1 focus:ring-emerald-700"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="POSTED">Đã ghi sổ (POSTED)</option>
            <option value="DRAFT">Bản nháp (DRAFT)</option>
          </select>

          {/* Supplier Filter */}
          <select
            value={supplierFilter}
            onChange={(e) => {
              setSupplierFilter(e.target.value)
              setPage(0)
            }}
            className="text-xs rounded-lg border border-slate-200 bg-white px-2.5 py-2 text-slate-700 focus:outline-none focus:ring-1 focus:ring-emerald-700 max-w-[180px] truncate"
          >
            <option value="">Tất cả nhà cung cấp</option>
            {suppliers.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>

          {/* Date range */}
          <div className="flex items-center gap-1.5">
            <Input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value)
                setPage(0)
              }}
              className="text-xs w-[130px] bg-white h-9"
              title="Từ ngày"
            />
            <span className="text-slate-400 text-xs">→</span>
            <Input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value)
                setPage(0)
              }}
              className="text-xs w-[130px] bg-white h-9"
              title="Đến ngày"
            />
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={fetchReceipts}
            disabled={isLoading}
            className="text-xs h-9 text-slate-600 hover:text-slate-900 border-slate-200"
            title="Tải lại danh sách"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </Button>
        </div>

        {/* Action Button: Create Receipt */}
        <Button
          type="button"
          onClick={() => setIsCreateOpen(true)}
          className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-xs shrink-0"
        >
          <Plus className="h-4 w-4" />
          <span>+ Nhập kho mới</span>
        </Button>
      </div>

      {/* Error Notice */}
      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-800">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
          <span>{error}</span>
          <Button
            type="button"
            variant="link"
            size="sm"
            onClick={fetchReceipts}
            className="text-xs text-red-900 underline ml-auto p-0 h-auto"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* Receipts Table */}
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden shadow-2xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3 px-4 min-w-[140px]">Số phiếu</th>
                <th className="py-3 px-4 min-w-[200px]">Nhà cung cấp</th>
                <th className="py-3 px-4 min-w-[100px]">Ngày nhận</th>
                <th className="py-3 px-4 min-w-[120px]">Số hóa đơn</th>
                <th className="py-3 px-4 min-w-[140px] text-right">Tổng tiền</th>
                <th className="py-3 px-4 min-w-[140px]">Thanh toán (TC-008)</th>
                <th className="py-3 px-4 min-w-[110px]">Trạng thái</th>
                <th className="py-3 px-4 min-w-[130px] text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {isLoading && receipts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <Loader2 className="h-6 w-6 animate-spin mx-auto text-emerald-800 mb-2" />
                    <span>Đang tải danh sách phiếu nhập kho...</span>
                  </td>
                </tr>
              ) : receipts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <Receipt className="h-10 w-10 text-slate-300" />
                      <span className="font-medium text-slate-700">
                        Chưa có phiếu nhập kho nào tại chi nhánh
                      </span>
                      <p className="text-xs text-slate-400 max-w-sm">
                        Nhấn nút &ldquo;+ Nhập kho mới&rdquo; để lập phiếu mua hàng trực tiếp từ nhà cung cấp.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                receipts.map((rc) => {
                  const isDraft = rc.status === 'DRAFT'
                  const isUnpaid = rc.paymentStatus === 'UNPAID'
                  const isPaid = rc.paymentStatus === 'PAID'

                  return (
                    <tr key={rc.id} className="hover:bg-slate-50/80 transition-colors">
                      {/* Receipt Number */}
                      <td className="py-3 px-4 font-mono font-bold text-slate-900">
                        <button
                          type="button"
                          onClick={() => setSelectedReceiptIdForDetail(rc.id)}
                          className="hover:underline text-emerald-900 hover:text-emerald-700 text-left font-mono"
                        >
                          {rc.receiptNumber}
                        </button>
                      </td>

                      {/* Supplier */}
                      <td className="py-3 px-4">
                        <span className="font-semibold text-slate-800 block">
                          {rc.supplierName || 'NCC Vãng lai'}
                        </span>
                      </td>

                      {/* Received Date */}
                      <td className="py-3 px-4 font-mono text-slate-600">
                        {rc.receivedOn}
                      </td>

                      {/* Invoice Number */}
                      <td className="py-3 px-4 font-mono text-slate-600">
                        {rc.invoiceNumber || '—'}
                      </td>

                      {/* Invoice Total */}
                      <td className="py-3 px-4 text-right font-mono font-bold text-slate-900">
                        {formatVnd(rc.invoiceTotal)}
                      </td>

                      {/* Payment Status Badge (TC-008) */}
                      <td className="py-3 px-4">
                        {isPaid ? (
                          <Badge className="bg-emerald-50 text-emerald-800 border-emerald-300 font-medium text-[11px]">
                            Đã thanh toán
                          </Badge>
                        ) : isUnpaid ? (
                          <div className="flex flex-col gap-0.5">
                            <Badge className="bg-rose-50 text-rose-800 border-rose-300 font-medium text-[11px] w-fit">
                              Chưa thanh toán (Nợ)
                            </Badge>
                            {rc.payableId && (
                              <span className="text-[10px] text-amber-700 flex items-center gap-1 font-mono">
                                <CreditCard className="h-2.5 w-2.5" /> Ghi nợ NCC
                              </span>
                            )}
                          </div>
                        ) : (
                          <Badge className="bg-amber-50 text-amber-800 border-amber-300 font-medium text-[11px]">
                            Thanh toán 1 phần
                          </Badge>
                        )}
                      </td>

                      {/* Status Badge */}
                      <td className="py-3 px-4">
                        {rc.status === 'POSTED' ? (
                          <Badge className="bg-emerald-600 text-white gap-1 text-[11px]">
                            <CheckCircle2 className="h-3 w-3" />
                            <span>Đã ghi sổ</span>
                          </Badge>
                        ) : rc.status === 'VOIDED' ? (
                          <Badge variant="destructive" className="text-[11px]">
                            Đã hủy
                          </Badge>
                        ) : (
                          <Badge variant="secondary" className="gap-1 text-[11px]">
                            <Clock className="h-3 w-3" />
                            <span>Bản nháp</span>
                          </Badge>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-4 text-right space-x-1.5">
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedReceiptIdForDetail(rc.id)}
                          className="h-8 px-2 text-xs text-slate-600 hover:text-slate-900"
                          title="Xem chi tiết"
                        >
                          <Eye className="h-3.5 w-3.5" />
                        </Button>

                        {isDraft && (
                          <Button
                            type="button"
                            size="sm"
                            disabled={postingId === rc.id}
                            onClick={() => handlePostReceipt(rc.id)}
                            className="h-8 px-2 text-xs bg-emerald-700 hover:bg-emerald-800 text-white gap-1"
                            title="Chốt ghi sổ ngay"
                          >
                            {postingId === rc.id ? (
                              <Loader2 className="h-3.5 w-3.5 animate-spin" />
                            ) : (
                              <CheckCircle2 className="h-3.5 w-3.5" />
                            )}
                            <span>Ghi sổ</span>
                          </Button>
                        )}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Table Footer / Pagination */}
        {totalItems > 0 && (
          <div className="bg-slate-50 border-t border-slate-200 px-4 py-3 flex items-center justify-between text-xs text-slate-500">
            <div>
              Hiển thị <strong>{receipts.length}</strong> trên tổng số <strong>{totalItems}</strong> phiếu nhập
            </div>

            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={page === 0 || isLoading}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="h-7 px-2 text-xs"
              >
                <ChevronLeft className="h-3.5 w-3.5" />
                <span>Trước</span>
              </Button>

              <span className="font-mono text-slate-700">
                Trang {page + 1} / {totalPages || 1}
              </span>

              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages || isLoading}
                onClick={() => setPage((p) => p + 1)}
                className="h-7 px-2 text-xs"
              >
                <span>Sau</span>
                <ChevronRight className="h-3.5 w-3.5" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateReceiptModal
        isOpen={isCreateOpen}
        entityId={entityId}
        branchId={branchId}
        branchName={branchName}
        suppliers={suppliers}
        items={items}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={() => {
          fetchReceipts()
        }}
      />

      <ReceiptDetailModal
        isOpen={Boolean(selectedReceiptIdForDetail)}
        branchId={branchId}
        receiptId={selectedReceiptIdForDetail}
        onClose={() => setSelectedReceiptIdForDetail(null)}
        onReceiptPosted={() => {
          fetchReceipts()
        }}
      />
    </div>
  )
}
