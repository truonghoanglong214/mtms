import { useState, useEffect, useCallback } from 'react'
import {
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
import { catalogApi } from '../api/catalogApi'
import { IssueListTab } from '../components/IssueListTab'
import type { BusinessEntityDto, BranchDto } from '@/features/organization/types'
import type { CatalogItemDto } from '../types/catalog.types'

export function StockIssuePage() {
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

  // Catalog items for current entity
  const [items, setItems] = useState<CatalogItemDto[]>([])

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

  // 3. Fetch Items when entity changes
  useEffect(() => {
    if (!selectedEntityId) return
    let isMounted = true

    catalogApi
      .getCatalogItems(selectedEntityId)
      .then((data) => {
        if (isMounted) setItems(data)
      })
      .catch((err) => console.error('Failed to load catalog items for stock issue:', err))

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
              Xuất Kho Thủ Công & Định Giá Vốn Moving WAC
            </h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Lập phiếu xuất quầy bar pha chế hoặc tiêu hủy hao hụt. Giá vốn được chốt theo đơn giá bình quân di động (Moving WAC) và khóa chặt không cho phép xuất âm.
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
              <span className="text-xs text-rose-500">Không có pháp nhân</span>
            ) : (
              <select
                value={selectedEntityId}
                onChange={(e) => setSelectedEntityId(e.target.value)}
                className="text-xs font-semibold text-slate-800 bg-transparent border-none focus:outline-none cursor-pointer"
              >
                {entities.map((ent) => (
                  <option key={ent.id} value={ent.id}>
                    [{ent.entityCode}] {ent.registeredName}
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
                className="text-xs font-semibold text-slate-800 bg-transparent border-none focus:outline-none cursor-pointer"
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

      {/* Main Content */}
      {branchesError ? (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-800 flex items-center gap-3">
          <AlertCircle className="h-5 w-5 text-rose-600 shrink-0" />
          <span className="text-xs">{branchesError}</span>
        </div>
      ) : !selectedBranchId ? (
        <div className="rounded-xl border border-dashed border-slate-300 p-12 text-center text-slate-500 bg-white">
          <Package className="h-10 w-10 mx-auto text-slate-300 mb-2" />
          <p className="text-sm font-medium">Chưa chọn chi nhánh hoạt động</p>
          <p className="text-xs text-slate-400 mt-1">
            Vui lòng chọn một chi nhánh ở góc trên để theo dõi và lập phiếu xuất kho.
          </p>
        </div>
      ) : (
        <IssueListTab
          entityId={selectedEntityId}
          branchId={selectedBranchId}
          branchName={activeBranch?.name}
          items={items}
        />
      )}
    </div>
  )
}

export default StockIssuePage
