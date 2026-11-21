import React, { useState, useEffect } from 'react'
import {
  Calendar,
  FileSpreadsheet,
  Plus,
  Store,
  AlertCircle,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { useAuthStore } from '@/features/auth/authStore'
import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import { DailyOverviewCalendarTab } from '../components/DailyOverviewCalendarTab'
import { DailyRevenueListTab } from '../components/DailyRevenueListTab'
import { CreateDailyRevenueModal } from '../components/CreateDailyRevenueModal'

interface BranchSummary {
  id: string
  code: string
  name: string
  status: string
}

type TabType = 'calendar' | 'list'

export function DailyRevenuePage() {
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const activeBranchName = useAuthStore((state) => state.activeBranchName)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  const [activeTab, setActiveTab] = useState<TabType>('calendar')
  const [branches, setBranches] = useState<BranchSummary[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '')
  const [createModalOpen, setCreateModalOpen] = useState<boolean>(false)

  // Fetch branches for branch switcher
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
      .catch(() => {
        // Fallback gracefully
      })
    return () => {
      isMounted = false
    }
  }, [activeBranchId, setActiveBranch])

  // Sync when activeBranchId changes in authStore
  useEffect(() => {
    if (activeBranchId) {
      setSelectedBranchId(activeBranchId)
    }
  }, [activeBranchId])

  const handleBranchChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const bId = e.target.value
    setSelectedBranchId(bId)
    const found = branches.find((b) => b.id === bId)
    setActiveBranch(bId, found ? found.name : '')
  }

  const currentBranchName =
    branches.find((b) => b.id === selectedBranchId)?.name || activeBranchName || 'Chi nhánh'

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <h1 className="text-2xl font-extrabold tracking-tight text-[#352D26]">
              Doanh thu hàng ngày
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-[#786C60] mt-1">
            Ghi nhận doanh thu tổng hợp cuối ca/ngày, đối soát kênh thanh toán và quản lý lịch kinh doanh chi nhánh.
          </p>
        </div>

        {/* Action */}
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            disabled={!selectedBranchId}
            onClick={() => setCreateModalOpen(true)}
            className="text-xs h-9 bg-[#B96849] hover:bg-[#A0573B] text-white shadow-sm font-semibold ml-1"
          >
            <Plus className="w-3.5 h-3.5 mr-1" />
            Nhập doanh thu ngày
          </Button>
        </div>
      </div>

      {/* Branch Selector & Tab Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-2.5 rounded-2xl border border-[#E5DED4] shadow-xs">
        {/* Branch Selector */}
        <div className="flex items-center space-x-2 px-2">
          <Store className="w-4 h-4 text-[#304638] shrink-0" />
          <span className="text-xs font-semibold text-[#786C60] shrink-0">Chi nhánh:</span>
          {branches.length > 0 ? (
            <select
              value={selectedBranchId}
              onChange={handleBranchChange}
              className="text-xs font-bold text-[#352D26] bg-[#F7F3EC] border border-[#E5DED4] rounded-xl px-3 py-1.5 focus:outline-hidden focus:ring-2 focus:ring-[#B96849]"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.code})
                </option>
              ))}
            </select>
          ) : (
            <span className="text-xs font-bold text-[#352D26]">{currentBranchName}</span>
          )}
        </div>

        {/* Tab Switcher */}
        <div className="flex items-center space-x-1 bg-[#F7F3EC] p-1 rounded-xl border border-[#E5DED4] self-start sm:self-auto">
          <button
            type="button"
            onClick={() => setActiveTab('calendar')}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === 'calendar'
                ? 'bg-white text-[#304638] shadow-xs'
                : 'text-[#786C60] hover:text-[#352D26]'
            }`}
          >
            <Calendar className="w-3.5 h-3.5" />
            <span>Lịch tổng quan ngày</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('list')}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === 'list'
                ? 'bg-white text-[#304638] shadow-xs'
                : 'text-[#786C60] hover:text-[#352D26]'
            }`}
          >
            <FileSpreadsheet className="w-3.5 h-3.5" />
            <span>Sổ báo cáo doanh thu</span>
          </button>
        </div>
      </div>

      {/* Main Tab Content */}
      {!selectedBranchId ? (
        <div className="p-12 text-center bg-white rounded-2xl border border-[#E5DED4] shadow-xs text-[#786C60] space-y-2">
          <AlertCircle className="w-8 h-8 text-[#B96849] mx-auto mb-2" />
          <p className="font-semibold text-sm text-[#352D26]">Vui lòng chọn chi nhánh kinh doanh</p>
          <p className="text-xs">Chọn chi nhánh từ thanh điều khiển phía trên để xem lịch và báo cáo doanh thu.</p>
        </div>
      ) : activeTab === 'calendar' ? (
        <DailyOverviewCalendarTab
          branchId={selectedBranchId}
          branchName={currentBranchName}
        />
      ) : (
        <DailyRevenueListTab
          branchId={selectedBranchId}
          branchName={currentBranchName}
        />
      )}

      {/* Quick Action Modal */}
      {selectedBranchId && (
        <CreateDailyRevenueModal
          isOpen={createModalOpen}
          branchId={selectedBranchId}
          branchName={currentBranchName}
          onClose={() => setCreateModalOpen(false)}
          onSuccess={() => {
            // Trigger refresh
            setCreateModalOpen(false)
          }}
        />
      )}
    </div>
  )
}
