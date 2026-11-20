import React, { useState, useEffect, useCallback } from 'react'
import {
  Store,
  RefreshCw,
  AlertCircle,
  Plus,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { organizationApi } from '@/features/organization/organizationApi'
import { openingApi } from '../api/openingApi'
import { OpeningInventoryStatusBanner } from './OpeningInventoryStatusBanner'
import { StockBalancesTable } from './StockBalancesTable'
import { OpeningInventoryWizardModal } from './OpeningInventoryWizardModal'
import type { BranchDto } from '@/features/organization/types'
import type { BranchOpeningStatusDto, CommitOpeningResponse } from '../types/opening.types'

interface OpeningInventoryTabProps {
  entityId: string
  initialBranchId?: string
}

export const OpeningInventoryTab: React.FC<OpeningInventoryTabProps> = ({
  entityId,
  initialBranchId,
}) => {
  // Branches state
  const [branches, setBranches] = useState<BranchDto[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(initialBranchId || '')
  const [isBranchesLoading, setIsBranchesLoading] = useState(false)
  const [branchesError, setBranchesError] = useState<string | null>(null)

  // Opening status state
  const [openingStatus, setOpeningStatus] = useState<BranchOpeningStatusDto | null>(null)
  const [isStatusLoading, setIsStatusLoading] = useState(false)
  const [statusError, setStatusError] = useState<string | null>(null)

  // Wizard modal state
  const [isWizardOpen, setIsWizardOpen] = useState(false)

  // 1. Fetch branches for selected entity
  const fetchBranches = useCallback(async () => {
    if (!entityId) return
    setIsBranchesLoading(true)
    setBranchesError(null)
    try {
      const data = await organizationApi.getBranches(entityId)
      setBranches(data)
      if (data.length > 0) {
        // Keep current selected if valid, otherwise select first branch
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
          'Không thể tải danh sách chi nhánh của pháp nhân.'
      )
    } finally {
      setIsBranchesLoading(false)
    }
  }, [entityId, selectedBranchId])

  useEffect(() => {
    fetchBranches()
  }, [fetchBranches])

  // 2. Fetch opening status for selected branch
  const fetchOpeningStatus = useCallback(async () => {
    if (!selectedBranchId) {
      setOpeningStatus(null)
      return
    }
    setIsStatusLoading(true)
    setStatusError(null)
    try {
      const data = await openingApi.getOpeningStatus(selectedBranchId)
      setOpeningStatus(data)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setStatusError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải trạng thái mở sổ của chi nhánh.'
      )
    } finally {
      setIsStatusLoading(false)
    }
  }, [selectedBranchId])

  useEffect(() => {
    fetchOpeningStatus()
  }, [fetchOpeningStatus])

  const activeBranch = branches.find((b) => b.id === selectedBranchId)

  const handleOpeningCommitted = (_res: CommitOpeningResponse) => {
    fetchOpeningStatus()
  }

  return (
    <div className="space-y-6">
      {/* Branch Selector Toolbar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-800 text-white shadow-xs shrink-0">
            <Store className="h-5 w-5" />
          </span>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-slate-500">Chi nhánh thao tác:</span>
              {isBranchesLoading ? (
                <span className="text-xs text-slate-400">Đang tải chi nhánh...</span>
              ) : branches.length === 0 ? (
                <span className="text-xs text-amber-600 font-medium">Chưa có chi nhánh nào</span>
              ) : (
                <select
                  value={selectedBranchId}
                  onChange={(e) => setSelectedBranchId(e.target.value)}
                  className="text-xs font-bold text-slate-900 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1 focus:outline-none focus:ring-1 focus:ring-emerald-700 cursor-pointer"
                >
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      [{b.branchCode}] {b.name}
                    </option>
                  ))}
                </select>
              )}
            </div>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Số dư tồn kho và giá vốn Moving WAC được cô lập và tính toán độc lập theo từng chi nhánh.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto shrink-0">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              fetchBranches()
              fetchOpeningStatus()
            }}
            disabled={isBranchesLoading || isStatusLoading}
            className="h-8 text-xs gap-1.5 rounded-xl border-slate-200"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isStatusLoading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </Button>

          {activeBranch && !openingStatus?.isOpeningCommitted && (
            <Button
              size="sm"
              onClick={() => setIsWizardOpen(true)}
              className="h-8 text-xs gap-1.5 rounded-xl bg-[#304638] hover:bg-[#25372C] text-white shadow-xs"
            >
              <Plus className="h-3.5 w-3.5" />
              <span>Khởi tạo tồn kho</span>
            </Button>
          )}
        </div>
      </div>

      {/* Errors */}
      {(branchesError || statusError) && (
        <div className="p-3.5 rounded-xl border border-red-200 bg-red-50 text-red-800 flex items-center gap-2.5 text-xs">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
          <span>{branchesError || statusError}</span>
        </div>
      )}

      {/* Opening Status Banner */}
      {selectedBranchId ? (
        <OpeningInventoryStatusBanner
          status={openingStatus}
          isLoading={isStatusLoading}
          onOpenWizard={() => setIsWizardOpen(true)}
        />
      ) : (
        <div className="p-8 text-center text-slate-500 bg-white rounded-2xl border border-slate-200">
          <p className="text-xs">Vui lòng chọn một chi nhánh để quản lý tồn kho đầu kỳ và số dư WAC.</p>
        </div>
      )}

      {/* Stock Balances Table */}
      {selectedBranchId && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-slate-900">
                Sổ Số Dư Tồn Kho & Giá Vốn Moving WAC Hiện Hành
              </h3>
              <Badge variant="outline" className="border-slate-200 text-slate-600 font-mono text-[10px]">
                BR-INV-08
              </Badge>
            </div>
          </div>

          <StockBalancesTable
            branchId={selectedBranchId}
            branchName={activeBranch?.name}
            isOpeningCommitted={openingStatus?.isOpeningCommitted}
            onOpenOpeningWizard={() => setIsWizardOpen(true)}
          />
        </div>
      )}

      {/* Opening Inventory Wizard Modal */}
      {isWizardOpen && selectedBranchId && (
        <OpeningInventoryWizardModal
          isOpen={isWizardOpen}
          onClose={() => setIsWizardOpen(false)}
          branchId={selectedBranchId}
          branchName={activeBranch?.name || selectedBranchId}
          openingStatus={openingStatus}
          onOpeningCommitted={handleOpeningCommitted}
        />
      )}
    </div>
  )
}
export default OpeningInventoryTab
