import React, { useState } from 'react'
import { motion } from 'motion/react'
import {
  X,
  UploadCloud,
  FileSpreadsheet,
  Download,
  AlertCircle,
  CheckCircle2,
  Lock,
  ArrowRight,
  ArrowLeft,
  ShieldCheck,
  Plus,
  Trash2,
  Package,
  Calendar,
  FileText,
  AlertTriangle,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { openingApi } from '../api/openingApi'
import type {
  OpeningManualRowInput,
  OpeningPreviewResponse,
  CommitOpeningResponse,
  BranchOpeningStatusDto,
} from '../types/opening.types'

interface OpeningInventoryWizardModalProps {
  isOpen: boolean
  onClose: () => void
  branchId: string
  branchName: string
  openingStatus: BranchOpeningStatusDto | null
  onOpeningCommitted: (res: CommitOpeningResponse) => void
}

type WizardStep = 'INPUT' | 'PREVIEW' | 'COMMIT'

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

export const OpeningInventoryWizardModal: React.FC<OpeningInventoryWizardModalProps> = ({
  isOpen,
  onClose,
  branchId,
  branchName,
  openingStatus,
  onOpeningCommitted,
}) => {
  const [currentStep, setCurrentStep] = useState<WizardStep>('INPUT')
  const [openingDate, setOpeningDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [note, setNote] = useState<string>('Khởi tạo kiểm kê tồn kho đầu kỳ')

  // Row entries
  const [rows, setRows] = useState<OpeningManualRowInput[]>([
    {
      itemCode: 'ING-MILK-OT-380G',
      quantity: '50',
      unitCost: '24000',
      lotCode: 'LOT-202611-01',
      expiresOn: '2027-11-16',
    },
    {
      itemCode: 'ING-COFFEE-01',
      quantity: '20',
      unitCost: '150000',
      lotCode: '',
      expiresOn: '',
    },
  ])

  // Preview state
  const [previewData, setPreviewData] = useState<OpeningPreviewResponse | null>(null)
  const [isPreviewLoading, setIsPreviewLoading] = useState(false)
  const [previewError, setPreviewError] = useState<string | null>(null)

  // Commit state
  const [isCommitting, setIsCommitting] = useState(false)
  const [commitError, setCommitError] = useState<string | null>(null)

  // CSV upload state
  const [uploadMessage, setUploadMessage] = useState<string | null>(null)

  const isAlreadyCommitted = openingStatus?.isOpeningCommitted ?? false

  if (!isOpen) return null

  // --- Step 1: Add/Edit Rows ---
  const handleAddRow = () => {
    setRows((prev) => [
      ...prev,
      { itemCode: '', quantity: '1', unitCost: '0', lotCode: '', expiresOn: '' },
    ])
  }

  const handleRemoveRow = (index: number) => {
    setRows((prev) => prev.filter((_, idx) => idx !== index))
  }

  const handleRowChange = (index: number, field: keyof OpeningManualRowInput, value: string) => {
    setRows((prev) =>
      prev.map((row, idx) => (idx === index ? { ...row, [field]: value } : row))
    )
  }

  // Parse CSV File on Client
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setUploadMessage(`Đang phân tích file "${file.name}"...`)

    const reader = new FileReader()
    reader.onload = (event) => {
      try {
        const text = event.target?.result as string
        if (!text) {
          setUploadMessage('File rỗng.')
          return
        }

        const lines = text.split(/\r?\n/).filter((line) => line.trim().length > 0)
        if (lines.length <= 1) {
          setUploadMessage('File chỉ có dòng tiêu đề hoặc không có dữ liệu.')
          return
        }

        // Header check
        const headerCols = lines[0].split(',').map((c) => c.replace(/^["']|["']$/g, '').trim().toLowerCase())
        const itemCodeIdx = headerCols.findIndex((c) => c === 'item_code' || c === 'mã nvl' || c === 'itemcode')
        const qtyIdx = headerCols.findIndex((c) => c === 'quantity' || c === 'số lượng' || c === 'qty')
        const costIdx = headerCols.findIndex((c) => c === 'unit_cost' || c === 'đơn giá' || c === 'unitcost')
        const lotIdx = headerCols.findIndex((c) => c === 'lot_code' || c === 'số lô' || c === 'lot')
        const expIdx = headerCols.findIndex((c) => c === 'expires_on' || c === 'hạn sử dụng' || c === 'expiry')

        const parsedRows: OpeningManualRowInput[] = []
        for (let i = 1; i < lines.length; i++) {
          const rawLine = lines[i].trim()
          if (!rawLine) continue

          // Basic CSV regex split
          const cols = rawLine.split(',').map((c) => c.replace(/^["']|["']$/g, '').trim())
          const code = itemCodeIdx !== -1 ? cols[itemCodeIdx] : cols[0]
          const qty = qtyIdx !== -1 ? cols[qtyIdx] : cols[3] || '1'
          const cost = costIdx !== -1 ? cols[costIdx] : cols[4] || '0'
          const lot = lotIdx !== -1 ? cols[lotIdx] : cols[5] || ''
          const exp = expIdx !== -1 ? cols[expIdx] : cols[6] || ''

          if (code) {
            parsedRows.push({
              itemCode: code,
              quantity: qty,
              unitCost: cost,
              lotCode: lot || undefined,
              expiresOn: exp || undefined,
            })
          }
        }

        if (parsedRows.length > 0) {
          setRows(parsedRows)
          setUploadMessage(`Đã nạp thành công ${parsedRows.length} mặt hàng từ file!`)
        } else {
          setUploadMessage('Không tìm thấy dòng dữ liệu hợp lệ trong file.')
        }
      } catch (err) {
        console.error('CSV parse error:', err)
        setUploadMessage('Lỗi đọc định dạng file CSV. Vui lòng sử dụng file mẫu.')
      }
    }
    reader.readAsText(file)
  }

  // --- Step 2: Live Validation & Preview ---
  const handleProceedToPreview = async () => {
    if (rows.length === 0) {
      setPreviewError('Vui lòng thêm ít nhất một mặt hàng để mở sổ.')
      return
    }

    setIsPreviewLoading(true)
    setPreviewError(null)

    try {
      const res = await openingApi.previewOpening(branchId, {
        openingDate,
        rows: rows.map((r) => ({
          itemCode: r.itemCode.trim(),
          quantity: r.quantity.trim(),
          unitCost: r.unitCost.trim(),
          lotCode: r.lotCode?.trim() || null,
          expiresOn: r.expiresOn?.trim() || null,
        })),
      })
      setPreviewData(res)
      setCurrentStep('PREVIEW')
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setPreviewError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể kiểm tra dữ liệu mở sổ với máy chủ.'
      )
    } finally {
      setIsPreviewLoading(false)
    }
  }

  // --- Step 3: Commit Opening (TC-024) ---
  const handleCommitOpening = async () => {
    setIsCommitting(true)
    setCommitError(null)

    try {
      const res = await openingApi.commitOpening(branchId, {
        openingDate,
        rows: rows.map((r) => ({
          itemCode: r.itemCode.trim(),
          quantity: r.quantity.trim(),
          unitCost: r.unitCost.trim(),
          lotCode: r.lotCode?.trim() || null,
          expiresOn: r.expiresOn?.trim() || null,
        })),
        note: note.trim() || undefined,
      })

      onOpeningCommitted(res)
      onClose()
    } catch (err: unknown) {
      const apiErr = err as {
        response?: {
          status?: number
          data?: { type?: string; detail?: string; message?: string }
        }
      }
      if (
        apiErr.response?.status === 409 ||
        apiErr.response?.data?.type?.includes('opening-inventory-already-posted')
      ) {
        setCommitError(
          'Chi nhánh này đã được mở sổ trước đó! Không thể mở sổ lần 2 để tránh nhân đôi số dư tồn kho.'
        )
      } else {
        setCommitError(
          apiErr.response?.data?.detail ||
            apiErr.response?.data?.message ||
            'Lỗi khi lưu số dư mở sổ. Vui lòng thử lại.'
        )
      }
    } finally {
      setIsCommitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
      <motion.div
        initial={{ opacity: 0, scale: 0.98, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.98, y: 10 }}
        className="bg-white rounded-3xl border border-slate-200 shadow-2xl w-full max-w-4xl overflow-hidden my-auto flex flex-col max-h-[92vh]"
      >
        {/* Modal Header */}
        <div className="px-6 py-4.5 border-b border-slate-200 flex items-center justify-between bg-slate-50/70 shrink-0">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-800 text-white shadow-xs">
              <Package className="h-5 w-5" />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-900">
                  Khởi Tạo Tồn Kho Đầu Kỳ & Giá Vốn WAC
                </h2>
              </div>
              <p className="text-xs text-slate-500">
                Chi nhánh: <strong className="text-slate-800">{branchName}</strong>
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* TC-024 Guard Warning if branch already locked */}
        {isAlreadyCommitted ? (
          <div className="p-8 text-center space-y-4 my-auto">
            <div className="h-16 w-16 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center mx-auto shadow-xs">
              <Lock className="h-8 w-8" />
            </div>
            <div className="space-y-1.5 max-w-lg mx-auto">
              <h3 className="text-lg font-bold text-slate-900">
                Chi nhánh đã Khóa Mở Sổ Tồn Kho Đầu Kỳ
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                Theo quy chuẩn kiểm toán và nguyên tắc an toàn dữ liệu tài chính, mỗi chi nhánh
                chỉ được phép mở sổ số dư đầu kỳ một lần duy nhất.
              </p>
              <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 font-mono text-left space-y-1 mt-3">
                <div>• Ngày mở sổ: {openingStatus?.openingRecord?.openingDate}</div>
                <div>• Tổng mặt hàng: {openingStatus?.openingRecord?.totalItems} mặt hàng</div>
                <div>• Tổng giá trị định giá: {formatVnd(openingStatus?.openingRecord?.totalValue || '0')}</div>
                <div>• Người phê duyệt: {openingStatus?.openingRecord?.approvedByName || openingStatus?.openingRecord?.approvedBy}</div>
              </div>
            </div>
            <div className="pt-4">
              <Button onClick={onClose} className="bg-[#304638] text-white rounded-xl text-xs px-6">
                Đóng thông báo
              </Button>
            </div>
          </div>
        ) : (
          <>
            {/* Step Wizard Breadcrumbs */}
            <div className="px-6 py-3 border-b border-slate-100 bg-white flex items-center gap-2 text-xs font-medium shrink-0">
              <div
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-colors ${
                  currentStep === 'INPUT'
                    ? 'bg-emerald-50 text-emerald-800 font-semibold'
                    : 'text-slate-400 hover:text-slate-600 cursor-pointer'
                }`}
                onClick={() => setCurrentStep('INPUT')}
              >
                <span className="h-5 w-5 rounded-full bg-emerald-800 text-white flex items-center justify-center text-[10px]">
                  1
                </span>
                <span>Nhập danh sách kiểm kê</span>
              </div>
              <span className="text-slate-300">/</span>
              <div
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-colors ${
                  currentStep === 'PREVIEW'
                    ? 'bg-emerald-50 text-emerald-800 font-semibold'
                    : 'text-slate-400'
                }`}
              >
                <span className={`h-5 w-5 rounded-full flex items-center justify-center text-[10px] ${
                  previewData ? 'bg-emerald-800 text-white' : 'bg-slate-200 text-slate-500'
                }`}>
                  2
                </span>
                <span>Kiểm tra & Tính WAC dự kiến</span>
              </div>
              <span className="text-slate-300">/</span>
              <div
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-colors ${
                  currentStep === 'COMMIT'
                    ? 'bg-emerald-50 text-emerald-800 font-semibold'
                    : 'text-slate-400'
                }`}
              >
                <span className="h-5 w-5 rounded-full bg-slate-200 text-slate-500 flex items-center justify-center text-[10px]">
                  3
                </span>
                <span>Ký duyệt mở sổ (Owner Sign-off)</span>
              </div>
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto flex-1 space-y-5">
              {/* --- STEP 1: INPUT --- */}
              {currentStep === 'INPUT' && (
                <div className="space-y-5">
                  {/* Business Date and Info */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-2xl border border-slate-200">
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                        <Calendar className="h-3.5 w-3.5 text-emerald-700" />
                        <span>Ngày kinh doanh mở sổ (Cutover Date)</span>
                      </label>
                      <Input
                        type="date"
                        value={openingDate}
                        onChange={(e) => setOpeningDate(e.target.value)}
                        className="h-9 text-xs rounded-xl bg-white border-slate-200"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                        <FileText className="h-3.5 w-3.5 text-emerald-700" />
                        <span>Ghi chú mở sổ</span>
                      </label>
                      <Input
                        value={note}
                        onChange={(e) => setNote(e.target.value)}
                        placeholder="VD: Kiểm kê khai trương chi nhánh..."
                        className="h-9 text-xs rounded-xl bg-white border-slate-200"
                      />
                    </div>
                  </div>

                  {/* CSV Template & Upload Banner */}
                  <div className="rounded-2xl border border-dashed border-emerald-300 bg-emerald-50/40 p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center shrink-0">
                        <FileSpreadsheet className="h-5 w-5" />
                      </div>
                      <div>
                        <div className="text-xs font-bold text-slate-900">
                          Nhập dữ liệu nhanh từ File Excel / CSV mẫu
                        </div>
                        <p className="text-[11px] text-slate-600">
                          Tải file mẫu kiểm kê chuẩn UTF-8, điền số lượng và đơn giá thực tế rồi nạp vào hệ thống.
                        </p>
                        {uploadMessage && (
                          <p className="text-[11px] font-semibold text-emerald-800 mt-1">
                            {uploadMessage}
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => openingApi.downloadOpeningTemplate()}
                        className="h-8 text-xs gap-1.5 rounded-xl border-emerald-300 text-emerald-900 bg-white hover:bg-emerald-50"
                      >
                        <Download className="h-3.5 w-3.5" />
                        <span>Tải mẫu CSV</span>
                      </Button>

                      <label className="cursor-pointer">
                        <input
                          type="file"
                          accept=".csv,.txt"
                          onChange={handleFileUpload}
                          className="hidden"
                        />
                        <span className="inline-flex items-center gap-1.5 h-8 px-3 rounded-xl bg-emerald-800 hover:bg-emerald-900 text-white text-xs font-medium shadow-2xs">
                          <UploadCloud className="h-3.5 w-3.5" />
                          <span>Chọn file CSV</span>
                        </span>
                      </label>
                    </div>
                  </div>

                  {/* Manual Rows Table */}
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                        <Package className="h-4 w-4 text-emerald-700" />
                        <span>Danh sách mặt hàng kiểm kê ({rows.length})</span>
                      </h4>

                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={handleAddRow}
                        className="h-7 text-xs gap-1 rounded-lg border-slate-200 text-slate-700 hover:bg-slate-50"
                      >
                        <Plus className="h-3.5 w-3.5 text-emerald-700" />
                        <span>Thêm dòng</span>
                      </Button>
                    </div>

                    <div className="rounded-xl border border-slate-200 overflow-hidden shadow-2xs">
                      <div className="overflow-x-auto max-h-64">
                        <table className="w-full text-xs text-left border-collapse">
                          <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 sticky top-0 z-10">
                            <tr>
                              <th className="py-2 px-2.5 w-10 text-center">#</th>
                              <th className="py-2 px-2.5 min-w-[140px]">Mã NVL *</th>
                              <th className="py-2 px-2.5 min-w-[100px] text-right">Số lượng *</th>
                              <th className="py-2 px-2.5 min-w-[120px] text-right">Đơn giá vốn (VNĐ) *</th>
                              <th className="py-2 px-2.5 min-w-[110px]">Số lô (Nếu có)</th>
                              <th className="py-2 px-2.5 min-w-[110px]">Hạn dùng (HSD)</th>
                              <th className="py-2 px-2 w-10 text-center">Xóa</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100">
                            {rows.map((row, idx) => (
                              <tr key={idx} className="hover:bg-slate-50/60">
                                <td className="py-2 px-2.5 text-center text-slate-400 font-mono text-[11px]">
                                  {idx + 1}
                                </td>
                                <td className="py-2 px-2.5">
                                  <Input
                                    value={row.itemCode}
                                    onChange={(e) => handleRowChange(idx, 'itemCode', e.target.value)}
                                    placeholder="VD: ING-MILK-OT"
                                    className="h-7 text-xs font-mono font-semibold text-emerald-900 rounded-lg bg-white"
                                  />
                                </td>
                                <td className="py-2 px-2.5 text-right">
                                  <Input
                                    type="number"
                                    step="any"
                                    value={row.quantity}
                                    onChange={(e) => handleRowChange(idx, 'quantity', e.target.value)}
                                    placeholder="0"
                                    className="h-7 text-xs font-mono text-right rounded-lg bg-white"
                                  />
                                </td>
                                <td className="py-2 px-2.5 text-right">
                                  <Input
                                    type="number"
                                    step="any"
                                    value={row.unitCost}
                                    onChange={(e) => handleRowChange(idx, 'unitCost', e.target.value)}
                                    placeholder="0"
                                    className="h-7 text-xs font-mono text-right rounded-lg bg-white"
                                  />
                                </td>
                                <td className="py-2 px-2.5">
                                  <Input
                                    value={row.lotCode || ''}
                                    onChange={(e) => handleRowChange(idx, 'lotCode', e.target.value)}
                                    placeholder="Tùy chọn"
                                    className="h-7 text-xs font-mono rounded-lg bg-white"
                                  />
                                </td>
                                <td className="py-2 px-2.5">
                                  <Input
                                    type="date"
                                    value={row.expiresOn || ''}
                                    onChange={(e) => handleRowChange(idx, 'expiresOn', e.target.value)}
                                    className="h-7 text-xs rounded-lg bg-white"
                                  />
                                </td>
                                <td className="py-2 px-2 text-center">
                                  <button
                                    type="button"
                                    disabled={rows.length <= 1}
                                    onClick={() => handleRemoveRow(idx)}
                                    className="p-1 text-slate-400 hover:text-red-600 disabled:opacity-20 rounded"
                                  >
                                    <Trash2 className="h-3.5 w-3.5" />
                                  </button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>

                  {previewError && (
                    <div className="p-3 rounded-xl border border-red-200 bg-red-50 text-red-800 flex items-center gap-2 text-xs">
                      <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
                      <span>{previewError}</span>
                    </div>
                  )}
                </div>
              )}

              {/* --- STEP 2: PREVIEW & TC-024 CHECK --- */}
              {currentStep === 'PREVIEW' && previewData && (
                <div className="space-y-4">
                  {/* Valuation Summary Card */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="bg-slate-50 rounded-xl p-3.5 border border-slate-200">
                      <div className="text-[11px] text-slate-500 font-medium">Tổng mặt hàng hợp lệ</div>
                      <div className="text-base font-bold text-slate-900 mt-0.5">
                        {previewData.totalItems} <span className="text-xs font-normal text-slate-500">mặt hàng</span>
                      </div>
                    </div>
                    <div className="bg-slate-50 rounded-xl p-3.5 border border-slate-200">
                      <div className="text-[11px] text-slate-500 font-medium">Tổng lượng tồn đầu kỳ</div>
                      <div className="text-base font-bold text-slate-900 font-mono mt-0.5">
                        {formatQuantity(previewData.totalQuantity)}
                      </div>
                    </div>
                    <div className="bg-emerald-50 rounded-xl p-3.5 border border-emerald-200">
                      <div className="text-[11px] text-emerald-800 font-medium">Tổng định giá vốn (WAC Seed)</div>
                      <div className="text-base font-bold text-emerald-800 font-mono mt-0.5">
                        {formatVnd(previewData.totalValuation)}
                      </div>
                    </div>
                  </div>

                  {previewData.hasErrors && (
                    <div className="p-3.5 rounded-xl border border-red-300 bg-red-50 text-red-800 flex items-start gap-2.5 text-xs">
                      <AlertTriangle className="h-4 w-4 shrink-0 text-red-600 mt-0.5" />
                      <div>
                        <div className="font-bold">Có lỗi kiểm tra dữ liệu!</div>
                        <p className="text-[11px] mt-0.5">
                          Một số dòng mặt hàng không hợp lệ (mã không tồn tại trong danh mục hoặc thiếu số lô bắt buộc).
                          Vui lòng quay lại Bước 1 để điều chỉnh trước khi ký duyệt mở sổ.
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Preview Rows Table */}
                  <div className="rounded-xl border border-slate-200 overflow-hidden shadow-2xs">
                    <div className="overflow-x-auto max-h-72">
                      <table className="w-full text-xs text-left border-collapse">
                        <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 sticky top-0 z-10">
                          <tr>
                            <th className="py-2.5 px-3 w-10 text-center">#</th>
                            <th className="py-2.5 px-3">Mã NVL</th>
                            <th className="py-2.5 px-3">Tên nguyên vật liệu</th>
                            <th className="py-2.5 px-3 text-center">ĐVT</th>
                            <th className="py-2.5 px-3 text-right">Số lượng</th>
                            <th className="py-2.5 px-3 text-right">Đơn giá vốn</th>
                            <th className="py-2.5 px-3 text-right">Thành tiền (VNĐ)</th>
                            <th className="py-2.5 px-3 text-right">WAC tính toán</th>
                            <th className="py-2.5 px-3 text-center">Trạng thái</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {previewData.previewRows.map((row) => (
                            <tr
                              key={row.rowNumber}
                              className={`hover:bg-slate-50/60 ${
                                row.status === 'ERROR' ? 'bg-red-50/40' : ''
                              }`}
                            >
                              <td className="py-2.5 px-3 text-center text-slate-400 font-mono text-[11px]">
                                {row.rowNumber}
                              </td>
                              <td className="py-2.5 px-3 font-mono font-semibold text-emerald-800">
                                {row.itemCode}
                              </td>
                              <td className="py-2.5 px-3 font-medium text-slate-800">
                                <div>{row.itemName}</div>
                                {row.lotTrackingRequired && (
                                  <div className="text-[10px] text-amber-700 flex items-center gap-1 mt-0.5">
                                    <span>Yêu cầu số lô: {row.lotCode || '(Chưa có)'}</span>
                                    {row.expiresOn && <span>- HSD: {row.expiresOn}</span>}
                                  </div>
                                )}
                              </td>
                              <td className="py-2.5 px-3 text-center">
                                <Badge variant="outline" className="text-[10px] font-mono border-slate-200 bg-slate-50">
                                  {row.unitName || row.unitCode}
                                </Badge>
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono font-bold text-slate-900">
                                {formatQuantity(row.quantity)}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono text-slate-700">
                                {formatVnd(row.unitCost)}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono font-bold text-emerald-800">
                                {formatVnd(row.lineTotal)}
                              </td>
                              <td className="py-2.5 px-3 text-right font-mono text-slate-800">
                                {formatVnd(row.calculatedWac)}
                              </td>
                              <td className="py-2.5 px-3 text-center">
                                {row.status === 'VALID' ? (
                                  <Badge className="bg-emerald-600 hover:bg-emerald-700 text-[10px] gap-1">
                                    <CheckCircle2 className="h-3 w-3" />
                                    Hợp lệ
                                  </Badge>
                                ) : (
                                  <div className="space-y-0.5">
                                    <Badge variant="destructive" className="text-[10px] gap-1">
                                      <AlertCircle className="h-3 w-3" />
                                      Lỗi
                                    </Badge>
                                    {row.errors.map((err, eIdx) => (
                                      <div key={eIdx} className="text-[10px] text-red-600 text-left">
                                        {err}
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}

              {/* --- STEP 3: CONFIRM & OWNER SIGN-OFF --- */}
              {currentStep === 'COMMIT' && previewData && (
                <div className="space-y-4 max-w-xl mx-auto py-2">
                  <div className="p-5 rounded-2xl border border-amber-200 bg-amber-50/70 text-slate-800 space-y-3">
                    <div className="flex items-center gap-2.5 text-amber-900 font-bold text-sm">
                      <ShieldCheck className="h-5 w-5 text-amber-700" />
                      <span>Xác nhận Ký duyệt Mở Sổ Tồn Kho Đầu Kỳ</span>
                    </div>
                    <p className="text-xs text-slate-700 leading-relaxed">
                      Bạn đang chuẩn bị chốt số dư tồn kho ban đầu cho chi nhánh <strong>{branchName}</strong>:
                    </p>
                    <ul className="text-xs text-slate-700 space-y-1.5 list-disc list-inside font-medium bg-white/70 p-3 rounded-xl border border-amber-200/60">
                      <li>Ngày kinh doanh mở sổ: <strong className="font-mono">{openingDate}</strong></li>
                      <li>Số lượng mặt hàng khởi tạo: <strong>{previewData.totalItems} mặt hàng</strong></li>
                      <li>Tổng lượng vật tư: <strong className="font-mono">{formatQuantity(previewData.totalQuantity)}</strong></li>
                      <li>Tổng giá trị vốn đầu kỳ: <strong className="text-emerald-800 font-mono">{formatVnd(previewData.totalValuation)}</strong></li>
                    </ul>
                    <div className="text-[11px] text-amber-900 font-semibold flex items-center gap-1.5 pt-1">
                      <Lock className="h-3.5 w-3.5 text-amber-700" />
                      <span>Lưu ý: Sau khi ký duyệt, sổ kho chi nhánh sẽ bị khóa và không thể mở lại lần 2!</span>
                    </div>
                  </div>

                  {commitError && (
                    <div className="p-3.5 rounded-xl border border-red-200 bg-red-50 text-red-800 flex items-center gap-2.5 text-xs">
                      <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
                      <span>{commitError}</span>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Modal Footer Controls */}
            <div className="px-6 py-4 border-t border-slate-200 bg-slate-50/70 flex items-center justify-between shrink-0">
              {currentStep === 'INPUT' && (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={onClose}
                    className="text-xs rounded-xl"
                  >
                    Hủy bỏ
                  </Button>
                  <Button
                    type="button"
                    disabled={rows.length === 0 || isPreviewLoading}
                    onClick={handleProceedToPreview}
                    className="bg-[#304638] hover:bg-[#25372C] text-white text-xs gap-1.5 rounded-xl shadow-xs"
                  >
                    {isPreviewLoading ? (
                      <span>Đang kiểm tra...</span>
                    ) : (
                      <>
                        <span>Tiếp tục kiểm tra & Tính WAC</span>
                        <ArrowRight className="h-4 w-4" />
                      </>
                    )}
                  </Button>
                </>
              )}

              {currentStep === 'PREVIEW' && (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setCurrentStep('INPUT')}
                    className="text-xs rounded-xl gap-1.5"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    <span>Quay lại chỉnh sửa</span>
                  </Button>
                  <Button
                    type="button"
                    disabled={previewData?.hasErrors}
                    onClick={() => setCurrentStep('COMMIT')}
                    className="bg-[#304638] hover:bg-[#25372C] text-white text-xs gap-1.5 rounded-xl shadow-xs disabled:opacity-40"
                  >
                    <span>Tiếp tục ký duyệt</span>
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </>
              )}

              {currentStep === 'COMMIT' && (
                <>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setCurrentStep('PREVIEW')}
                    className="text-xs rounded-xl gap-1.5"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    <span>Quay lại</span>
                  </Button>
                  <Button
                    type="button"
                    disabled={isCommitting}
                    onClick={handleCommitOpening}
                    className="bg-emerald-700 hover:bg-emerald-800 text-white text-xs gap-1.5 rounded-xl shadow-xs font-bold px-5"
                  >
                    <Lock className="h-4 w-4" />
                    <span>{isCommitting ? 'Đang khóa sổ mở kỳ...' : 'Xác nhận Ký duyệt Mở Sổ'}</span>
                  </Button>
                </>
              )}
            </div>
          </>
        )}
      </motion.div>
    </div>
  )
}
export default OpeningInventoryWizardModal
