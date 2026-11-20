import { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  Package,
  Truck,
  Scale,
  Building2,
  Calculator,
  Coins,
  Receipt,
  CreditCard,
  Store,
  FileSpreadsheet,
} from 'lucide-react'
import { Badge } from '@/shared/components/ui/badge'
import { organizationApi } from '@/features/organization/organizationApi'
import { useAuthStore } from '@/features/auth/authStore'
import { catalogApi } from '../api/catalogApi'
import { CatalogItemList } from '../components/CatalogItemList'
import { PackConversionsTab } from '../components/PackConversionsTab'
import { SuppliersTab } from '../components/SuppliersTab'
import { UnitsTab } from '../components/UnitsTab'
import { OpeningInventoryTab } from '../components/OpeningInventoryTab'
import { ReceiptListTab } from '../components/ReceiptListTab'
import { PayablesLedgerTab } from '../components/PayablesLedgerTab'
import { IssueListTab } from '../components/IssueListTab'
import type { BusinessEntityDto, BranchDto } from '@/features/organization/types'
import type {
  CatalogItemDto,
  SupplierDto,
  UnitDto,
} from '../types/catalog.types'

type ActiveTab = 'items' | 'conversions' | 'suppliers' | 'units' | 'opening' | 'receipts' | 'payables' | 'issues'

export function InventoryCatalogPage() {
  const currentUser = useAuthStore((state) => state.user)

  // Entity selection
  const [entities, setEntities] = useState<BusinessEntityDto[]>([])
  const [selectedEntityId, setSelectedEntityId] = useState<string>('')
  const [isEntitiesLoading, setIsEntitiesLoading] = useState(true)

  // Tabs
  const [activeTab, setActiveTab] = useState<ActiveTab>('items')
  const [selectedItemForConversions, setSelectedItemForConversions] = useState<string | undefined>()

  // Data
  const [units, setUnits] = useState<UnitDto[]>([])
  const [isUnitsLoading, setIsUnitsLoading] = useState(false)
  const [unitsError, setUnitsError] = useState<string | null>(null)

  const [items, setItems] = useState<CatalogItemDto[]>([])
  const [isItemsLoading, setIsItemsLoading] = useState(false)
  const [itemsError, setItemsError] = useState<string | null>(null)

  const [suppliers, setSuppliers] = useState<SupplierDto[]>([])
  const [isSuppliersLoading, setIsSuppliersLoading] = useState(false)
  const [suppliersError, setSuppliersError] = useState<string | null>(null)

  // Branches state for receipts & payables tabs
  const [branches, setBranches] = useState<BranchDto[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>('')
  const [isBranchesLoading, setIsBranchesLoading] = useState(false)

  // 1. Load entities
  useEffect(() => {
    let isMounted = true
    setIsEntitiesLoading(true)
    organizationApi
      .getEntities()
      .then((data) => {
        if (!isMounted) return
        setEntities(data)
        if (data.length > 0) {
          // Check if user has a preferred entity from grants
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

  // 2. Load canonical units (global scope)
  const fetchUnits = useCallback(async () => {
    setIsUnitsLoading(true)
    setUnitsError(null)
    try {
      const data = await catalogApi.getUnits()
      setUnits(data)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setUnitsError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh mục đơn vị đo lường.'
      )
    } finally {
      setIsUnitsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchUnits()
  }, [fetchUnits])

  // 3. Load items for selected entity
  const fetchItems = useCallback(async () => {
    if (!selectedEntityId) return
    setIsItemsLoading(true)
    setItemsError(null)
    try {
      const data = await catalogApi.getCatalogItems(selectedEntityId)
      setItems(data)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setItemsError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách mặt hàng của pháp nhân.'
      )
    } finally {
      setIsItemsLoading(false)
    }
  }, [selectedEntityId])

  // 4. Load suppliers for selected entity
  const fetchSuppliers = useCallback(async () => {
    if (!selectedEntityId) return
    setIsSuppliersLoading(true)
    setSuppliersError(null)
    try {
      const data = await catalogApi.getSuppliers(selectedEntityId)
      setSuppliers(data)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setSuppliersError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách nhà cung cấp của pháp nhân.'
      )
    } finally {
      setIsSuppliersLoading(false)
    }
  }, [selectedEntityId])

  // Refetch when entity changes
  useEffect(() => {
    if (selectedEntityId) {
      fetchItems()
      fetchSuppliers()
      setIsBranchesLoading(true)
      organizationApi
        .getBranches(selectedEntityId)
        .then((data) => {
          setBranches(data)
          if (data.length > 0) {
            setSelectedBranchId((prev) => (data.some((b) => b.id === prev) ? prev : data[0].id))
          } else {
            setSelectedBranchId('')
          }
        })
        .catch((err) => console.error('Failed to load branches:', err))
        .finally(() => setIsBranchesLoading(false))
    }
  }, [selectedEntityId, fetchItems, fetchSuppliers])

  // Helper when clicking "Xem quy cách" from an item
  const handleSelectForConversions = (item: CatalogItemDto) => {
    setSelectedItemForConversions(item.id)
    setActiveTab('conversions')
  }

  return (
    <div className="space-y-6">
      {/* Top Header & Entity Selector */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Danh Mục Hàng Hóa & Quy Đổi Quy Cách
            </h1>
            <Badge variant="secondary" className="font-mono text-xs">
              P0-INV-01 / BR-INV-04
            </Badge>
            <Badge variant="outline" className="border-emerald-600 text-emerald-800 bg-emerald-50 text-xs font-mono">
              TC-023
            </Badge>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý nguyên vật liệu, đơn vị tính gốc, nhà cung cấp và hệ số quy đổi đóng gói snapshot nhập kho.
          </p>
        </div>

        {/* Entity & Branch Switchers */}
        <div className="flex flex-wrap items-center gap-2 shrink-0">
          <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-sm">
            <Building2 className="h-4 w-4 text-emerald-800" />
            <span className="text-xs font-medium text-slate-500">Pháp nhân:</span>
            {isEntitiesLoading ? (
              <span className="text-xs text-slate-400">Đang tải pháp nhân...</span>
            ) : entities.length === 0 ? (
              <span className="text-xs text-red-500">Chưa có pháp nhân</span>
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

          {(activeTab === 'receipts' || activeTab === 'payables' || activeTab === 'issues') && (
            <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-sm">
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
                  className="text-xs font-semibold text-slate-800 bg-transparent focus:outline-none cursor-pointer max-w-[150px] truncate"
                >
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      [{b.branchCode}] {b.name}
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Tabs Bar */}
      <div className="flex items-center gap-2 border-b border-slate-200">
        <button
          type="button"
          onClick={() => setActiveTab('items')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'items'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Package className="h-4 w-4" />
          <span>Mặt hàng ({items.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('conversions')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'conversions'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Calculator className="h-4 w-4 text-emerald-700" />
          <span>Quy cách & Tính toán TC-023</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('suppliers')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'suppliers'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Truck className="h-4 w-4" />
          <span>Nhà cung cấp ({suppliers.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('units')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'units'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Scale className="h-4 w-4" />
          <span>Đơn vị tính chuẩn ({units.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('opening')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'opening'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Coins className="h-4 w-4" />
          <span>Tồn kho đầu kỳ & Số dư WAC</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('receipts')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'receipts'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <Receipt className="h-4 w-4 text-emerald-700" />
          <span>Nhập kho mua hàng (TC-008)</span>
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
          <span>Công nợ NCC</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('issues')}
          className={`relative pb-3 px-3 text-xs font-semibold transition-colors flex items-center gap-2 ${
            activeTab === 'issues'
              ? 'text-emerald-800 border-b-2 border-emerald-700'
              : 'text-slate-500 hover:text-slate-800'
          }`}
        >
          <FileSpreadsheet className="h-4 w-4 text-emerald-700" />
          <span>Xuất kho pha chế (TC-020/022)</span>
        </button>
      </div>

      {/* Tab Contents with Animated Transitions */}
      <div className="pt-2">
        <AnimatePresence mode="wait">
          {activeTab === 'items' && (
            <motion.div
              key="tab-items"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.15 }}
            >
              <CatalogItemList
                entityId={selectedEntityId}
                items={items}
                units={units}
                isLoading={isItemsLoading}
                error={itemsError}
                onRefresh={fetchItems}
                onItemCreated={(newItem) => setItems((prev) => [newItem, ...prev])}
                onItemUpdated={(updated) =>
                  setItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)))
                }
                onSelectForConversions={handleSelectForConversions}
              />
            </motion.div>
          )}

          {activeTab === 'conversions' && (
            <motion.div
              key="tab-conversions"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.15 }}
            >
              <PackConversionsTab
                entityId={selectedEntityId}
                items={items}
                suppliers={suppliers}
                initialSelectedItemId={selectedItemForConversions}
              />
            </motion.div>
          )}

          {activeTab === 'suppliers' && (
            <motion.div
              key="tab-suppliers"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.15 }}
            >
              <SuppliersTab
                entityId={selectedEntityId}
                suppliers={suppliers}
                isLoading={isSuppliersLoading}
                error={suppliersError}
                onRefresh={fetchSuppliers}
                onSupplierCreated={(newSupplier) =>
                  setSuppliers((prev) => [newSupplier, ...prev])
                }
              />
            </motion.div>
          )}

          {activeTab === 'units' && (
            <motion.div
              key="tab-units"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.15 }}
            >
              <UnitsTab
                units={units}
                isLoading={isUnitsLoading}
                error={unitsError}
                onRefresh={fetchUnits}
                onUnitCreated={(newUnit) => setUnits((prev) => [...prev, newUnit])}
              />
            </motion.div>
          )}

          {activeTab === 'opening' && (
            <motion.div
              key="tab-opening"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.15 }}
            >
              <OpeningInventoryTab entityId={selectedEntityId} />
            </motion.div>
          )}

          {activeTab === 'receipts' && (
            <motion.div
              key="tab-receipts"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.15 }}
            >
              {selectedBranchId ? (
                <ReceiptListTab
                  entityId={selectedEntityId}
                  branchId={selectedBranchId}
                  branchName={branches.find((b) => b.id === selectedBranchId)?.name}
                  suppliers={suppliers}
                  items={items}
                />
              ) : (
                <div className="rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-500">
                  <p className="text-xs">Vui lòng chọn chi nhánh ở góc trên để xem và lập phiếu nhập kho.</p>
                </div>
              )}
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
              {selectedBranchId ? (
                <PayablesLedgerTab
                  branchId={selectedBranchId}
                  branchName={branches.find((b) => b.id === selectedBranchId)?.name}
                />
              ) : (
                <div className="rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-500">
                  <p className="text-xs">Vui lòng chọn chi nhánh ở góc trên để theo dõi sổ công nợ nhà cung cấp.</p>
                </div>
              )}
            </motion.div>
          )}

          {activeTab === 'issues' && (
            <motion.div
              key="tab-issues"
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.15 }}
            >
              {selectedBranchId ? (
                <IssueListTab
                  entityId={selectedEntityId}
                  branchId={selectedBranchId}
                  branchName={branches.find((b) => b.id === selectedBranchId)?.name}
                  items={items}
                />
              ) : (
                <div className="rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-500">
                  <p className="text-xs">Vui lòng chọn chi nhánh ở góc trên để xem và lập phiếu xuất kho.</p>
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}
export default InventoryCatalogPage
