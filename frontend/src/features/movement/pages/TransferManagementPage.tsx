import React, { useState, useEffect, useCallback } from 'react';
import {
  ArrowRightLeft,
  Truck,
  PackageCheck,
  AlertTriangle,
  Building2,
  RefreshCw,
  AlertCircle,
  Send
} from 'lucide-react';
import { useAuthStore } from '@/features/auth/authStore';
import { apiClient } from '@/shared/api/apiClient';
import { movementApi } from '../api/movementApi';
import { StockTransfer, TransferSummary } from '../types/movement.types';
import { OutboundTransfersTab } from '../components/OutboundTransfersTab';
import { InboundTransfersTab } from '../components/InboundTransfersTab';
import { DiscrepanciesTab } from '../components/DiscrepanciesTab';
import { CreateTransferModal } from '../components/CreateTransferModal';
import { ReceiveTransferModal } from '../components/ReceiveTransferModal';
import { TransferDetailModal } from '../components/TransferDetailModal';

interface BranchOption {
  id: string;
  name: string;
  code?: string;
  businessEntityId?: string;
}

export const TransferManagementPage: React.FC = () => {
  const activeBranchId = useAuthStore((state) => state.activeBranchId);
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch);
  const user = useAuthStore((state) => state.user);

  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '');

  const [activeTab, setActiveTab] = useState<'outbound' | 'inbound' | 'discrepancies'>('outbound');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [summary, setSummary] = useState<TransferSummary | null>(null);
  const [outboundTransfers, setOutboundTransfers] = useState<StockTransfer[]>([]);
  const [inboundTransfers, setInboundTransfers] = useState<StockTransfer[]>([]);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isReceiveOpen, setIsReceiveOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [selectedTransfer, setSelectedTransfer] = useState<StockTransfer | null>(null);

  // Fetch branches
  useEffect(() => {
    const fetchBranches = async () => {
      try {
        const res = await apiClient.get<BranchOption[]>('/branches');
        const list = Array.isArray(res.data) ? res.data : (res.data as unknown as { data: BranchOption[] })?.data || [];
        setBranches(list);
        if (list.length > 0 && !selectedBranchId) {
          const initial = activeBranchId || list[0].id;
          const found = list.find((b: BranchOption) => b.id === initial);
          setSelectedBranchId(initial);
          setActiveBranch(initial, found?.name || null);
        }
      } catch (err) {
        console.error('Failed to load branches:', err);
      }
    };
    fetchBranches();
  }, [activeBranchId, selectedBranchId, setActiveBranch]);

  const selectedBranch = branches.find((b) => b.id === selectedBranchId);
  const entityId =
    selectedBranch?.businessEntityId ||
    user?.grants?.[0]?.businessEntityId ||
    '00000000-0000-0000-0000-000000000001';

  const loadData = useCallback(async () => {
    if (!selectedBranchId) return;
    setLoading(true);
    setError(null);
    try {
      const [sumRes, outRes, inRes] = await Promise.all([
        movementApi.getTransferSummary(selectedBranchId),
        movementApi.getOutboundTransfers(selectedBranchId, { size: 50 }),
        movementApi.getInboundTransfers(selectedBranchId, { size: 50 })
      ]);

      setSummary(sumRes);
      setOutboundTransfers(outRes.items);
      setInboundTransfers(inRes.items);
    } catch (err: unknown) {
      console.error('Failed to load transfer data:', err);
      const message = err instanceof Error ? err.message : 'Lỗi khi tải dữ liệu điều chuyển kho';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [selectedBranchId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleBranchChange = (newBranchId: string) => {
    setSelectedBranchId(newBranchId);
    const found = branches.find((b) => b.id === newBranchId);
    setActiveBranch(newBranchId, found?.name || null);
  };

  const handleDispatch = async (transferId: string) => {
    try {
      await movementApi.dispatchTransfer(selectedBranchId, transferId);
      loadData();
      setIsDetailOpen(false);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Lỗi khi xuất kho điều chuyển';
      alert(message);
    }
  };

  const handleCancel = async (transferId: string) => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy phiếu điều chuyển nháp này?')) return;
    try {
      await movementApi.cancelTransfer(selectedBranchId, transferId);
      loadData();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Lỗi khi hủy phiếu điều chuyển';
      alert(message);
    }
  };

  const handleOpenReceive = (transfer: StockTransfer) => {
    setSelectedTransfer(transfer);
    setIsReceiveOpen(true);
  };

  const handleOpenDetail = (transfer: StockTransfer) => {
    setSelectedTransfer(transfer);
    setIsDetailOpen(true);
  };

  const formatVnd = (val?: number) => {
    if (val === undefined || val === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div className="space-y-6 p-6">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <ArrowRightLeft className="h-6 w-6 text-primary-600" />
            <h1 className="text-2xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
              Điều Chuyển Hàng Nội Bộ (TC-027 / TC-028)
            </h1>
          </div>
          <p className="mt-1 text-sm text-neutral-500">
            Quản lý xuất/nhận hàng giữa các chi nhánh cùng pháp nhân, snapshot Moving WAC và bảo lưu hao hụt
          </p>
        </div>

        {/* Branch Selector & Refresh */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 shadow-sm dark:border-neutral-700 dark:bg-neutral-800">
            <Building2 className="h-4 w-4 text-neutral-400" />
            <select
              value={selectedBranchId}
              onChange={(e) => handleBranchChange(e.target.value)}
              className="bg-transparent text-sm font-medium focus:outline-none dark:text-neutral-200"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} ({b.code || 'BR'})
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={() => loadData()}
            disabled={loading}
            className="flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-2 text-sm font-medium text-neutral-700 shadow-sm hover:bg-neutral-50 disabled:opacity-50 dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-300 dark:hover:bg-neutral-700"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>
      </div>

      {/* Error notification */}
      {error && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
          <AlertCircle className="h-5 w-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Metric Stat Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Card 1: Outbound */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Chuyển Đi (Outbound)</span>
            <div className="rounded-lg bg-blue-100 p-2 text-blue-600 dark:bg-blue-950 dark:text-blue-400">
              <Send className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
              {summary?.outboundCount || 0}
            </div>
            <p className="mt-1 text-xs text-neutral-500">
              Giá trị xuất: {formatVnd(summary?.totalDispatchedCost)}
            </p>
          </div>
        </div>

        {/* Card 2: In-Transit */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Đang Vận Chuyển</span>
            <div className="rounded-lg bg-amber-100 p-2 text-amber-600 dark:bg-amber-950 dark:text-amber-400">
              <Truck className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-amber-600 dark:text-amber-400">
              {summary?.inTransitCount || 0}
            </div>
            <p className="mt-1 text-xs text-neutral-500">
              Chờ chi nhánh nhận kiểm đếm
            </p>
          </div>
        </div>

        {/* Card 3: Inbound */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Nhận Về (Inbound)</span>
            <div className="rounded-lg bg-emerald-100 p-2 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-400">
              <PackageCheck className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
              {summary?.inboundCount || 0}
            </div>
            <p className="mt-1 text-xs text-neutral-500">
              Giá trị nhập: {formatVnd(summary?.totalReceivedCost)}
            </p>
          </div>
        </div>

        {/* Card 4: Discrepancies */}
        <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-neutral-500">Hao Hụt / Chênh Lệch (TC-027)</span>
            <div className="rounded-lg bg-rose-100 p-2 text-rose-600 dark:bg-rose-950 dark:text-rose-400">
              <AlertTriangle className="h-5 w-5" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold text-rose-600 dark:text-rose-400">
              {summary?.discrepancyCount || 0}
            </div>
            <p className="mt-1 text-xs text-neutral-500">
              Chuyến nhận một phần cần đối soát
            </p>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex border-b border-neutral-200 dark:border-neutral-800">
        <button
          onClick={() => setActiveTab('outbound')}
          className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
            activeTab === 'outbound'
              ? 'border-primary-600 text-primary-600 dark:border-primary-400 dark:text-primary-400'
              : 'border-transparent text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200'
          }`}
        >
          <Send className="h-4 w-4" />
          Chuyển đi (Outbound)
          <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
            {outboundTransfers.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('inbound')}
          className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
            activeTab === 'inbound'
              ? 'border-primary-600 text-primary-600 dark:border-primary-400 dark:text-primary-400'
              : 'border-transparent text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200'
          }`}
        >
          <PackageCheck className="h-4 w-4" />
          Nhận về (Inbound)
          <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
            {inboundTransfers.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('discrepancies')}
          className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-colors ${
            activeTab === 'discrepancies'
              ? 'border-primary-600 text-primary-600 dark:border-primary-400 dark:text-primary-400'
              : 'border-transparent text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200'
          }`}
        >
          <AlertTriangle className="h-4 w-4" />
          Đối soát Hao hụt (TC-027)
          <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-semibold text-rose-700 dark:bg-rose-950 dark:text-rose-300">
            {summary?.discrepancyCount || 0}
          </span>
        </button>
      </div>

      {/* Tab Panels */}
      {loading ? (
        <div className="flex h-64 items-center justify-center">
          <div className="flex items-center gap-2 text-sm text-neutral-500">
            <RefreshCw className="h-5 w-5 animate-spin" />
            Đang tải dữ liệu điều chuyển kho...
          </div>
        </div>
      ) : (
        <>
          {activeTab === 'outbound' && (
            <OutboundTransfersTab
              transfers={outboundTransfers}
              onOpenCreate={() => setIsCreateOpen(true)}
              onOpenDetail={handleOpenDetail}
              onDispatch={handleDispatch}
              onCancel={handleCancel}
            />
          )}

          {activeTab === 'inbound' && (
            <InboundTransfersTab
              transfers={inboundTransfers}
              onOpenDetail={handleOpenDetail}
              onOpenReceive={handleOpenReceive}
            />
          )}

          {activeTab === 'discrepancies' && (
            <DiscrepanciesTab
              transfers={[...outboundTransfers, ...inboundTransfers]}
              onOpenDetail={handleOpenDetail}
            />
          )}
        </>
      )}

      {/* Modals */}
      <CreateTransferModal
        fromBranchId={selectedBranchId}
        businessEntityId={entityId}
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={() => {
          setIsCreateOpen(false);
          loadData();
        }}
      />

      <ReceiveTransferModal
        branchId={selectedBranchId}
        transfer={selectedTransfer}
        isOpen={isReceiveOpen}
        onClose={() => {
          setIsReceiveOpen(false);
          setSelectedTransfer(null);
        }}
        onSuccess={() => {
          setIsReceiveOpen(false);
          setSelectedTransfer(null);
          loadData();
        }}
      />

      <TransferDetailModal
        transfer={selectedTransfer}
        isOpen={isDetailOpen}
        onClose={() => {
          setIsDetailOpen(false);
          setSelectedTransfer(null);
        }}
        onDispatch={(id) => {
          handleDispatch(id);
        }}
        onReceive={(t) => {
          setIsDetailOpen(false);
          handleOpenReceive(t);
        }}
      />
    </div>
  );
};
