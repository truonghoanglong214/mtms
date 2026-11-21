import { useState, useEffect, useCallback, useMemo } from 'react'
import {
  ArrowRightLeft,
  Plus,
  Layers,
  Calendar,
  Filter,
  RefreshCw,
  Store,
  AlertCircle,
  CheckCircle2,
  Search,
  RotateCcw,
  Wallet,
  CreditCard,
  TrendingUp,
  Receipt,
  Scale,
  Landmark,
  FolderOpen,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { useAuthStore } from '@/features/auth/authStore'
import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import {
  financeApi,
  getLedgerItems,
  getLedgerTotalElements,
} from '../api/financeApi'
import { AccountSummaryCards } from '../components/AccountSummaryCards'
import { CashLedgerTable } from '../components/CashLedgerTable'
import { InternalTransferModal } from '../components/InternalTransferModal'
import { CreateManualEntryModal } from '../components/CreateManualEntryModal'
import { FinancialCategoryModal } from '../components/FinancialCategoryModal'
import { OperatingExpensesTab } from '../components/OperatingExpensesTab'
import { PayablesLedgerTab } from '../components/PayablesLedgerTab'
import { ReceivablesLedgerTab } from '../components/ReceivablesLedgerTab'
import { ReconciliationTab } from '../components/ReconciliationTab'
import { TaxObligationsTab } from '../components/TaxObligationsTab'
import { OpeningBalancesTab } from '../components/OpeningBalancesTab'
import type {
  FinancialAccountDto,
  FinancialCategoryDto,
  CashLedgerEntryDto,
  CashLedgerFilterParams,
  EntryType,
  SourceType,
  InternalTransferResponse,
} from '../types/finance.types'

interface BranchSummary {
  id: string
  code: string
  name: string
  status: string
}

type FinanceTab =
  | 'CASH_LEDGER'
  | 'EXPENSES'
  | 'PAYABLES'
  | 'RECEIVABLES'
  | 'RECONCILIATION'
  | 'TAX'
  | 'OPENING_BALANCES'

export function FinancePage() {
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  // Tab state
  const [activeTab, setActiveTab] = useState<FinanceTab>('CASH_LEDGER')

  // Branch switcher state
  const [branches, setBranches] = useState<BranchSummary[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '')

  // Finance data state
  const [accounts, setAccounts] = useState<FinancialAccountDto[]>([])
  const [categories, setCategories] = useState<FinancialCategoryDto[]>([])
  const [ledgerEntries, setLedgerEntries] = useState<CashLedgerEntryDto[]>([])
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  // Loading states
  const [isLoadingAccounts, setIsLoadingAccounts] = useState<boolean>(false)
  const [isLoadingLedger, setIsLoadingLedger] = useState<boolean>(false)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Pagination state (Cash Ledger)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [pageSize, setPageSize] = useState<number>(15)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [totalItems, setTotalItems] = useState<number>(0)

  // Filter state (Cash Ledger)
  const [selectedAccountId, setSelectedAccountId] = useState<string | undefined>(undefined)
  const [selectedEntryType, setSelectedEntryType] = useState<string>('ALL')
  const [selectedSourceType, setSelectedSourceType] = useState<string>('ALL')
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [searchQuery, setSearchQuery] = useState<string>('')

  // Modals state
  const [isTransferModalOpen, setIsTransferModalOpen] = useState<boolean>(false)
  const [isManualEntryModalOpen, setIsManualEntryModalOpen] = useState<boolean>(false)
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState<boolean>(false)

  // 1. Fetch branches for selector
  useEffect(() => {
    let isMounted = true
    apiClient
      .get<ApiResponse<BranchSummary[]>>('/organizations/branches')
      .then((res) => {
        if (isMounted && res.data?.data) {
          const list = res.data.data
          setBranches(list)
          if (!activeBranchId && list.length > 0) {
            setSelectedBranchId(list[0].id)
            setActiveBranch(list[0].id, list[0].name)
          }
        }
      })
      .catch((err) => {
        console.error('Failed to load branches:', err)
      })

    return () => {
      isMounted = false
    }
  }, [activeBranchId, setActiveBranch])

  // Sync when activeBranchId changes
  useEffect(() => {
    if (activeBranchId) {
      setSelectedBranchId(activeBranchId)
    }
  }, [activeBranchId])

  const handleBranchChange = (newBranchId: string) => {
    setSelectedBranchId(newBranchId)
    const branch = branches.find((b) => b.id === newBranchId)
    if (branch) {
      setActiveBranch(branch.id, branch.name)
    }
    // Reset filters and pagination
    setCurrentPage(0)
    setSelectedAccountId(undefined)
  }

  // 2. Load Categories
  const loadCategories = useCallback(async () => {
    try {
      const cats = await financeApi.getCategories({ activeOnly: true })
      setCategories(cats || [])
    } catch (err) {
      console.error('Failed to load financial categories:', err)
    }
  }, [])

  useEffect(() => {
    loadCategories()
  }, [loadCategories])

  // 3. Load Accounts
  const loadAccounts = useCallback(async () => {
    if (!selectedBranchId) return
    try {
      setIsLoadingAccounts(true)
      const accs = await financeApi.getAccounts(selectedBranchId)
      setAccounts(accs || [])
      setLastUpdated(new Date())
    } catch (err: unknown) {
      console.error('Failed to load accounts:', err)
      setErrorNotice('Không thể tải danh sách tài khoản tài chính chi nhánh.')
    } finally {
      setIsLoadingAccounts(false)
    }
  }, [selectedBranchId])

  // 4. Load Ledger Entries with Filters & Pagination
  const loadLedgerEntries = useCallback(async () => {
    if (!selectedBranchId) return
    try {
      setIsLoadingLedger(true)
      setErrorNotice(null)

      const filterParams: CashLedgerFilterParams = {
        page: currentPage,
        size: pageSize,
      }

      if (selectedAccountId) {
        filterParams.accountId = selectedAccountId
      }
      if (selectedEntryType !== 'ALL') {
        filterParams.entryType = selectedEntryType as EntryType
      }
      if (selectedSourceType !== 'ALL') {
        filterParams.sourceType = selectedSourceType as SourceType
      }
      if (startDate) {
        filterParams.startDate = startDate
      }
      if (endDate) {
        filterParams.endDate = endDate
      }

      const res = await financeApi.getLedgerEntries(selectedBranchId, filterParams)
      const items = getLedgerItems(res)
      const total = getLedgerTotalElements(res)

      setLedgerEntries(items)
      setTotalItems(total)
      setTotalPages(res.totalPages || Math.ceil(total / pageSize) || 1)
      setLastUpdated(new Date())
    } catch (err: unknown) {
      console.error('Failed to load ledger entries:', err)
      setErrorNotice('Không thể tải sổ nhật ký thu chi. Vui lòng thử lại.')
    } finally {
      setIsLoadingLedger(false)
    }
  }, [
    selectedBranchId,
    currentPage,
    pageSize,
    selectedAccountId,
    selectedEntryType,
    selectedSourceType,
    startDate,
    endDate,
  ])

  // Initial and reactive data loading
  useEffect(() => {
    if (selectedBranchId) {
      loadAccounts()
      loadLedgerEntries()
    }
  }, [selectedBranchId, loadAccounts, loadLedgerEntries])

  // Filter entries locally by client-side search query if user types in search box
  const filteredEntries = useMemo(() => {
    if (!searchQuery.trim()) return ledgerEntries
    const q = searchQuery.toLowerCase().trim()
    return ledgerEntries.filter((item) => {
      const matchMemo = item.memo?.toLowerCase().includes(q)
      const matchCategory = item.categoryName?.toLowerCase().includes(q)
      const matchCode = item.categoryCode?.toLowerCase().includes(q)
      const matchAccount = item.accountName?.toLowerCase().includes(q)
      return matchMemo || matchCategory || matchCode || matchAccount
    })
  }, [ledgerEntries, searchQuery])

  // Quick Date Shortcut Handlers
  const handleQuickDateFilter = (type: 'today' | '7days' | 'thisMonth' | 'all') => {
    const today = new Date()
    const formatDate = (d: Date) => d.toISOString().split('T')[0]

    setCurrentPage(0)
    if (type === 'today') {
      const str = formatDate(today)
      setStartDate(str)
      setEndDate(str)
    } else if (type === '7days') {
      const d = new Date()
      d.setDate(d.getDate() - 6)
      setStartDate(formatDate(d))
      setEndDate(formatDate(today))
    } else if (type === 'thisMonth') {
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
      setStartDate(formatDate(firstDay))
      setEndDate(formatDate(today))
    } else {
      setStartDate('')
      setEndDate('')
    }
  }

  const handleResetFilters = () => {
    setSelectedAccountId(undefined)
    setSelectedEntryType('ALL')
    setSelectedSourceType('ALL')
    setStartDate('')
    setEndDate('')
    setSearchQuery('')
    setCurrentPage(0)
  }

  // Handle successful transfer
  const handleTransferSuccess = (res: InternalTransferResponse) => {
    setSuccessToast(`Chuyển tiền nội bộ thành công. Mã giao dịch: ${res.id.slice(0, 8)}`)
    setTimeout(() => setSuccessToast(null), 5000)
    loadAccounts()
    loadLedgerEntries()
  }

  // Handle successful manual entry
  const handleManualEntrySuccess = (res: CashLedgerEntryDto) => {
    setSuccessToast(`Đã ghi sổ thành công bút toán: ${res.categoryName || res.categoryId}`)
    setTimeout(() => setSuccessToast(null), 5000)
    loadAccounts()
    loadLedgerEntries()
  }

  // Handle created category
  const handleCategoryCreated = (newCat: FinancialCategoryDto) => {
    setCategories((prev) => [...prev, newCat])
  }

  const currentBranch = branches.find((b) => b.id === selectedBranchId)

  return (
    <div className="space-y-6 pb-12">
      {/* 1. Page Header & Branch Selector */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Sổ Quỹ & Công Nợ Chi Nhánh
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Theo dõi dòng tiền mặt, ngân hàng, công nợ nhà cung cấp và các khoản phải thu chi nhánh.
          </p>
        </div>

        {/* Branch Switcher */}
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl border border-slate-200 bg-white shadow-xs">
            <Store className="w-4 h-4 text-[#304638]" />
            <span className="text-xs font-semibold text-slate-600">Chi nhánh:</span>
            <select
              value={selectedBranchId}
              onChange={(e) => handleBranchChange(e.target.value)}
              className="text-xs font-medium text-slate-800 bg-transparent focus:outline-none cursor-pointer"
            >
              {branches.length === 0 ? (
                <option value="">Đang tải chi nhánh...</option>
              ) : (
                branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({b.code})
                  </option>
                ))
              )}
            </select>
          </div>
        </div>
      </div>

      {/* Toast Notification */}
      {successToast && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800 shadow-sm animate-in fade-in duration-200">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium">{successToast}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessToast(null)}
            className="text-emerald-700 hover:text-emerald-900 cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Error Notice */}
      {errorNotice && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-xs text-rose-800 shadow-sm">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorNotice}</span>
        </div>
      )}

      {/* 2. Flexible Tab Navigation Strip */}
      <div className="border-b border-slate-200">
        <nav className="flex space-x-2 sm:space-x-4 overflow-x-auto">
          {/* Tab 1: Cash Ledger */}
          <button
            type="button"
            onClick={() => setActiveTab('CASH_LEDGER')}
            className={`flex items-center gap-2 py-3 px-4 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'CASH_LEDGER'
                ? 'border-[#304638] text-[#304638]'
                : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
            }`}
          >
            <Wallet className="w-4 h-4" />
            <span>Sổ quỹ Tiền mặt & Ngân hàng</span>
          </button>

          {/* Tab 2: Operating Expenses */}
          <button
            type="button"
            onClick={() => setActiveTab('EXPENSES')}
            className={`flex items-center gap-2 py-3 px-4 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'EXPENSES'
                ? 'border-[#304638] text-[#304638]'
                : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
            }`}
          >
            <Receipt className="w-4 h-4" />
            <span>Chi phí Vận hành (OPEX)</span>
          </button>

          {/* Tab 3: Supplier Payables Ledger */}
          <button
            type="button"
            onClick={() => setActiveTab('PAYABLES')}
            className={`flex items-center gap-2 py-3 px-4 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'PAYABLES'
                ? 'border-[#304638] text-[#304638]'
                : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
            }`}
          >
            <CreditCard className="w-4 h-4" />
            <span>Sổ Công nợ Phải trả NCC</span>
          </button>

          {/* Tab 4: Other Receivables Ledger */}
          <button
            type="button"
            onClick={() => setActiveTab('RECEIVABLES')}
            className={`flex items-center gap-2 py-3 px-4 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'RECEIVABLES'
                ? 'border-[#304638] text-[#304638]'
                : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
            }`}
          >
            <TrendingUp className="w-4 h-4" />
            <span>Sổ Quản lý Phải thu khác</span>
          </button>

          {/* Tab 5: Cash & Bank Reconciliation */}
          <button
            type="button"
            onClick={() => setActiveTab('RECONCILIATION')}
            className={`flex items-center gap-2 py-3 px-4 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'RECONCILIATION'
                ? 'border-[#304638] text-[#304638]'
                : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
            }`}
          >
            <Scale className="w-4 h-4" />
            <span>Kiểm kê & Đối soát quỹ</span>
          </button>

          {/* Tab 6: Tax Manual Obligations */}
          <button
            type="button"
            onClick={() => setActiveTab('TAX')}
            className={`flex items-center gap-2 py-3 px-4 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'TAX'
                ? 'border-[#304638] text-[#304638]'
                : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
            }`}
          >
            <Landmark className="w-4 h-4" />
            <span>Nghĩa vụ thuế & Ngân sách</span>
          </button>

          {/* Tab 7: Opening Balances Import & Cutover Batch */}
          <button
            type="button"
            onClick={() => setActiveTab('OPENING_BALANCES')}
            className={`flex items-center gap-2 py-3 px-4 text-xs font-semibold border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'OPENING_BALANCES'
                ? 'border-[#304638] text-[#304638]'
                : 'border-transparent text-slate-500 hover:text-slate-800 hover:border-slate-300'
            }`}
          >
            <FolderOpen className="w-4 h-4" />
            <span>Số dư đầu kỳ</span>
          </button>
        </nav>
      </div>

      {/* 3. Tab Contents */}
      {activeTab === 'CASH_LEDGER' && (
        <div className="space-y-6 animate-in fade-in duration-200">
          {/* Account KPI Summary Cards */}
          <AccountSummaryCards
            accounts={accounts}
            isLoading={isLoadingAccounts}
            onRefresh={() => {
              loadAccounts()
              loadLedgerEntries()
            }}
            selectedAccountFilter={selectedAccountId}
            onSelectAccountFilter={(accId) => {
              setSelectedAccountId(accId)
              setCurrentPage(0)
            }}
            lastUpdated={lastUpdated}
          />

          {/* Action Toolbar */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <div className="flex items-center gap-2 flex-wrap">
              {/* Action 1: Internal Transfer (TC-010) */}
              <Button
                type="button"
                onClick={() => setIsTransferModalOpen(true)}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-3.5 py-2 rounded-xl flex items-center gap-2 shadow-xs cursor-pointer"
              >
                <ArrowRightLeft className="w-4 h-4" />
                <span>Chuyển tiền nội bộ</span>
              </Button>

              {/* Action 2: Direct Manual Cash Entry */}
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsManualEntryModalOpen(true)}
                className="border-slate-300 hover:bg-slate-100 text-slate-800 text-xs font-semibold px-3.5 py-2 rounded-xl flex items-center gap-2 cursor-pointer shadow-xs"
              >
                <Plus className="w-4 h-4 text-[#B96849]" />
                <span>Ghi sổ dòng tiền trực tiếp</span>
              </Button>

              {/* Action 3: Categories Chart */}
              <Button
                type="button"
                variant="ghost"
                onClick={() => setIsCategoryModalOpen(true)}
                className="text-xs text-slate-700 hover:text-slate-900 hover:bg-slate-100 px-3 py-2 rounded-xl flex items-center gap-1.5 cursor-pointer"
              >
                <Layers className="w-4 h-4 text-slate-500" />
                <span>Danh mục thu chi</span>
              </Button>
            </div>

            {/* Reload button */}
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => loadLedgerEntries()}
              disabled={isLoadingLedger}
              className="text-xs text-slate-600 flex items-center gap-1.5 cursor-pointer rounded-xl h-8"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoadingLedger ? 'animate-spin text-[#B96849]' : ''}`} />
              <span>Tải lại sổ</span>
            </Button>
          </div>

          {/* Filter Toolbar & Search */}
          <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-3">
            <div className="flex items-center justify-between text-xs font-semibold text-slate-700">
              <div className="flex items-center gap-1.5">
                <Filter className="w-3.5 h-3.5 text-[#304638]" />
                <span>Bộ lọc sổ nhật ký thu chi</span>
              </div>
              {(selectedAccountId ||
                selectedEntryType !== 'ALL' ||
                selectedSourceType !== 'ALL' ||
                startDate ||
                endDate ||
                searchQuery) && (
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="text-[11px] text-rose-600 hover:text-rose-800 font-medium flex items-center gap-1 cursor-pointer"
                >
                  <RotateCcw className="w-3 h-3" />
                  <span>Xóa bộ lọc</span>
                </button>
              )}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
              {/* Filter: Tài khoản */}
              <div className="space-y-1">
                <label className="text-slate-500 font-medium">Tài khoản</label>
                <select
                  value={selectedAccountId || 'ALL'}
                  onChange={(e) => {
                    const val = e.target.value
                    setSelectedAccountId(val === 'ALL' ? undefined : val)
                    setCurrentPage(0)
                  }}
                  className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
                >
                  <option value="ALL">Tất cả tài khoản ({accounts.length})</option>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountName} ({a.accountKind})
                    </option>
                  ))}
                </select>
              </div>

              {/* Filter: Hướng dòng tiền */}
              <div className="space-y-1">
                <label className="text-slate-500 font-medium">Hướng dòng tiền</label>
                <select
                  value={selectedEntryType}
                  onChange={(e) => {
                    setSelectedEntryType(e.target.value)
                    setCurrentPage(0)
                  }}
                  className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
                >
                  <option value="ALL">Tất cả hướng (+ Thu & - Chi)</option>
                  <option value="INFLOW">Chỉ dòng tiền vào (+ Thu)</option>
                  <option value="OUTFLOW">Chỉ dòng tiền ra (- Chi)</option>
                </select>
              </div>

              {/* Filter: Nguồn nghiệp vụ */}
              <div className="space-y-1">
                <label className="text-slate-500 font-medium">Nguồn nghiệp vụ</label>
                <select
                  value={selectedSourceType}
                  onChange={(e) => {
                    setSelectedSourceType(e.target.value)
                    setCurrentPage(0)
                  }}
                  className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
                >
                  <option value="ALL">Tất cả nguồn nghiệp vụ</option>
                  <option value="DAILY_REVENUE">Doanh thu bán hàng</option>
                  <option value="SUPPLIER_PAYMENT">Chi trả nợ NCC</option>
                  <option value="INTERNAL_TRANSFER">Chuyển quỹ nội bộ</option>
                  <option value="CAPITAL_FLOW">Vốn góp / Rút vốn CSH</option>
                  <option value="MANUAL_EXPENSE">Chi phí vận hành trực tiếp</option>
                  <option value="PAYROLL_PAYMENT">Chi lương nhân sự</option>
                  <option value="MANUAL_ADJUSTMENT">Điều chỉnh sổ quỹ</option>
                </select>
              </div>

              {/* Filter: Tìm kiếm memo */}
              <div className="space-y-1">
                <label className="text-slate-500 font-medium">Tìm kiếm diễn giải</label>
                <div className="relative">
                  <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
                  <Input
                    type="text"
                    placeholder="Nội dung, danh mục..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-8 text-xs h-8"
                  />
                </div>
              </div>
            </div>

            {/* Date Range & Quick Shortcuts */}
            <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-100 text-xs">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-slate-500 text-[11px] font-medium flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  Khoảng thời gian:
                </span>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value)
                    setCurrentPage(0)
                  }}
                  className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 font-mono"
                />
                <span className="text-slate-400">đến</span>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value)
                    setCurrentPage(0)
                  }}
                  className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 font-mono"
                />
              </div>

              {/* Quick Date Pills */}
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="text-slate-400 text-[11px]">Nhanh:</span>
                <button
                  type="button"
                  onClick={() => handleQuickDateFilter('today')}
                  className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
                >
                  Hôm nay
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickDateFilter('7days')}
                  className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
                >
                  7 ngày qua
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickDateFilter('thisMonth')}
                  className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
                >
                  Tháng này
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickDateFilter('all')}
                  className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
                >
                  Tất cả
                </button>
              </div>
            </div>
          </div>

          {/* Immutable Cash Ledger Table */}
          <CashLedgerTable
            entries={filteredEntries}
            isLoading={isLoadingLedger}
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={totalItems}
            pageSize={pageSize}
            onPageChange={(page) => setCurrentPage(page)}
            onPageSizeChange={(size) => {
              setPageSize(size)
              setCurrentPage(0)
            }}
          />

          {/* Modals */}
          <InternalTransferModal
            isOpen={isTransferModalOpen}
            branchId={selectedBranchId}
            branchName={currentBranch?.name}
            accounts={accounts}
            onClose={() => setIsTransferModalOpen(false)}
            onSuccess={handleTransferSuccess}
          />

          <CreateManualEntryModal
            isOpen={isManualEntryModalOpen}
            branchId={selectedBranchId}
            branchName={currentBranch?.name}
            accounts={accounts}
            categories={categories}
            onClose={() => setIsManualEntryModalOpen(false)}
            onSuccess={handleManualEntrySuccess}
          />

          <FinancialCategoryModal
            isOpen={isCategoryModalOpen}
            categories={categories}
            onClose={() => setIsCategoryModalOpen(false)}
            onCategoryCreated={handleCategoryCreated}
          />
        </div>
      )}

      {/* Tab 2: Operating Expenses (FE-P0-12 / BR-FIN-07) */}
      {activeTab === 'EXPENSES' && (
        <OperatingExpensesTab
          branchId={selectedBranchId}
          branchName={currentBranch?.name}
          accounts={accounts}
          categories={categories}
          onRefreshAccounts={loadAccounts}
        />
      )}

      {/* Tab 3: Supplier Payables Ledger */}
      {activeTab === 'PAYABLES' && (
        <PayablesLedgerTab
          branchId={selectedBranchId}
          branchName={currentBranch?.name}
          accounts={accounts}
          onRefreshAccounts={loadAccounts}
        />
      )}

      {/* Tab 4: Other Receivables Ledger */}
      {activeTab === 'RECEIVABLES' && (
        <ReceivablesLedgerTab
          branchId={selectedBranchId}
          branchName={currentBranch?.name}
          accounts={accounts}
          onRefreshAccounts={loadAccounts}
        />
      )}

      {/* Tab 5: Cash & Bank Reconciliation (FE-P0-14 / BR-FIN-06 / TC-012) */}
      {activeTab === 'RECONCILIATION' && (
        <ReconciliationTab
          branchId={selectedBranchId}
          branchName={currentBranch?.name}
          accounts={accounts}
          onRefreshAccounts={loadAccounts}
        />
      )}

      {/* Tab 6: Tax Manual Obligations (FE-P0-15 / BR-FIN-04 / TC-012) */}
      {activeTab === 'TAX' && (
        <TaxObligationsTab
          branchId={selectedBranchId}
          branchName={currentBranch?.name}
          accounts={accounts}
          onRefreshAccounts={loadAccounts}
        />
      )}

      {/* Tab 7: Opening Balances Import & Cutover Batch (FE-P0-16 / BR-FIN-08 / GAP-01) */}
      {activeTab === 'OPENING_BALANCES' && (
        <OpeningBalancesTab
          branchId={selectedBranchId || activeBranchId || ''}
          branchName={currentBranch?.name}
          accounts={accounts}
          onRefreshAccounts={loadAccounts}
        />
      )}
    </div>
  )
}
