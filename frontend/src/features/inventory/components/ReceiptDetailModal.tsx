import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Receipt,
  CreditCard,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  Clock,
  UserCheck,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { receiptApi } from '../api/receiptApi'
import type { PurchaseReceiptDto } from '../types/receipt.types'

interface ReceiptDetailModalProps {
  isOpen: boolean
  branchId: string
  receiptId: string | null
  onClose: () => void
  onReceiptPosted?: (updated: PurchaseReceiptDto) => void
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

function formatQuantity(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(num)
}

export const ReceiptDetailModal: React.FC<ReceiptDetailModalProps> = ({
  isOpen,
  branchId,
  receiptId,
  onClose,
  onReceiptPosted,
}) => {
  const [receipt, setReceipt] = useState<PurchaseReceiptDto | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [isPosting, setIsPosting] = useState(false)
  const [postError, setPostError] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen && branchId && receiptId) {
      setIsLoading(true)
      setError(null)
      setPostError(null)
      receiptApi
        .getReceiptById(branchId, receiptId)
        .then((data) => {
          setReceipt(data)
        })
        .catch((err: unknown) => {
          const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
          setError(
            apiErr.response?.data?.detail ||
              apiErr.response?.data?.message ||
              'Không thể tải chi tiết phiếu nhập kho.'
          )
        })
        .finally(() => {
          setIsLoading(false)
        })
    } else {
      setReceipt(null)
    }
  }, [isOpen, branchId, receiptId])

  const handlePostReceipt = async () => {
    if (!branchId || !receipt) return
    setIsPosting(true)
    setPostError(null)
    try {
      const updated = await receiptApi.postReceipt(branchId, receipt.id)
      setReceipt(updated)
      if (onReceiptPosted) {
        onReceiptPosted(updated)
      }
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setPostError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể chốt ghi sổ phiếu nhập kho.'
      )
    } finally {
      setIsPosting(false)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto bg-black/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.98, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: 10 }}
          transition={{ duration: 0.18 }}
          className="relative w-full max-w-4xl rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden my-6 flex flex-col max-h-[92vh]"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50/70 shrink-0">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#304638] text-white shadow-xs">
                <Receipt className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-slate-900">
                    Chi Tiết Phiếu Nhập Kho Mua Hàng
                  </h2>
                  {receipt && (
                    <Badge variant="outline" className="font-mono text-xs font-semibold">
                      {receipt.receiptNumber}
                    </Badge>
                  )}
                </div>
                <p className="text-xs text-slate-500">
                  Chứng từ nhập kho mua hàng trực tiếp từ nhà cung cấp (BR-INV-05 / TC-008)
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Modal Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {isLoading ? (
              <div className="py-12 flex flex-col items-center justify-center gap-2 text-slate-500">
                <Loader2 className="h-6 w-6 animate-spin text-emerald-800" />
                <span className="text-xs">Đang tải dữ liệu phiếu nhập kho...</span>
              </div>
            ) : error ? (
              <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-800 flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 shrink-0 text-red-600" />
                <span>{error}</span>
              </div>
            ) : receipt ? (
              <>
                {/* Post Error if any */}
                {postError && (
                  <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-800 flex items-center gap-2">
                    <AlertTriangle className="h-4 w-4 shrink-0 text-red-600" />
                    <span>{postError}</span>
                  </div>
                )}

                {/* Status Bar */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200 text-xs">
                  <div>
                    <span className="text-slate-500 block mb-1">Trạng thái phiếu:</span>
                    {receipt.status === 'POSTED' ? (
                      <Badge className="bg-emerald-600 hover:bg-emerald-600 text-white gap-1">
                        <CheckCircle2 className="h-3 w-3" />
                        <span>Đã ghi sổ</span>
                      </Badge>
                    ) : receipt.status === 'VOIDED' ? (
                      <Badge variant="destructive">Đã hủy</Badge>
                    ) : (
                      <Badge variant="secondary" className="gap-1">
                        <Clock className="h-3 w-3" />
                        <span>Bản nháp (Draft)</span>
                      </Badge>
                    )}
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-1">Thanh toán:</span>
                    {receipt.paymentStatus === 'PAID' ? (
                      <Badge className="bg-emerald-50 text-emerald-800 border-emerald-300 font-medium">
                        Đã thanh toán
                      </Badge>
                    ) : receipt.paymentStatus === 'PARTIALLY_PAID' ? (
                      <Badge className="bg-amber-50 text-amber-800 border-amber-300 font-medium">
                        Thanh toán 1 phần
                      </Badge>
                    ) : (
                      <Badge className="bg-rose-50 text-rose-800 border-rose-300 font-medium">
                        Chưa thanh toán (Ghi nợ)
                      </Badge>
                    )}
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-1">Ngày nhận hàng:</span>
                    <span className="font-semibold text-slate-800 font-mono">
                      {receipt.receivedOn}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-500 block mb-1">Số hóa đơn NCC:</span>
                    <span className="font-semibold text-slate-800 font-mono">
                      {receipt.invoiceNumber || '—'}
                    </span>
                  </div>
                </div>

                {/* Supplier & Value Summary */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="p-4 rounded-xl border border-slate-200 bg-white space-y-2">
                    <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">
                      Thông tin Nhà Cung Cấp
                    </span>
                    <div className="text-sm font-bold text-slate-900">
                      {receipt.supplierName || 'Nhà cung cấp tự do / vãng lai'}
                    </div>
                    {receipt.notes && (
                      <p className="text-xs text-slate-500 italic mt-1">
                        Ghi chú: {receipt.notes}
                      </p>
                    )}
                  </div>

                  <div className="p-4 rounded-xl border border-slate-200 bg-emerald-50/50 space-y-2">
                    <span className="text-xs font-semibold text-emerald-800 uppercase tracking-wider block">
                      Tổng Giá Trị Phiếu Nhập
                    </span>
                    <div className="text-2xl font-bold font-mono text-emerald-950">
                      {formatVnd(receipt.invoiceTotal)}
                    </div>
                    <span className="text-xs text-slate-500">
                      Đã bao gồm toàn bộ đơn giá vốn của các dòng hàng.
                    </span>
                  </div>
                </div>

                {/* TC-008 Linked Payable Debt Card */}
                {receipt.payableId && (
                  <div className="rounded-xl border border-amber-300 bg-amber-50/70 p-4 space-y-2 text-xs">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <CreditCard className="h-4 w-4 text-amber-700" />
                        <h4 className="font-bold text-amber-950">
                          Khoản Công Nợ Nhà Cung Cấp Liên Kết (TC-008)
                        </h4>
                      </div>
                      <Badge className="bg-amber-100 text-amber-900 border-amber-300 text-[10px]">
                        PAYABLE RECORD
                      </Badge>
                    </div>
                    <p className="text-amber-800">
                      Mã khoản nợ: <strong className="font-mono">{receipt.payableId}</strong> · Trị giá gốc: <strong className="font-mono">{formatVnd(receipt.invoiceTotal)}</strong>
                    </p>
                    <p className="text-amber-700/90 text-[11px] leading-relaxed">
                      ✓ Đã tự động hạch toán tăng nợ phải trả NCC. Quỹ tiền mặt/ngân hàng của chi nhánh không bị trừ. Khi chi nhánh lập phiếu chi thanh toán nợ, khoản này sẽ được tất toán tương ứng.
                    </p>
                  </div>
                )}

                {/* Line items table */}
                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Danh sách dòng hàng ({receipt.lines?.length || 0})
                  </h4>

                  <div className="border border-slate-200 rounded-xl overflow-hidden">
                    <table className="w-full text-left text-xs">
                      <thead className="bg-slate-100 text-slate-700 font-semibold border-b border-slate-200">
                        <tr>
                          <th className="py-2.5 px-3 w-10 text-center">#</th>
                          <th className="py-2.5 px-3">Mặt hàng</th>
                          <th className="py-2.5 px-3">Quy cách đóng gói</th>
                          <th className="py-2.5 px-3 text-right">SL gốc</th>
                          <th className="py-2.5 px-3 text-right">Đơn giá vốn gốc</th>
                          <th className="py-2.5 px-3 text-right">Thành tiền</th>
                          <th className="py-2.5 px-3">Số lô / Hạn dùng</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-200 bg-white">
                        {receipt.lines && receipt.lines.length > 0 ? (
                          receipt.lines.map((line) => (
                            <tr key={line.id} className="hover:bg-slate-50/60">
                              <td className="py-2.5 px-3 text-center text-slate-500 font-mono">
                                {line.lineNo}
                              </td>
                              <td className="py-2.5 px-3 font-medium text-slate-800">
                                <div>{line.itemName || line.itemCode || line.itemId}</div>
                                {line.itemCode && (
                                  <span className="text-[10px] text-slate-500 font-mono">
                                    {line.itemCode}
                                  </span>
                                )}
                              </td>
                              <td className="py-2.5 px-3 text-slate-600">
                                {line.packName ? (
                                  <div>
                                    <span className="font-semibold text-slate-700">
                                      {formatQuantity(line.declaredPackQuantity || 0)} {line.packName}
                                    </span>
                                    <span className="block text-[10px] text-slate-500">
                                      (Tỷ lệ quy đổi snapshot: x{line.conversionSnapshot})
                                    </span>
                                  </div>
                                ) : (
                                  <span className="text-slate-400">Đơn vị gốc</span>
                                )}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono font-semibold text-slate-800">
                                {formatQuantity(line.receivedBaseQuantity)} {line.baseUnitName || ''}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono text-slate-600">
                                {formatVnd(line.baseUnitCost)}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-900">
                                {formatVnd(line.lineTotal)}
                              </td>
                              <td className="py-2.5 px-3 text-slate-600">
                                {line.lotCode ? (
                                  <div>
                                    <Badge variant="outline" className="font-mono text-[10px]">
                                      {line.lotCode}
                                    </Badge>
                                    {line.expiresOn && (
                                      <span className="block text-[10px] text-slate-500 mt-0.5">
                                        HSD: {line.expiresOn}
                                      </span>
                                    )}
                                  </div>
                                ) : (
                                  <span className="text-slate-400">—</span>
                                )}
                              </td>
                            </tr>
                          ))
                        ) : (
                          <tr>
                            <td colSpan={7} className="py-6 text-center text-slate-400">
                              Không có dữ liệu dòng hàng.
                            </td>
                          </tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Audit Information */}
                <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between text-[11px] text-slate-500 gap-2">
                  <div className="flex items-center gap-1.5">
                    <UserCheck className="h-3.5 w-3.5 text-slate-400" />
                    <span>
                      Người ghi sổ:{' '}
                      <strong className="text-slate-700">
                        {receipt.postedByName || 'Chưa ghi sổ'}
                      </strong>
                    </span>
                  </div>
                  {receipt.postedAt && (
                    <div className="flex items-center gap-1.5 font-mono">
                      <Clock className="h-3.5 w-3.5 text-slate-400" />
                      <span>{new Date(receipt.postedAt).toLocaleString('vi-VN')}</span>
                    </div>
                  )}
                </div>
              </>
            ) : null}
          </div>

          {/* Modal Footer */}
          <div className="flex items-center justify-between border-t border-slate-200 px-6 py-4 bg-slate-50 shrink-0">
            <div>
              {receipt?.status === 'DRAFT' && (
                <span className="text-xs text-amber-700 flex items-center gap-1.5 font-medium">
                  <AlertTriangle className="h-3.5 w-3.5" />
                  Phiếu này chưa ghi sổ. Tồn kho và công nợ chưa được cập nhật.
                </span>
              )}
            </div>

            <div className="flex items-center gap-3">
              <Button type="button" variant="outline" onClick={onClose} className="text-xs">
                Đóng
              </Button>

              {receipt?.status === 'DRAFT' && (
                <Button
                  type="button"
                  disabled={isPosting}
                  onClick={handlePostReceipt}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-1.5 shadow-xs"
                >
                  {isPosting && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  <span>Chốt ghi sổ phiếu này (Post Receipt)</span>
                </Button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
