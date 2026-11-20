import { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  Receipt,
  CreditCard,
  Building2,
  Store,
  ArrowLeft,
  AlertCircle,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { organizationApi } from '@/features/organization/organizationApi'
import { useAuthStore } from '@/features/auth/authStore'
import { catalogApi } from '../api/catalogApi'
import { ReceiptListTab } from '../components/ReceiptListTab'
import { PayablesLedgerTab } from '../components/PayablesLedgerTab'
import type { BusinessEntityDto, BranchDto } from '@/features/organization/types'
import type { CatalogItemDto, SupplierDto } from '../types/catalog.types'

type ActiveTab = 'receipts' | 'payables'

export function GoodsReceiptPage() {
  const currentUser = useAuthStore((state) => state.user)

  // Entity state
  const [entities, setEntities] = useState<BusinessEntityDto[]>([])
  const [selectedEntityId, setSelectedEntityId] = useState<string>('')
  const [isEntitiesLoading, setIsEntitiesLoading] = useState(true)

  // Branch state
  const [branches, setBranches] = useState<BranchDto[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>('')
  const [isBranchesLoading, setIsBranchesLoading] = useState(false)
  const [branchesError, setBranchesError] = useState<string | null>(null)

  // Catalog items & Suppliers for current entity
  const [items, setItems] = useState<CatalogItemDto[]>([])
  const [suppliers, setSuppliers] = useState<SupplierDto[]>([])

  // Tabs
  const [activeTab, setActiveTab] = useState<ActiveTab>('receipts')

  // 1. Fetch Entities
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
        if (!selectedBranchId || !data.some((b) => b.id === selectedBranchId)) {
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
  }, [selectedEntityId, selectedBranchId])

  useEffect(() => {
    fetchBranches()
  }, [fetchBranches])

  // 3. Fetch Items & Suppliers when entity changes
  useEffect(() => {
    if (!selectedEntityId) return
    let isMounted = true

    catalogApi
      .getCatalogItems(selectedEntityId)
      .then((data) => {
        if (isMounted) setItems(data)
      })
      .catch((err) => console.error('Failed to load items:', err))

    catalogApi
      .getSuppliers(selectedEntityId)
      .then((data) => {
        if (isMounted) setSuppliers(data)
      })
      .catch((err) => console.error('Failed to load suppliers:', err))

    return () => {
      isMounted = false
    }
  }, [selectedEntityId])

  const activeBranch = branches.find((b) => b.id === selectedBranchId)

  return (
    <div className="space-y-6">
      {/* Top Header & Entity/Branch Selectors */}
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <Link to="/inventory">
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 p-0 rounded-lg text-slate-500 hover:text-slate-800"
              >
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Nhập Kho Mua Hàng & Công Nợ NCC
            </h1>
            <Badge variant="secondary" className="font-mono text-xs">
              P0-INV-03 / BR-INV-05
            </Badge>
            <Badge
              variant="outline"
              className="border-emerald-600 text-emerald-800 bg-emerald-50 text-xs font-mono"
            >
              TC-008
            </Badge>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Nhập hàng trực tiếp từ nhà cung cấp, quy đổi quy cách đóng gói (TC-023), tự động cập nhật WAC và theo dõi công nợ chi nhánh.
          </p>
        </div>

        {/* Scope Selectors (Entity & Branch) */}
        <div className="flex flex-wrap items-center gap-3 shrink-0">
          {/* Entity Selector */}
          <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-2xs">
            <Building2 className="h-4 w-4 text-emerald-800" />
            <span className="text-xs font-medium text-slate-500">Pháp nhân:</span>
            {isEntitiesLoading ? (
              <span className="text-xs text-slate-400">Đang tải...</span>
            ) : entities.length === 0 ? (
              <span className="text-xs text-red-500">Không có dữ liệu</span>
            ) : (
              <select
                value={selectedEntityId}
                onChange={(e) => setSelectedEntityId(e.target.value)}
                className="text-xs font-semibold text-slate-800 bg-transparent focus:outline-none cursor-pointer max-w-[160px] truncate"
              >
                {entities.map((e) => (
                  <option key={e.id} value={e.id}>
                    [{e.entityCode}] {e.registeredName}
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Branch Selector */}
          <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-2xs">
            <Store className="h-4 w-4 text-emerald-800" />
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

      {branchesError && (
        <div className="flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-800">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
          <span>{branchesError}</span>
        </div>
      )}

      {/* Tabs Switcher */}
      <div className="flex items-center gap-2 border-b border-slate-200">
        <button
          type="button"
          onClick={() => setActiveTab('receipts')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'receipts'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Receipt className="h-4 w-4" />
          <span>Phiếu nhập kho mua hàng</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('payables')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'payables'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <CreditCard className="h-4 w-4 text-emerald-700" />
          <span>Sổ công nợ nhà cung cấp (TC-008)</span>
        </button>
      </div>

      {/* Tab Contents */}
      <div>
        {!selectedBranchId ? (
          <div className="rounded-xl border border-dashed border-slate-300 p-12 text-center text-slate-500">
            <Store className="h-8 w-8 mx-auto text-slate-400 mb-2" />
            <p className="text-sm font-semibold">Vui lòng chọn chi nhánh để quản lý phiếu nhập kho</p>
          </div>
        ) : (
          <AnimatePresence mode="wait">
            {activeTab === 'receipts' && (
              <motion.div
                key="tab-receipts"
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                transition={{ duration: 0.15 }}
              >
                <ReceiptListTab
                  entityId={selectedEntityId}
                  branchId={selectedBranchId}
                  branchName={activeBranch?.name}
                  suppliers={suppliers}
                  items={items}
                />
              </motion.div>
            )}

            {activeTab === 'payables' && (
              <motion.div
                key="tab-payables"
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                transition={{ duration: 0.15 }}
              >
                <PayablesLedgerTab
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
export default GoodsReceiptPage
