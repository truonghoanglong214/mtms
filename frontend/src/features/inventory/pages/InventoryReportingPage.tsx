import { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  LayoutDashboard,
  FileSpreadsheet,
  ShieldCheck,
  Package,
  Building2,
  Store,
  ArrowLeft,
  AlertCircle,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/shared/components/ui/button'
import { organizationApi } from '@/features/organization/organizationApi'
import { useAuthStore } from '@/features/auth/authStore'
import { InventoryDashboardTab } from '../components/InventoryDashboardTab'
import { StockMovementsLedgerTab } from '../components/StockMovementsLedgerTab'
import { CogsReconciliationTab } from '../components/CogsReconciliationTab'
import { StockBalancesTable } from '../components/StockBalancesTable'
import type { BusinessEntityDto, BranchDto } from '@/features/organization/types'

type ReportingTab = 'dashboard' | 'ledger' | 'cogs' | 'balances'

export function InventoryReportingPage() {
  const currentUser = useAuthStore((state) => state.user)

  // Entity selection
  const [entities, setEntities] = useState<BusinessEntityDto[]>([])
  const [selectedEntityId, setSelectedEntityId] = useState<string>('')
  const [isEntitiesLoading, setIsEntitiesLoading] = useState(true)

  // Branch selection
  const [branches, setBranches] = useState<BranchDto[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>('')
  const [isBranchesLoading, setIsBranchesLoading] = useState(false)
  const [branchesError, setBranchesError] = useState<string | null>(null)

  // Active tab state
  const [activeTab, setActiveTab] = useState<ReportingTab>('dashboard')

  // 1. Fetch Entities on mount
  useEffect(() => {
    let isMounted = true
    setIsEntitiesLoading(true)
    organizationApi
      .getEntities()
      .then((data) => {
        if (!isMounted) return
        setEntities(data)
        if (data.length > 0) {
          const userEntityId = currentUser?.grants?.find((g) => g.businessEntityId)?.businessEntityId
          const matched = data.find((e) => e.id === userEntityId)
          setSelectedEntityId(matched ? matched.id : data[0].id)
        }
      })
      .catch((err) => {
        console.error('Failed to load business entities:', err)
      })
      .finally(() => {
        if (isMounted) setIsEntitiesLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [currentUser])

  // 2. Fetch Branches when entity changes
  const fetchBranches = useCallback(async () => {
    if (!selectedEntityId) return
    setIsBranchesLoading(true)
    setBranchesError(null)
    try {
      const data = await organizationApi.getBranches(selectedEntityId)
      setBranches(data)
      if (data.length > 0) {
        const userBranchId = currentUser?.grants?.find((g) => g.branchId)?.branchId
        const matched = data.find((b) => b.id === userBranchId)
        if (matched) {
          setSelectedBranchId(matched.id)
        } else if (!selectedBranchId || !data.some((b) => b.id === selectedBranchId)) {
          setSelectedBranchId(data[0].id)
        }
      } else {
        setSelectedBranchId('')
      }
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setBranchesError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách chi nhánh.'
      )
    } finally {
      setIsBranchesLoading(false)
    }
  }, [selectedEntityId, selectedBranchId, currentUser])

  useEffect(() => {
    fetchBranches()
  }, [fetchBranches])

  const activeBranch = branches.find((b) => b.id === selectedBranchId)

  return (
    <div className="space-y-6">
      {/* Header and Branch Selector */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Link to="/inventory">
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Báo Cáo Kho & Đối Soát Giá Vốn (COGS)
            </h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Tổng quan tài sản kho, sổ chi tiết chuyển động xuất nhập và kiểm chứng không trùng chi phí giá vốn pha chế.
          </p>
        </div>

        {/* Entity & Branch Selector */}
        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-2xs">
            <Building2 className="h-4 w-4 text-emerald-800 shrink-0" />
            <span className="text-xs font-medium text-slate-500">Pháp nhân:</span>
            {isEntitiesLoading ? (
              <span className="text-xs text-slate-400">Đang tải...</span>
            ) : entities.length === 0 ? (
              <span className="text-xs text-rose-500">Chưa có dữ liệu</span>
            ) : (
              <select
                value={selectedEntityId}
                onChange={(e) => setSelectedEntityId(e.target.value)}
                className="text-xs font-semibold text-slate-800 bg-transparent focus:outline-none cursor-pointer"
              >
                {entities.map((e) => (
                  <option key={e.id} value={e.id}>
                    [{e.entityCode}] {e.registeredName}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-2xs">
            <Store className="h-4 w-4 text-emerald-800 shrink-0" />
            <span className="text-xs font-medium text-slate-500">Chi nhánh:</span>
            {isBranchesLoading ? (
              <span className="text-xs text-slate-400">Đang tải...</span>
            ) : branches.length === 0 ? (
              <span className="text-xs text-amber-600">Chưa có chi nhánh</span>
            ) : (
              <select
                value={selectedBranchId}
                onChange={(e) => setSelectedBranchId(e.target.value)}
                className="text-xs font-semibold text-slate-800 bg-transparent focus:outline-none cursor-pointer max-w-[160px] truncate"
              >
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    [{b.branchCode}] {b.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        </div>
      </div>

      {/* Branch Load Error */}
      {branchesError && (
        <div className="p-3.5 rounded-xl border border-rose-200 bg-rose-50 text-rose-800 flex items-center gap-2 text-xs">
          <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
          <span>{branchesError}</span>
        </div>
      )}

      {/* Tabs Navigation */}
      <div className="flex border-b border-slate-200 bg-white px-2 rounded-t-2xl shadow-2xs overflow-x-auto">
        <button
          type="button"
          onClick={() => setActiveTab('dashboard')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-semibold border-b-2 transition-colors whitespace-nowrap ${
            activeTab === 'dashboard'
              ? 'border-[#304638] text-[#304638]'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <LayoutDashboard className="h-4 w-4" />
          <span>Tổng quan & KPI (Dashboard)</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('ledger')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-semibold border-b-2 transition-colors whitespace-nowrap ${
            activeTab === 'ledger'
              ? 'border-[#304638] text-[#304638]'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <FileSpreadsheet className="h-4 w-4" />
          <span>Sổ chi tiết biến động (Movements Ledger)</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('cogs')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-semibold border-b-2 transition-colors whitespace-nowrap ${
            activeTab === 'cogs'
              ? 'border-[#304638] text-[#304638]'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <ShieldCheck className="h-4 w-4" />
          <span>Đối soát Giá vốn (COGS Reconciliation)</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('balances')}
          className={`flex items-center gap-2 px-4 py-3 text-xs font-semibold border-b-2 transition-colors whitespace-nowrap ${
            activeTab === 'balances'
              ? 'border-[#304638] text-[#304638]'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Package className="h-4 w-4" />
          <span>Tồn kho hiện tại (Stock Balances)</span>
        </button>
      </div>

      {/* Main Tab Content */}
      <div>
        {!selectedBranchId ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center text-slate-500 shadow-2xs">
            <Store className="h-10 w-10 text-slate-300 mx-auto mb-2" />
            <p className="font-semibold text-slate-700">Vui lòng chọn một chi nhánh</p>
            <p className="text-xs text-slate-500">
              Dữ liệu tồn kho, sổ chuyển động và đối soát giá vốn được hạch toán độc lập theo từng chi nhánh.
            </p>
          </div>
        ) : (
          <AnimatePresence mode="wait">
            {activeTab === 'dashboard' && (
              <motion.div
                key="dashboard"
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.15 }}
              >
                <InventoryDashboardTab
                  branchId={selectedBranchId}
                  branchName={activeBranch?.name}
                  onNavigateToLedger={() => setActiveTab('ledger')}
                  onNavigateToCogs={() => setActiveTab('cogs')}
                />
              </motion.div>
            )}

            {activeTab === 'ledger' && (
              <motion.div
                key="ledger"
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.15 }}
              >
                <StockMovementsLedgerTab
                  branchId={selectedBranchId}
                  branchName={activeBranch?.name}
                />
              </motion.div>
            )}

            {activeTab === 'cogs' && (
              <motion.div
                key="cogs"
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.15 }}
              >
                <CogsReconciliationTab
                  branchId={selectedBranchId}
                  branchName={activeBranch?.name}
                />
              </motion.div>
            )}

            {activeTab === 'balances' && (
              <motion.div
                key="balances"
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.15 }}
              >
                <StockBalancesTable
                  branchId={selectedBranchId}
                  branchName={activeBranch?.name}
                />
              </motion.div>
            )}
          </AnimatePresence>
        )}
      </div>
    </div>
  )
}
export default InventoryReportingPage
