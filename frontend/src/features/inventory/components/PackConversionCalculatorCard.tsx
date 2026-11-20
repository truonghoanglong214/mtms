import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  Calculator,
  ArrowRight,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ShieldCheck,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { catalogApi } from '../api/catalogApi'
import type {
  CatalogItemDto,
  SupplierDto,
  PackConversionCalculationResponse,
  PackConversionCalculationRequest,
} from '../types/catalog.types'

interface PackConversionCalculatorCardProps {
  entityId: string
  items: CatalogItemDto[]
  suppliers: SupplierDto[]
}

export const PackConversionCalculatorCard: React.FC<PackConversionCalculatorCardProps> = ({
  entityId,
  items,
  suppliers,
}) => {
  const [selectedItemId, setSelectedItemId] = useState<string>('')
  const [selectedSupplierId, setSelectedSupplierId] = useState<string>('')
  const [packQuantity, setPackQuantity] = useState<string>('2.000')
  const [businessDate, setBusinessDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )

  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<PackConversionCalculationResponse | null>(null)

  const handleCalculate = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    setError(null)

    if (!selectedItemId) {
      setError('Vui lòng chọn mặt hàng cần tính toán.')
      return
    }
    if (!selectedSupplierId) {
      setError('Vui lòng chọn nhà cung cấp.')
      return
    }
    const qty = parseFloat(packQuantity)
    if (isNaN(qty) || qty <= 0) {
      setError('Số lượng quy cách nhập phải lớn hơn 0 (VD: 2.000).')
      return
    }
    if (!businessDate) {
      setError('Vui lòng chọn ngày kinh doanh.')
      return
    }

    const payload: PackConversionCalculationRequest = {
      itemId: selectedItemId,
      supplierId: selectedSupplierId,
      packQuantity: qty.toFixed(3),
      businessDate,
    }

    setIsLoading(true)
    try {
      const data = await catalogApi.calculatePackConversion(entityId, payload)
      setResult(data)
    } catch (err: unknown) {
      const apiErr = err as {
        response?: {
          data?: {
            title?: string
            detail?: string
            message?: string
          }
        }
      }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.title ||
          apiErr.response?.data?.message ||
          'Chưa tìm thấy quy cách đóng gói hợp lệ giữa mặt hàng và nhà cung cấp này trong ngày đã chọn.'
      )
      setResult(null)
    } finally {
      setIsLoading(false)
    }
  }

  // Preset button for TC-023
  const handleApplyTc023Preset = () => {
    // Look for milk or first item
    const milkItem =
      items.find((i) => i.itemCode.includes('MILK') || i.itemCode.includes('OT')) ||
      items[0]
    const vinamilkSupplier =
      suppliers.find(
        (s) =>
          s.supplierCode.includes('VINAMILK') ||
          s.name.toLowerCase().includes('vinamilk')
      ) || suppliers[0]

    if (milkItem) setSelectedItemId(milkItem.id)
    if (vinamilkSupplier) setSelectedSupplierId(vinamilkSupplier.id)
    setPackQuantity('2.000')
    setBusinessDate(new Date().toISOString().split('T')[0])
  }

  return (
    <div className="rounded-2xl border border-emerald-200/80 bg-gradient-to-br from-emerald-50/40 via-white to-slate-50/80 p-6 shadow-sm space-y-6">
      {/* Title & Badge */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-emerald-100/80 pb-4">
        <div className="flex items-center space-x-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-sm">
            <Calculator className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-base font-bold text-slate-900">
                Công Cụ Tính Toán Quy Cách Đóng Gói (Live Calculator)
              </h3>
              <Badge variant="outline" className="border-emerald-600 text-emerald-700 bg-emerald-50 font-mono text-[10px]">
                TC-023 Verified
              </Badge>
            </div>
            <p className="text-xs text-slate-600 mt-0.5">
              Quy đổi tức thời số lượng thùng/hộp thành đơn vị gốc & snapshot tỷ lệ chốt sổ kho
            </p>
          </div>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleApplyTc023Preset}
          className="text-xs border-emerald-300 text-emerald-800 hover:bg-emerald-50 gap-1.5 h-8 shrink-0"
        >
          <Sparkles className="h-3.5 w-3.5 text-emerald-600" />
          <span>Nạp kịch bản mẫu TC-023</span>
        </Button>
      </div>

      {/* Form Grid */}
      <form onSubmit={handleCalculate} className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Select Item */}
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              1. Chọn mặt hàng <span className="text-red-500">*</span>
            </label>
            <select
              value={selectedItemId}
              onChange={(e) => setSelectedItemId(e.target.value)}
              className="w-full h-9 rounded-md border border-input bg-background px-3 py-1.5 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-emerald-600"
              required
            >
              <option value="">-- Chọn mặt hàng --</option>
              {items.map((i) => (
                <option key={i.id} value={i.id}>
                  [{i.itemCode}] {i.name} ({i.baseUnitCode})
                </option>
              ))}
            </select>
          </div>

          {/* Select Supplier */}
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              2. Chọn nhà cung cấp <span className="text-red-500">*</span>
            </label>
            <select
              value={selectedSupplierId}
              onChange={(e) => setSelectedSupplierId(e.target.value)}
              className="w-full h-9 rounded-md border border-input bg-background px-3 py-1.5 text-xs shadow-sm focus:outline-none focus:ring-1 focus:ring-emerald-600"
              required
            >
              <option value="">-- Chọn nhà cung cấp --</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  [{s.supplierCode}] {s.name}
                </option>
              ))}
            </select>
          </div>

          {/* Quantity */}
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              3. Số lượng quy cách <span className="text-red-500">*</span>
            </label>
            <Input
              type="number"
              step="0.001"
              min="0.001"
              value={packQuantity}
              onChange={(e) => setPackQuantity(e.target.value)}
              placeholder="VD: 2.000"
              className="h-9 text-xs font-mono"
              required
            />
          </div>

          {/* Business Date */}
          <div className="md:col-span-1">
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              4. Ngày chứng từ / Ngày áp dụng <span className="text-red-500">*</span>
            </label>
            <Input
              type="date"
              value={businessDate}
              onChange={(e) => setBusinessDate(e.target.value)}
              className="h-9 text-xs"
              required
            />
          </div>
        </div>

        {/* Submit Button */}
        <div className="flex items-center justify-end gap-3 pt-1">
          <Button
            type="submit"
            disabled={isLoading}
            className="bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold h-9 px-5 gap-2 shadow-sm"
          >
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                <span>Đang tính toán qua API...</span>
              </>
            ) : (
              <>
                <Calculator className="h-4 w-4" />
                <span>Tính toán quy đổi TC-023</span>
              </>
            )}
          </Button>
        </div>
      </form>

      {/* Error state */}
      {error && (
        <motion.div
          initial={{ opacity: 0, y: -4 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-xl border border-red-200 bg-red-50/90 p-4 text-xs text-red-700 flex items-start gap-3"
        >
          <AlertCircle className="h-4 w-4 mt-0.5 text-red-600 shrink-0" />
          <div className="space-y-1">
            <p className="font-semibold">Không thể thực hiện tính toán quy đổi</p>
            <p>{error}</p>
            <p className="text-[11px] text-red-600/80">
              * Gợi ý: Hãy kiểm tra xem mặt hàng đã được cấu hình quy cách đóng gói với nhà cung cấp
              được chọn chưa trong tab &quot;Bảng quy cách&quot;.
            </p>
          </div>
        </motion.div>
      )}

      {/* Result Card with Motion */}
      <AnimatePresence>
        {result && (
          <motion.div
            initial={{ opacity: 0, scale: 0.98, y: 8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.98, y: 8 }}
            transition={{ duration: 0.25 }}
            className="rounded-xl border border-emerald-300 bg-white p-5 shadow-md space-y-4"
          >
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2 text-emerald-800 font-semibold text-sm">
                <CheckCircle2 className="h-5 w-5 text-emerald-600" />
                <span>Kết Quả Quy Đổi Hợp Lệ (TC-023 Verified Snapshot)</span>
              </div>
              <Badge variant="default" className="bg-emerald-700 text-white font-mono text-xs">
                Snapshot: {result.conversionSnapshot} {result.baseUnitCode}/{result.packName}
              </Badge>
            </div>

            {/* Big Math Visual */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 flex flex-col md:flex-row items-center justify-around gap-4 text-center">
              <div>
                <span className="text-[11px] uppercase tracking-wider text-slate-500 block mb-1">
                  Quy cách nhập
                </span>
                <span className="text-base font-bold text-slate-800 font-mono">
                  {result.packQuantity} {result.packName}
                </span>
              </div>

              <div className="text-xl font-bold text-slate-400">×</div>

              <div>
                <span className="text-[11px] uppercase tracking-wider text-slate-500 block mb-1">
                  Tỉ lệ quy đổi (Snapshot)
                </span>
                <span className="text-base font-bold text-emerald-700 font-mono">
                  {result.baseUnitsPerPack} {result.baseUnitCode}
                </span>
              </div>

              <div className="text-xl font-bold text-slate-400">=</div>

              <div>
                <span className="text-[11px] uppercase tracking-wider text-slate-500 block mb-1">
                  Số lượng gốc thực nhập (Base Qty)
                </span>
                <span className="text-xl font-extrabold text-emerald-800 font-mono">
                  {result.calculatedBaseQuantity} {result.baseUnitName} ({result.baseUnitCode})
                </span>
              </div>
            </div>

            {/* Formula Explanation & Governance Guarantee */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
              <div className="p-3 rounded-lg bg-emerald-50/60 border border-emerald-100 text-emerald-900 space-y-1">
                <div className="flex items-center gap-1.5 font-semibold">
                  <ArrowRight className="h-3.5 w-3.5 text-emerald-600" />
                  <span>Diễn giải công thức tính:</span>
                </div>
                <p className="font-mono text-xs text-emerald-950 font-medium">
                  {result.formulaExplanation}
                </p>
                <p className="text-[11px] text-emerald-800/80">
                  Mặt hàng: [{result.itemCode}] {result.itemName} · NCC: {result.supplierName}
                </p>
              </div>

              <div className="p-3 rounded-lg bg-blue-50/60 border border-blue-100 text-blue-900 space-y-1">
                <div className="flex items-center gap-1.5 font-semibold">
                  <ShieldCheck className="h-3.5 w-3.5 text-blue-600" />
                  <span>Bảo đảm tính bất biến (TC-023 Invariant):</span>
                </div>
                <p className="text-[11px] text-blue-800 leading-relaxed">
                  Tỷ lệ quy đổi <strong>{result.conversionSnapshot}</strong> được snapshot cố định
                  trên phiếu nhập. Mọi thay đổi quy cách về sau của NCC sẽ không làm thay đổi giá vốn
                  hoặc số dư lịch sử.
                </p>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
