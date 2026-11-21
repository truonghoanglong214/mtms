import { useState, useEffect, useCallback } from 'react'
import {
  Building2,
  Calendar,
  DollarSign,
  TrendingUp,
  RefreshCw,
  Info,
  Loader2,
  Receipt,
  Users,
  Wallet,
  ShoppingBag,
} from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { useAuthStore } from '@/features/auth/authStore'
import { apiClient } from '@/shared/api/apiClient'
import { closingApi } from '@/features/closing/api/closingApi'
import { ManagerialPnLReportDto } from '@/features/closing/types/closing.types'

interface BranchOption {
  id: string
  name: string
  code?: string
}

function formatCurrency(amount: number | undefined | null): string {
  if (amount == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount)
}

function formatPercent(value: number, total: number): string {
  if (!total || total === 0) return '0%'
  return ((value / total) * 100).toFixed(1) + '%'
}

function getDefaultMonth(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

export function ReportingPage() {
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  const [branches, setBranches] = useState<BranchOption[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '')
  const [selectedMonth, setSelectedMonth] = useState<string>(getDefaultMonth())

  const [loading, setLoading] = useState(false)
  const [pnl, setPnl] = useState<ManagerialPnLReportDto | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Fetch branches
  useEffect(() => {
    const fetchBranches = async () => {
      try {
        const res = await apiClient.get<BranchOption[]>('/branches')
        const list = Array.isArray(res.data) ? res.data : (res.data as unknown as { data: BranchOption[] })?.data || []
        setBranches(list)
        if (list.length > 0 && !selectedBranchId) {
          const initial = activeBranchId || list[0].id
          const found = list.find((b: BranchOption) => b.id === initial)
          setSelectedBranchId(initial)
          setActiveBranch(initial, found?.name || null)
        }
      } catch (err) {
        console.error('Failed to load branches:', err)
      }
    }
    fetchBranches()
  }, [activeBranchId, selectedBranchId, setActiveBranch])

  const loadPnL = useCallback(async () => {
    if (!selectedBranchId || !selectedMonth) return
    setLoading(true)
    setErrorMsg(null)
    const monthStart = `${selectedMonth}-01`
    try {
      const data = await closingApi.getManagerialPnL(selectedBranchId, monthStart)
      setPnl(data)
    } catch (err: unknown) {
      console.error('Failed to load P&L report:', err)
      const res = (err as { response?: { data?: { detail?: string; title?: string } } })?.response?.data
      setErrorMsg(res?.detail || res?.title || 'Không thể tải báo cáo P&L quản trị.')
    } finally {
      setLoading(false)
    }
  }, [selectedBranchId, selectedMonth])

  useEffect(() => {
    loadPnL()
  }, [loadPnL])

  const handleBranchChange = (branchId: string) => {
    setSelectedBranchId(branchId)
    const found = branches.find((b) => b.id === branchId)
    setActiveBranch(branchId, found?.name || null)
  }

  const netRevTotal = pnl?.netRevenue.total || 0
  const grossProfit = pnl?.grossProfit || 0
  const opProfit = pnl?.managerialOperatingProfit || 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight">Báo Cáo Quản Trị (P&amp;L Tạm Tính)</h1>
            <Badge variant="provisional">TẠM TÍNH / PROVISIONAL</Badge>
          </div>
          <p className="text-muted-foreground text-sm mt-1">
            Báo cáo kết quả kinh doanh theo cơ sở dồn tích (Accrual) và đối chiếu dòng tiền Sổ quỹ.
          </p>
        </div>

        {/* Global Controls */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 bg-card border rounded-md px-3 py-1.5 text-sm shadow-sm">
            <Building2 className="h-4 w-4 text-muted-foreground" />
            <select
              value={selectedBranchId}
              onChange={(e) => handleBranchChange(e.target.value)}
              className="bg-transparent focus:outline-none cursor-pointer font-medium"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-2 bg-card border rounded-md px-3 py-1.5 text-sm shadow-sm">
            <Calendar className="h-4 w-4 text-muted-foreground" />
            <input
              type="month"
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(e.target.value)}
              className="bg-transparent focus:outline-none cursor-pointer font-medium"
            />
          </div>

          <Button variant="outline" size="sm" onClick={loadPnL} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
        </div>
      </div>

      {/* Mandatory Disclaimer Box (TC-051 / ADR-023 / ADR-007) */}
      <div className="p-4 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-900 dark:text-amber-200 flex items-start gap-3">
        <Info className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
        <div className="text-xs space-y-1">
          <div className="font-semibold text-sm text-amber-800 dark:text-amber-300">
            Lưu Ý Quản Trị Rủi Ro &amp; Nguyên Tắc Kế Toán
          </div>
          <p>
            • <strong>Khấu hao tài sản</strong>: Báo cáo P&amp;L quản trị giai đoạn hiện tại <strong>chưa trích khấu hao tài sản thiết bị vào chi phí</strong>.
          </p>
          <p>
            • <strong>Chi phí lương</strong>: Chi phí lương được ghi nhận theo <strong>tháng phát sinh công (Service Month Accrual)</strong>, bất kể ngày thực chi tiền mặt hay chuyển khoản ngân hàng.
          </p>
          <p>
            • <strong>Nguyên tắc phân phối lợi nhuận</strong>: Lợi nhuận quản trị mang nhãn <strong>TẠM TÍNH</strong> phục vụ điều hành nội bộ, <strong>tuyệt đối không áp dụng để chi trả lợi nhuận thực tế cho nhà đầu tư</strong> khi chưa hoàn tất kiểm toán chuyên gia.
          </p>
        </div>
      </div>

      {/* Error state */}
      {errorMsg && (
        <div className="p-4 rounded-md bg-destructive/10 border border-destructive/20 text-destructive text-sm font-medium">
          {errorMsg}
        </div>
      )}

      {/* Loading state */}
      {loading ? (
        <div className="flex flex-col items-center justify-center p-12 space-y-3">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-sm text-muted-foreground">Đang tính toán kết quả kinh doanh P&amp;L...</p>
        </div>
      ) : pnl ? (
        <>
          {/* Top KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Net Revenue */}
            <Card className="border-l-4 border-l-blue-500">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted-foreground uppercase">1. Doanh Thu Thuần</span>
                  <DollarSign className="h-4 w-4 text-blue-500" />
                </div>
                <div className="mt-2 text-2xl font-bold text-foreground">
                  {formatCurrency(pnl.netRevenue.total)}
                </div>
                <div className="mt-1 text-xs text-muted-foreground flex justify-between">
                  <span>Tiền mặt: {formatCurrency(pnl.netRevenue.cashRevenue)}</span>
                  <span>App NET: {formatCurrency(pnl.netRevenue.appNetRevenue)}</span>
                </div>
              </CardContent>
            </Card>

            {/* COGS */}
            <Card className="border-l-4 border-l-rose-500">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted-foreground uppercase">2. Giá Vốn (COGS)</span>
                  <ShoppingBag className="h-4 w-4 text-rose-500" />
                </div>
                <div className="mt-2 text-2xl font-bold text-rose-600 dark:text-rose-400">
                  -{formatCurrency(pnl.costOfGoodsSold.total)}
                </div>
                <div className="mt-1 text-xs text-muted-foreground flex items-center justify-between">
                  <span>Tỷ lệ giá vốn:</span>
                  <span className="font-semibold text-foreground">
                    {formatPercent(pnl.costOfGoodsSold.total, netRevTotal)}
                  </span>
                </div>
              </CardContent>
            </Card>

            {/* Gross Profit */}
            <Card className="border-l-4 border-l-indigo-500">
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted-foreground uppercase">3. Lợi Nhuận Gộp</span>
                  <TrendingUp className="h-4 w-4 text-indigo-500" />
                </div>
                <div className="mt-2 text-2xl font-bold text-indigo-600 dark:text-indigo-400">
                  {formatCurrency(grossProfit)}
                </div>
                <div className="mt-1 text-xs text-muted-foreground flex items-center justify-between">
                  <span>Biên lợi nhuận gộp:</span>
                  <span className="font-semibold text-foreground">
                    {formatPercent(grossProfit, netRevTotal)}
                  </span>
                </div>
              </CardContent>
            </Card>

            {/* Net Operating Profit */}
            <Card
              className={`border-l-4 ${
                opProfit >= 0 ? 'border-l-emerald-500 bg-emerald-50/20' : 'border-l-red-500 bg-red-50/20'
              }`}
            >
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted-foreground uppercase">
                    LN Quản Trị Tạm Tính
                  </span>
                  <Badge variant="provisional">TẠM TÍNH</Badge>
                </div>
                <div
                  className={`mt-2 text-2xl font-bold ${
                    opProfit >= 0
                      ? 'text-emerald-600 dark:text-emerald-400'
                      : 'text-red-600 dark:text-red-400'
                  }`}
                >
                  {formatCurrency(opProfit)}
                </div>
                <div className="mt-1 text-xs text-muted-foreground flex items-center justify-between">
                  <span>Biên lợi nhuận thuần:</span>
                  <span className="font-semibold text-foreground">
                    {formatPercent(opProfit, netRevTotal)}
                  </span>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Detailed Waterfall Statement */}
          <Card>
            <CardHeader className="pb-3 border-b">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base flex items-center gap-2">
                  <Receipt className="h-5 w-5 text-primary" />
                  Bảng Chi Tiết Kết Quả Kinh Doanh P&amp;L — Tháng {selectedMonth}
                </CardTitle>
                <span className="text-xs text-muted-foreground">{pnl.branchName}</span>
              </div>
            </CardHeader>
            <CardContent className="pt-0">
              <div className="divide-y text-sm">
                {/* 1. Net Revenue */}
                <div className="py-3 flex justify-between items-center font-semibold bg-muted/20 px-3 -mx-3 rounded">
                  <span className="flex items-center gap-2">
                    <DollarSign className="h-4 w-4 text-blue-500" />
                    1. TỔNG DOANH THU THUẦN (NET REVENUE)
                  </span>
                  <span className="text-base font-bold text-foreground">
                    {formatCurrency(pnl.netRevenue.total)}
                  </span>
                </div>
                <div className="py-2 pl-8 pr-3 text-xs text-muted-foreground flex justify-between">
                  <span>• Doanh thu Tiền mặt (Cash Revenue)</span>
                  <span>{formatCurrency(pnl.netRevenue.cashRevenue)}</span>
                </div>
                <div className="py-2 pl-8 pr-3 text-xs text-muted-foreground flex justify-between">
                  <span>• Doanh thu Chuyển khoản (Bank Revenue)</span>
                  <span>{formatCurrency(pnl.netRevenue.bankRevenue)}</span>
                </div>
                <div className="py-2 pl-8 pr-3 text-xs text-muted-foreground flex justify-between">
                  <span>• Doanh thu App giao hàng NET (Sau trừ phí sàn đối tác)</span>
                  <span>{formatCurrency(pnl.netRevenue.appNetRevenue)}</span>
                </div>

                {/* 2. COGS */}
                <div className="py-3 flex justify-between items-center font-semibold px-3 -mx-3">
                  <span className="flex items-center gap-2 text-rose-600 dark:text-rose-400">
                    <ShoppingBag className="h-4 w-4" />
                    2. GIÁ VỐN HÀNG BÁN (COGS — Moving WAC)
                  </span>
                  <span className="text-base font-bold text-rose-600 dark:text-rose-400">
                    -{formatCurrency(pnl.costOfGoodsSold.total)}
                  </span>
                </div>

                {/* 3. Gross Profit */}
                <div className="py-3 flex justify-between items-center font-bold bg-muted/40 px-3 -mx-3 rounded text-base">
                  <span className="flex items-center gap-2 text-foreground">
                    <TrendingUp className="h-4 w-4 text-indigo-500" />
                    3. LỢI NHUẬN GỘP (GROSS PROFIT)
                  </span>
                  <span className="text-indigo-600 dark:text-indigo-400">
                    {formatCurrency(pnl.grossProfit)}
                  </span>
                </div>

                {/* 4. Total OPEX */}
                <div className="py-3 flex justify-between items-center font-semibold px-3 -mx-3">
                  <span className="flex items-center gap-2 text-amber-600 dark:text-amber-400">
                    <Users className="h-4 w-4" />
                    4. TỔNG CHI PHÍ VẬN HÀNH (TOTAL OPEX)
                  </span>
                  <span className="text-base font-bold text-amber-600 dark:text-amber-400">
                    -{formatCurrency(pnl.operatingExpenses.totalOpex)}
                  </span>
                </div>
                <div className="py-2 pl-8 pr-3 text-xs text-muted-foreground flex justify-between">
                  <span>• Chi phí nhân sự dồn tích theo tháng công (Payroll Accrual)</span>
                  <span className="font-medium text-foreground">
                    {formatCurrency(pnl.operatingExpenses.payrollAccrual)}
                  </span>
                </div>
                <div className="py-2 pl-8 pr-3 text-xs text-muted-foreground flex justify-between">
                  <span>• Chi phí mặt bằng, điện nước, vật tư &amp; vận hành khác</span>
                  <span className="font-medium text-foreground">
                    {formatCurrency(pnl.operatingExpenses.otherOpex)}
                  </span>
                </div>

                {/* 5. Tax */}
                <div className="py-3 flex justify-between items-center font-semibold px-3 -mx-3">
                  <span className="flex items-center gap-2 text-muted-foreground">
                    <Receipt className="h-4 w-4" />
                    5. NGHĨA VỤ THUẾ PHÁT SINH (TAX OBLIGATIONS)
                  </span>
                  <span className="text-base font-bold text-foreground">
                    -{formatCurrency(pnl.taxObligations.total)}
                  </span>
                </div>

                {/* 6. Managerial Net Operating Profit */}
                <div className="py-4 flex justify-between items-center font-bold bg-primary/10 px-4 -mx-3 rounded-lg text-lg border-2 border-primary/30">
                  <span className="flex items-center gap-2 text-primary">
                    <TrendingUp className="h-5 w-5" />
                    LỢI NHUẬN KINH DOANH QUẢN TRỊ TẠM TÍNH
                  </span>
                  <span className="text-xl text-primary">
                    {formatCurrency(pnl.managerialOperatingProfit)}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Accrual vs Cash Flow Comparison (TC-016) */}
          <Card>
            <CardHeader className="pb-3 border-b">
              <div className="flex items-center justify-between">
                <CardTitle className="text-base flex items-center gap-2">
                  <Wallet className="h-5 w-5 text-emerald-600" />
                  Đối Chiếu Cơ Sở Dồn Tích vs Dòng Tiền Thực Tế Sổ Quỹ
                </CardTitle>
                <span className="text-xs text-muted-foreground">Cash Flow Comparison</span>
              </div>
            </CardHeader>
            <CardContent className="pt-4 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-center">
                <div className="p-4 rounded-lg bg-muted/40 border">
                  <div className="text-xs text-muted-foreground uppercase font-medium">Tổng Thu Sổ Quỹ</div>
                  <div className="mt-1 text-xl font-bold text-foreground">
                    {formatCurrency(pnl.cashFlowComparison.cashInflow)}
                  </div>
                </div>

                <div className="p-4 rounded-lg bg-muted/40 border">
                  <div className="text-xs text-muted-foreground uppercase font-medium">Tổng Chi Sổ Quỹ</div>
                  <div className="mt-1 text-xl font-bold text-destructive">
                    -{formatCurrency(pnl.cashFlowComparison.cashOutflow)}
                  </div>
                </div>

                <div className="p-4 rounded-lg bg-muted/40 border">
                  <div className="text-xs text-muted-foreground uppercase font-medium">Dòng Tiền Thuần (Net Cash Flow)</div>
                  <div className="mt-1 text-xl font-bold text-foreground">
                    {formatCurrency(pnl.cashFlowComparison.netCashFlow)}
                  </div>
                </div>

                <div className="p-4 rounded-lg bg-blue-50/50 border border-blue-200 dark:bg-blue-950/20 dark:border-blue-800">
                  <div className="text-xs text-blue-700 dark:text-blue-300 uppercase font-medium">
                    Chênh Lệch Dồn Tích - Dòng Tiền
                  </div>
                  <div className="mt-1 text-xl font-bold text-blue-700 dark:text-blue-300">
                    {formatCurrency(pnl.cashFlowComparison.accrualCashVariance)}
                  </div>
                </div>
              </div>

              {/* Variance Explanation Box */}
              <div className="p-4 rounded-lg bg-muted/30 border text-xs text-muted-foreground space-y-1.5">
                <div className="font-semibold text-foreground flex items-center gap-1.5">
                  <Info className="h-4 w-4 text-primary" />
                  Giải Trình Chênh Lệch Dồn Tích vs Dòng Tiền:
                </div>
                <p className="leading-relaxed">
                  {pnl.cashFlowComparison.varianceExplanation}
                </p>
                <div className="pt-1 text-[11px] text-muted-foreground">
                  * Ghi chú: Chênh lệch phát sinh do nguyên tắc kế toán dồn tích (ghi nhận doanh thu App và chi phí lương ngay trong tháng hoạt động) so với thực tế dòng tiền đối soát qua ngân hàng và chi lương vào đầu tháng sau.
                </div>
              </div>
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  )
}
