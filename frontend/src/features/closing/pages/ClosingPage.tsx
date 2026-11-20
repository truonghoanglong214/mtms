import { useState, useEffect, useCallback } from 'react'
import {
  Building2,
  Calendar,
  CheckCircle2,
  AlertTriangle,
  Lock,
  FileText,
  RefreshCw,
  Eye,
  Loader2,
  AlertCircle,
  X,
} from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { useAuthStore } from '@/features/auth/authStore'
import { apiClient } from '@/shared/api/apiClient'
import { closingApi } from '../api/closingApi'
import {
  ClosingPrerequisitesDto,
  MonthlyCloseDto,
  ManagerialPnLReportDto,
} from '../types/closing.types'

interface BranchOption {
  id: string
  name: string
  code?: string
}

function formatCurrency(amount: number | undefined | null): string {
  if (amount == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount)
}

function getDefaultMonth(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

export function ClosingPage() {
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  const [branches, setBranches] = useState<BranchOption[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '')
  const [selectedMonth, setSelectedMonth] = useState<string>(getDefaultMonth())

  const [loading, setLoading] = useState(false)
  const [prereqs, setPrereqs] = useState<ClosingPrerequisitesDto | null>(null)
  const [history, setHistory] = useState<MonthlyCloseDto[]>([])
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Modals state
  const [isCloseModalOpen, setIsCloseModalOpen] = useState(false)
  const [closeNote, setCloseNote] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const [isSnapshotOpen, setIsSnapshotOpen] = useState(false)
  const [snapshotData, setSnapshotData] = useState<ManagerialPnLReportDto | null>(null)

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

  const loadClosingData = useCallback(async () => {
    if (!selectedBranchId || !selectedMonth) return
    setLoading(true)
    setErrorMsg(null)
    const monthStart = `${selectedMonth}-01`
    try {
      const [prereqRes, historyRes] = await Promise.all([
        closingApi.getPrerequisites(selectedBranchId, monthStart),
        closingApi.getCloseHistory(selectedBranchId),
      ])
      setPrereqs(prereqRes)
      setHistory(historyRes)
    } catch (err: unknown) {
      console.error('Failed to fetch closing data:', err)
      const message = err instanceof Error ? err.message : 'Lỗi khi tải dữ liệu chốt sổ'
      setErrorMsg(message)
    } finally {
      setLoading(false)
    }
  }, [selectedBranchId, selectedMonth])

  useEffect(() => {
    loadClosingData()
  }, [loadClosingData])

  const handleBranchChange = (branchId: string) => {
    setSelectedBranchId(branchId)
    const found = branches.find((b) => b.id === branchId)
    setActiveBranch(branchId, found?.name || null)
  }

  const handleExecuteClose = async () => {
    if (!selectedBranchId || !selectedMonth) return
    setIsSubmitting(true)
    setErrorMsg(null)
    try {
      await closingApi.closePeriod(selectedBranchId, {
        monthStart: `${selectedMonth}-01`,
        note: closeNote.trim() || undefined,
      })
      setIsCloseModalOpen(false)
      setCloseNote('')
      await loadClosingData()
    } catch (err: unknown) {
      console.error('Close failed:', err)
      const res = (err as { response?: { data?: { detail?: string; title?: string } } })?.response?.data
      setErrorMsg(res?.detail || res?.title || 'Chốt sổ thất bại.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleViewSnapshot = (closeItem: MonthlyCloseDto) => {
    if (closeItem.snapshot) {
      try {
        const parsed = JSON.parse(closeItem.snapshot) as ManagerialPnLReportDto
        setSnapshotData(parsed)
        setIsSnapshotOpen(true)
        return
      } catch (e) {
        console.error('Failed to parse snapshot JSON', e)
      }
    }
    setErrorMsg('Không tìm thấy bản chụp snapshot hợp lệ.')
  }

  const isCurrentMonthClosed = prereqs?.status === 'CLOSED'
  const currentClosedRecord = history.find((h) => h.monthStart === `${selectedMonth}-01` && h.status === 'CLOSED')

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight">Chốt Sổ Kỳ Kinh Doanh</h1>
            <Badge variant="provisional">TẠM TÍNH / PROVISIONAL</Badge>
          </div>
          <p className="text-muted-foreground text-sm mt-1">
            Kiểm tra checklist điều kiện tiên quyết (TC-040), chụp snapshot P&amp;L và khóa bất biến số liệu (TC-041).
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

          <Button variant="outline" size="sm" onClick={loadClosingData} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </Button>
        </div>
      </div>

      {/* Error Message */}
      {errorMsg && (
        <div className="p-4 rounded-md bg-destructive/10 border border-destructive/20 text-destructive flex items-start gap-3">
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <div className="text-sm font-medium flex-1">{errorMsg}</div>
          <button onClick={() => setErrorMsg(null)} className="text-destructive/70 hover:text-destructive">
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Period Status Banner */}
      <div
        className={`p-5 rounded-lg border flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all shadow-sm ${
          isCurrentMonthClosed
            ? 'bg-emerald-50/70 border-emerald-300 dark:bg-emerald-950/30 dark:border-emerald-800'
            : 'bg-card border-border'
        }`}
      >
        <div className="flex items-start gap-3">
          <div
            className={`p-2.5 rounded-full mt-0.5 ${
              isCurrentMonthClosed
                ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/60 dark:text-emerald-300'
                : 'bg-blue-100 text-blue-700 dark:bg-blue-900/60 dark:text-blue-300'
            }`}
          >
            {isCurrentMonthClosed ? <Lock className="h-5 w-5" /> : <Calendar className="h-5 w-5" />}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-semibold text-base">
                Kỳ kinh doanh Tháng {selectedMonth}
              </span>
              <span
                className={`text-xs px-2.5 py-0.5 rounded-full font-semibold uppercase tracking-wider ${
                  isCurrentMonthClosed
                    ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200'
                    : 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
                }`}
              >
                {prereqs?.status || 'OPEN'}
              </span>
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              {isCurrentMonthClosed
                ? `Đã chốt sổ lúc ${new Date(currentClosedRecord?.closedAt || '').toLocaleString('vi-VN')} bởi Chủ quán. Dữ liệu nguồn đã bị KHÓA BẤT BIẾN theo TC-041.`
                : 'Kỳ kinh doanh đang mở. Hãy hoàn tất báo cáo doanh thu và duyệt bảng lương trước khi chốt sổ.'}
            </p>
          </div>
        </div>

        {isCurrentMonthClosed && currentClosedRecord && (
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <div className="text-xs text-muted-foreground">Lợi nhuận tạm tính</div>
              <div className="text-base font-bold text-emerald-600 dark:text-emerald-400">
                {formatCurrency(currentClosedRecord.managerialProfit)}
              </div>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleViewSnapshot(currentClosedRecord)}
              className="gap-2 shrink-0 border-emerald-400 hover:bg-emerald-100 dark:hover:bg-emerald-900"
            >
              <Eye className="h-4 w-4" />
              Xem Snapshot Bất Biến
            </Button>
          </div>
        )}
      </div>

      {/* Prerequisites Checklist Section (TC-040) */}
      <Card>
        <CardHeader className="pb-3 border-b">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <CheckCircle2 className="h-5 w-5 text-primary" />
              Checklist Điều Kiện Tiên Quyết Chốt Sổ (TC-040)
            </CardTitle>
            <span className="text-xs text-muted-foreground">BR-CLS-01 &bull; TC-040</span>
          </div>
        </CardHeader>
        <CardContent className="pt-4 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* 1. Daily Revenue Check */}
            <div
              className={`p-4 rounded-lg border transition-all ${
                prereqs?.revenueCheck.passed
                  ? 'bg-emerald-50/50 border-emerald-200 dark:bg-emerald-950/20'
                  : 'bg-amber-50/50 border-amber-200 dark:bg-amber-950/20'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  {prereqs?.revenueCheck.passed ? (
                    <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
                  ) : (
                    <AlertTriangle className="h-5 w-5 text-amber-600 shrink-0" />
                  )}
                  <div>
                    <h4 className="font-semibold text-sm">1. Báo cáo Doanh thu Ngày (BR-CLS-01)</h4>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      100% ngày hoạt động phải có Báo cáo xác nhận (hoặc ngày nghỉ có lý do)
                    </p>
                  </div>
                </div>
                <span
                  className={`text-xs px-2.5 py-0.5 rounded-full font-semibold border ${
                    prereqs?.revenueCheck.passed
                      ? 'bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-950/60 dark:text-emerald-300'
                      : 'bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-950/60 dark:text-amber-300'
                  }`}
                >
                  {prereqs?.revenueCheck.passed ? 'ĐẠT' : 'CHƯA ĐẠT'}
                </span>
              </div>

              <div className="mt-3 pt-3 border-t text-xs flex justify-between items-center">
                <span>Số ngày đã nộp báo cáo:</span>
                <span className="font-bold">
                  {prereqs?.revenueCheck.reportedDays || 0} / {prereqs?.revenueCheck.totalDaysInMonth || 0} ngày
                </span>
              </div>

              {prereqs?.revenueCheck.missingDates && prereqs.revenueCheck.missingDates.length > 0 && (
                <div className="mt-2 text-xs text-amber-700 dark:text-amber-300">
                  <span className="font-semibold">Ngày còn thiếu: </span>
                  {prereqs.revenueCheck.missingDates.join(', ')}
                </div>
              )}
            </div>

            {/* 2. Payroll Approval Check */}
            <div
              className={`p-4 rounded-lg border transition-all ${
                prereqs?.payrollCheck.passed
                  ? 'bg-emerald-50/50 border-emerald-200 dark:bg-emerald-950/20'
                  : 'bg-amber-50/50 border-amber-200 dark:bg-amber-950/20'
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  {prereqs?.payrollCheck.passed ? (
                    <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
                  ) : (
                    <AlertTriangle className="h-5 w-5 text-amber-600 shrink-0" />
                  )}
                  <div>
                    <h4 className="font-semibold text-sm">2. Bảng Lương Chi Nhánh (TC-017)</h4>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Bảng lương tháng bắt buộc phải được Chủ quán phê duyệt (APPROVED)
                    </p>
                  </div>
                </div>
                <span
                  className={`text-xs px-2.5 py-0.5 rounded-full font-semibold border ${
                    prereqs?.payrollCheck.passed
                      ? 'bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-950/60 dark:text-emerald-300'
                      : 'bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-950/60 dark:text-amber-300'
                  }`}
                >
                  {prereqs?.payrollCheck.passed ? 'ĐẠT (APPROVED)' : 'CHƯA DUYỆT'}
                </span>
              </div>

              <div className="mt-3 pt-3 border-t text-xs flex justify-between items-center">
                <span>Trạng thái bảng lương:</span>
                <span className="font-bold uppercase">{prereqs?.payrollCheck.status || 'CHƯA TẠO'}</span>
              </div>
              <div className="mt-1 text-xs flex justify-between items-center text-muted-foreground">
                <span>Tổng chi phí lương trích trước:</span>
                <span className="font-semibold">{formatCurrency(prereqs?.payrollCheck.totalGrossSalary)}</span>
              </div>
            </div>

            {/* 3. Financial Reconciliation Check */}
            <div className="p-4 rounded-lg border bg-card/60">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
                  <div>
                    <h4 className="font-semibold text-sm">3. Đối Chiếu Sổ Quỹ &amp; Ngân Hàng (TC-012)</h4>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Kiểm tra chốt số dư và biên bản đối soát tài khoản tài chính
                    </p>
                  </div>
                </div>
                <span className="text-xs px-2.5 py-0.5 rounded-full font-semibold border bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-950/60 dark:text-emerald-300">
                  ĐẠT
                </span>
              </div>
              <div className="mt-3 pt-3 border-t text-xs flex justify-between items-center">
                <span>Số tài khoản đối chiếu:</span>
                <span className="font-bold">
                  {prereqs?.reconciliationCheck.reconciledCount || 0} / {prereqs?.reconciliationCheck.totalAccounts || 0} tài khoản
                </span>
              </div>
            </div>

            {/* 4. Moving WAC Inventory COGS Check */}
            <div className="p-4 rounded-lg border bg-card/60">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
                  <div>
                    <h4 className="font-semibold text-sm">4. Xuất Kho &amp; Giá Vốn COGS (TC-020)</h4>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Phiếu xuất kho tiêu hao nguyên vật liệu tính theo Moving WAC
                    </p>
                  </div>
                </div>
                <span className="text-xs px-2.5 py-0.5 rounded-full font-semibold border bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-950/60 dark:text-emerald-300">
                  ĐẠT
                </span>
              </div>
              <div className="mt-3 pt-3 border-t text-xs flex justify-between items-center">
                <span>Số phiếu xuất kho ghi nhận:</span>
                <span className="font-bold">{prereqs?.inventoryCheck.consumptionIssuesCount || 0} phiếu</span>
              </div>
            </div>
          </div>

          {/* Action Callout */}
          <div className="pt-2">
            {!isCurrentMonthClosed && (
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-lg bg-muted/40 border">
                <div>
                  <div className="font-semibold text-sm">
                    {prereqs?.canClose ? 'Sẵn sàng chốt sổ' : 'Chưa thể thực hiện chốt sổ'}
                  </div>
                  <div className="text-xs text-muted-foreground mt-0.5">
                    {prereqs?.canClose
                      ? 'Tất cả điều kiện tiên quyết đã thỏa mãn. Thao tác này chỉ dành riêng cho Chủ quán (BRANCH_OWNER).'
                      : prereqs?.blockReason || 'Vui lòng hoàn thành các yêu cầu trong checklist trên.'}
                  </div>
                </div>

                <Button
                  onClick={() => setIsCloseModalOpen(true)}
                  disabled={!prereqs?.canClose || loading}
                  className="gap-2 shrink-0 bg-primary font-semibold"
                >
                  <Lock className="h-4 w-4" />
                  Chốt Sổ Tháng Này
                </Button>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Closing History Table */}
      <Card>
        <CardHeader className="pb-3 border-b">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <FileText className="h-5 w-5 text-muted-foreground" />
              Lịch Sử Chốt Sổ Chi Nhánh
            </CardTitle>
            <span className="text-xs text-muted-foreground">{history.length} kỳ chốt</span>
          </div>
        </CardHeader>
        <CardContent className="pt-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-muted-foreground text-left">
                  <th className="py-3 px-3 font-medium">Kỳ Chốt (Tháng)</th>
                  <th className="py-3 px-3 font-medium">Phiên Bản</th>
                  <th className="py-3 px-3 font-medium">Trạng Thái</th>
                  <th className="py-3 px-3 font-medium text-right">LN Quản Trị Tạm Tính</th>
                  <th className="py-3 px-3 font-medium">Thời Gian Chốt</th>
                  <th className="py-3 px-3 font-medium">Ghi Chú</th>
                  <th className="py-3 px-3 font-medium text-center">Hành Động</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {history.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-muted-foreground text-sm">
                      Chưa có kỳ nào được chốt sổ cho chi nhánh này.
                    </td>
                  </tr>
                ) : (
                  history.map((item) => (
                    <tr key={item.id} className="hover:bg-muted/40 transition-colors">
                      <td className="py-3 px-3 font-semibold">{item.monthStart.substring(0, 7)}</td>
                      <td className="py-3 px-3 text-muted-foreground">v{item.revision}</td>
                      <td className="py-3 px-3">
                        <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300">
                          {item.status}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-right font-bold text-emerald-600 dark:text-emerald-400">
                        {formatCurrency(item.managerialProfit)}
                      </td>
                      <td className="py-3 px-3 text-muted-foreground text-xs">
                        {item.closedAt ? new Date(item.closedAt).toLocaleString('vi-VN') : '—'}
                      </td>
                      <td className="py-3 px-3 text-muted-foreground text-xs max-w-xs truncate">
                        {item.note || '—'}
                      </td>
                      <td className="py-3 px-3 text-center">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleViewSnapshot(item)}
                          className="h-8 gap-1.5 text-xs"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          Xem Snapshot
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Confirmation Modal to Close Month */}
      {isCloseModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-card border rounded-xl max-w-md w-full p-6 shadow-xl space-y-4 animate-in fade-in zoom-in-95">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2 text-base font-bold">
                <Lock className="h-5 w-5 text-primary" />
                Xác Nhận Chốt Sổ Tháng {selectedMonth}
              </div>
              <button
                onClick={() => setIsCloseModalOpen(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="p-3.5 rounded-lg bg-amber-50 border border-amber-200 dark:bg-amber-950/40 dark:border-amber-800 text-amber-900 dark:text-amber-200 text-xs space-y-2">
              <div className="font-semibold flex items-center gap-1.5">
                <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
                Cảnh Báo Khóa Dữ Liệu Bất Biến (TC-041)
              </div>
              <p>
                Sau khi chốt sổ, toàn bộ dữ liệu <strong>Doanh thu ngày</strong>, <strong>Bảng lương</strong> và <strong>Chi phí</strong> của tháng {selectedMonth} sẽ bị khóa vĩnh viễn. Quản lý chi nhánh sẽ <strong>KHÔNG THỂ</strong> sửa đổi hoặc tạo thêm hồi tố.
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-muted-foreground">Ghi chú chốt sổ (tùy chọn):</label>
              <textarea
                value={closeNote}
                onChange={(e) => setCloseNote(e.target.value)}
                placeholder="Nhập ghi chú hoặc lý do chốt sổ kỳ này..."
                rows={3}
                className="w-full text-sm p-2.5 rounded-md border bg-transparent focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" size="sm" onClick={() => setIsCloseModalOpen(false)} disabled={isSubmitting}>
                Hủy
              </Button>
              <Button size="sm" onClick={handleExecuteClose} disabled={isSubmitting} className="gap-2 font-semibold">
                {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Lock className="h-4 w-4" />}
                Xác Nhận Chốt Sổ
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* View Snapshot Modal */}
      {isSnapshotOpen && snapshotData && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-card border rounded-xl max-w-2xl w-full p-6 shadow-xl space-y-4 max-h-[90vh] overflow-y-auto animate-in fade-in zoom-in-95">
            <div className="flex items-start justify-between border-b pb-3">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-lg">Snapshot P&amp;L Kỳ {snapshotData.monthStart?.substring(0, 7)}</h3>
                  <Badge variant="provisional">TẠM TÍNH</Badge>
                </div>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Bản chụp bất biến được tạo tại thời điểm Chủ quán chốt sổ.
                </p>
              </div>
              <button
                onClick={() => setIsSnapshotOpen(false)}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="p-3 bg-muted/30 rounded-lg text-xs space-y-1 text-muted-foreground border">
              <div>Chi nhánh: <span className="font-semibold text-foreground">{snapshotData.branchName}</span></div>
              <div className="italic">{snapshotData.depreciationDisclaimer}</div>
            </div>

            {/* Snapshot Components Breakdown */}
            <div className="space-y-2 text-sm">
              <div className="flex justify-between py-1.5 border-b">
                <span className="font-medium">1. Doanh Thu Thuần (Net Revenue)</span>
                <span className="font-bold text-foreground">{formatCurrency(snapshotData.netRevenue?.total)}</span>
              </div>
              <div className="pl-4 text-xs text-muted-foreground space-y-1">
                <div className="flex justify-between">
                  <span>- Tiền mặt (Cash):</span>
                  <span>{formatCurrency(snapshotData.netRevenue?.cashRevenue)}</span>
                </div>
                <div className="flex justify-between">
                  <span>- Chuyển khoản (Bank):</span>
                  <span>{formatCurrency(snapshotData.netRevenue?.bankRevenue)}</span>
                </div>
                <div className="flex justify-between">
                  <span>- Giao hàng App (Net):</span>
                  <span>{formatCurrency(snapshotData.netRevenue?.appNetRevenue)}</span>
                </div>
              </div>

              <div className="flex justify-between py-1.5 border-b">
                <span className="font-medium text-destructive">2. Giá Vốn Hàng Bán (COGS Moving WAC)</span>
                <span className="font-bold text-destructive">-{formatCurrency(snapshotData.costOfGoodsSold?.total)}</span>
              </div>

              <div className="flex justify-between py-2 border-b bg-muted/20 px-2 rounded font-bold">
                <span>3. Lợi Nhuận Gộp (Gross Profit)</span>
                <span className="text-foreground">{formatCurrency(snapshotData.grossProfit)}</span>
              </div>

              <div className="flex justify-between py-1.5 border-b">
                <span className="font-medium text-destructive">4. Chi Phí Vận Hành (Total OPEX)</span>
                <span className="font-bold text-destructive">-{formatCurrency(snapshotData.operatingExpenses?.totalOpex)}</span>
              </div>
              <div className="pl-4 text-xs text-muted-foreground space-y-1">
                <div className="flex justify-between">
                  <span>- Lương trích trước (Payroll Accrual):</span>
                  <span>{formatCurrency(snapshotData.operatingExpenses?.payrollAccrual)}</span>
                </div>
                <div className="flex justify-between">
                  <span>- Chi phí vận hành khác:</span>
                  <span>{formatCurrency(snapshotData.operatingExpenses?.otherOpex)}</span>
                </div>
              </div>

              <div className="flex justify-between py-1.5 border-b">
                <span className="font-medium text-destructive">5. Nghĩa Vụ Thuế (Tax Obligations)</span>
                <span className="font-bold text-destructive">-{formatCurrency(snapshotData.taxObligations?.total)}</span>
              </div>

              <div className="flex justify-between py-3 border-t-2 border-primary bg-primary/5 px-3 rounded-lg text-base font-bold">
                <span className="text-primary">Lợi Nhuận Quản Trị Tạm Tính</span>
                <span className="text-primary">{formatCurrency(snapshotData.managerialOperatingProfit)}</span>
              </div>

              {/* Cash Flow Comparison */}
              <div className="pt-3 border-t">
                <h4 className="font-semibold text-xs uppercase tracking-wider text-muted-foreground mb-2">
                  Đối Chiếu Dòng Tiền Sổ Quỹ (Accrual vs Cash Flow)
                </h4>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="p-2 bg-muted/40 rounded">
                    <span className="text-muted-foreground">Dòng tiền thuần thực tế:</span>
                    <div className="font-bold text-sm mt-0.5">{formatCurrency(snapshotData.cashFlowComparison?.netCashFlow)}</div>
                  </div>
                  <div className="p-2 bg-muted/40 rounded">
                    <span className="text-muted-foreground">Chênh lệch Dồn tích vs Tiền mặt:</span>
                    <div className="font-bold text-sm mt-0.5">{formatCurrency(snapshotData.cashFlowComparison?.accrualCashVariance)}</div>
                  </div>
                </div>
                <p className="text-xs text-muted-foreground mt-2 italic">
                  {snapshotData.cashFlowComparison?.varianceExplanation}
                </p>
              </div>
            </div>

            <div className="flex justify-end pt-2 border-t">
              <Button size="sm" onClick={() => setIsSnapshotOpen(false)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
