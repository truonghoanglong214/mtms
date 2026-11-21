import React, { useState, useEffect, useCallback } from 'react';
import {
  ClipboardCheck,
  Plus,
  Search,
  CheckCircle2,
  TrendingUp,
  TrendingDown,
  RefreshCw,
  Eye,
  ShieldCheck,
  Building2
} from 'lucide-react';
import { useAuthStore } from '@/features/auth/authStore';
import { apiClient } from '@/shared/api/apiClient';
import { stockCountApi } from '../api/stockCountApi';
import {
  CreateStockCountRequest,
  StockCountDetailDto,
  StockCountStatus,
  StockCountSummaryDto,
  StockCountType,
  STOCK_COUNT_STATUS_META,
  STOCK_COUNT_TYPE_META,
  UpdateCountLineRequest
} from '../types/stockCount.types';
import { CreateStockCountModal } from '../components/CreateStockCountModal';
import { StockCountDetailModal } from '../components/StockCountDetailModal';
import { ApproveStockCountModal } from '../components/ApproveStockCountModal';

interface BranchOption {
  id: string;
  name: string;
  code?: string;
  businessEntityId?: string;
}

export const StockCountManagementPage: React.FC = () => {
  const activeBranchId = useAuthStore((state) => state.activeBranchId);
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch);

  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '');

  const [counts, setCounts] = useState<StockCountDetailDto[]>([]);
  const [summary, setSummary] = useState<StockCountSummaryDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StockCountStatus | ''>('');
  const [typeFilter, setTypeFilter] = useState<StockCountType | ''>('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedCount, setSelectedCount] = useState<StockCountDetailDto | null>(null);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);

  // Fetch branches
  useEffect(() => {
    const fetchBranches = async () => {
      try {
        const res = await apiClient.get<BranchOption[]>('/branches');
        const list = Array.isArray(res.data) ? res.data : (res.data as unknown as { data: BranchOption[] })?.data || [];
        setBranches(list);
        if (list.length > 0 && !selectedBranchId) {
          const initial = activeBranchId || list[0].id;
          setSelectedBranchId(initial);
          const found = list.find((b) => b.id === initial);
          setActiveBranch(initial, found?.name || null);
        }
      } catch {
        // Non-blocking fallback
      }
    };
    fetchBranches();
  }, [activeBranchId, selectedBranchId, setActiveBranch]);

  const handleBranchChange = (newBranchId: string) => {
    setSelectedBranchId(newBranchId);
    const found = branches.find((b) => b.id === newBranchId);
    setActiveBranch(newBranchId, found?.name || null);
  };

  const branchId = selectedBranchId;
  const activeBranchName = branches.find((b) => b.id === selectedBranchId)?.name || 'Chi nhánh';

  const fetchSummary = useCallback(async () => {
    if (!branchId) return;
    try {
      const data = await stockCountApi.getStockCountSummary(branchId);
      setSummary(data);
    } catch {
      // Non-blocking
    }
  }, [branchId]);

  const fetchCounts = useCallback(async () => {
    if (!branchId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await stockCountApi.getStockCounts(branchId, {
        status: statusFilter ? statusFilter : undefined,
        countType: typeFilter ? typeFilter : undefined,
        startDate: startDate ? startDate : undefined,
        endDate: endDate ? endDate : undefined,
        search: search.trim() ? search.trim() : undefined,
        page,
        size: 15
      });
      setCounts(res.items);
      setTotalPages(res.totalPages);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lỗi khi tải danh sách kiểm kê';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [branchId, statusFilter, typeFilter, startDate, endDate, search, page]);

  useEffect(() => {
    fetchCounts();
    fetchSummary();
  }, [fetchCounts, fetchSummary]);

  const handleCreateSubmit = async (request: CreateStockCountRequest) => {
    if (!branchId) return;
    const created = await stockCountApi.createStockCount(branchId, request);
    fetchCounts();
    fetchSummary();
    setSelectedCount(created);
    setIsDetailModalOpen(true);
  };

  const handleUpdateCounts = async (countId: string, lines: UpdateCountLineRequest[]) => {
    if (!branchId) return;
    const updated = await stockCountApi.updateCounts(branchId, countId, { lines });
    setSelectedCount(updated);
    fetchCounts();
    fetchSummary();
  };

  const handleSubmitCount = async (countId: string) => {
    if (!branchId) return;
    const updated = await stockCountApi.submitStockCount(branchId, countId);
    setSelectedCount(updated);
    fetchCounts();
    fetchSummary();
  };

  const handleApproveCount = async (countId: string) => {
    if (!branchId) return;
    const updated = await stockCountApi.approveAndPostStockCount(branchId, countId);
    setSelectedCount(updated);
    fetchCounts();
    fetchSummary();
  };

  const handleCancelCount = async (countId: string) => {
    if (!branchId) return;
    await stockCountApi.cancelStockCount(branchId, countId);
    setIsDetailModalOpen(false);
    fetchCounts();
    fetchSummary();
  };

  const handleOpenDetail = (count: StockCountDetailDto) => {
    setSelectedCount(count);
    setIsDetailModalOpen(true);
  };

  const handleOpenApprove = (count: StockCountDetailDto) => {
    setSelectedCount(count);
    setIsApproveModalOpen(true);
  };

  const formatVnd = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  if (!branchId) {
    return (
      <div className="p-8 text-center text-slate-400">
        Vui lòng chọn một chi nhánh hoạt động để quản lý kiểm kê kho.
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded-xl">
              <ClipboardCheck className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-100 tracking-tight">
                Kiểm Kê Kho & Đối Soát Thực Tế
              </h1>
              <p className="text-xs text-slate-400 mt-0.5">
                Chi nhánh: <span className="text-blue-400 font-semibold">{activeBranchName}</span> • Tuân thủ rào cản phê duyệt Chủ sở hữu
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-200">
            <Building2 className="h-4 w-4 text-slate-400" />
            <select
              value={selectedBranchId}
              onChange={(e) => handleBranchChange(e.target.value)}
              className="bg-transparent text-sm font-medium focus:outline-none text-slate-200"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id} className="bg-slate-900 text-slate-200">
                  {b.name} ({b.code || 'BR'})
                </option>
              ))}
            </select>
          </div>
          <button
            onClick={() => {
              fetchCounts();
              fetchSummary();
            }}
            disabled={loading}
            className="p-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 rounded-xl transition-colors disabled:opacity-50"
            title="Tải lại dữ liệu"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="px-4 py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold rounded-xl shadow-lg shadow-blue-600/20 flex items-center gap-2 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo đợt kiểm kê</span>
          </button>
        </div>
      </div>

      {/* Governance Banner */}
      <div className="p-4 bg-slate-800/40 border border-slate-700/80 rounded-xl flex items-start gap-3.5">
        <div className="p-2 bg-blue-500/10 text-blue-400 rounded-lg shrink-0 mt-0.5">
          <ShieldCheck className="w-5 h-5" />
        </div>
        <div className="text-xs space-y-1">
          <span className="font-semibold text-slate-200">
            Nguyên tắc Quản trị Kho Hàng (Chênh lệch cần Chủ sở hữu duyệt mới hạch toán):
          </span>
          <p className="text-slate-400 leading-relaxed">
            Việc lập phiếu kiểm kê hoặc nộp biên bản đếm thừa/thiếu là bước ghi nhận hiện trạng thực tế.
            Số dư tồn kho trên sổ sách và giá vốn Moving WAC <strong>hoàn toàn không thay đổi</strong> cho đến khi có sự phê duyệt của Chủ sở hữu (Owner).
            Khi Owner duyệt, hệ thống sẽ tự động hạch toán các bút toán điều chỉnh `ADJUSTMENT` và khóa biên bản vĩnh viễn (immutable).
          </p>
        </div>
      </div>

      {/* 4 Summary KPI Cards */}
      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="p-4 bg-slate-800/50 border border-slate-700/60 rounded-xl">
            <span className="text-xs text-slate-400 block mb-1">Tổng đợt kiểm kê</span>
            <div className="text-2xl font-extrabold text-slate-100">{summary.totalCounts}</div>
            <span className="text-[11px] text-slate-500 mt-1 block">Toàn bộ lịch sử chi nhánh</span>
          </div>

          <div className="p-4 bg-slate-800/50 border border-slate-700/60 rounded-xl">
            <span className="text-xs text-amber-400 font-medium block mb-1">Chờ duyệt</span>
            <div className="text-2xl font-extrabold text-amber-400">{summary.pendingApprovalCount}</div>
            <span className="text-[11px] text-slate-500 mt-1 block">Bản nháp & Đang chờ duyệt</span>
          </div>

          <div className="p-4 bg-slate-800/50 border border-slate-700/60 rounded-xl">
            <div className="flex items-center gap-1.5 text-xs text-emerald-400 font-medium mb-1">
              <TrendingUp className="w-3.5 h-3.5" />
              <span>Tổng giá trị THỪA</span>
            </div>
            <div className="text-xl font-extrabold text-emerald-400">+{formatVnd(summary.totalSurplusValue)}</div>
            <span className="text-[11px] text-slate-500 mt-1 block">Các đợt đã chốt sổ</span>
          </div>

          <div className="p-4 bg-slate-800/50 border border-slate-700/60 rounded-xl">
            <div className="flex items-center gap-1.5 text-xs text-rose-400 font-medium mb-1">
              <TrendingDown className="w-3.5 h-3.5" />
              <span>Tổng giá trị THIẾU</span>
            </div>
            <div className="text-xl font-extrabold text-rose-400">-{formatVnd(summary.totalShortageValue)}</div>
            <span className="text-[11px] text-slate-500 mt-1 block">Các đợt đã chốt sổ</span>
          </div>
        </div>
      )}

      {/* Filters Toolbar */}
      <div className="p-4 bg-slate-800/40 border border-slate-800 rounded-xl flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-3 flex-1 min-w-[280px]">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã phiếu, ghi chú..."
              className="w-full pl-9 pr-3 py-2 bg-slate-900/80 border border-slate-700/80 rounded-lg text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StockCountStatus | '')}
            className="px-3 py-2 bg-slate-900/80 border border-slate-700/80 rounded-lg text-xs text-slate-300 focus:outline-none focus:border-blue-500"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="SUBMITTED">Chờ duyệt</option>
            <option value="POSTED">Đã chốt sổ (POSTED)</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>

          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value as StockCountType | '')}
            className="px-3 py-2 bg-slate-900/80 border border-slate-700/80 rounded-lg text-xs text-slate-300 focus:outline-none focus:border-blue-500"
          >
            <option value="">Tất cả hình thức</option>
            <option value="PERIODIC">Định kỳ</option>
            <option value="SURPRISE">Đột xuất</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="px-2.5 py-1.5 bg-slate-900/80 border border-slate-700/80 rounded-lg text-xs text-slate-300 focus:outline-none focus:border-blue-500"
          />
          <span className="text-slate-500 text-xs">-</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="px-2.5 py-1.5 bg-slate-900/80 border border-slate-700/80 rounded-lg text-xs text-slate-300 focus:outline-none focus:border-blue-500"
          />
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm">Đang tải danh sách kiểm kê...</div>
        ) : error ? (
          <div className="p-8 text-center text-rose-400 text-sm">{error}</div>
        ) : counts.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-sm">
            Chưa có đợt kiểm kê nào phù hợp với bộ lọc. Bấm "Tạo đợt kiểm kê" để bắt đầu.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-800/80 text-slate-400 font-medium border-b border-slate-700">
                <tr>
                  <th className="py-3 px-4">Mã phiếu</th>
                  <th className="py-3 px-4">Ngày kiểm kê</th>
                  <th className="py-3 px-3">Hình thức</th>
                  <th className="py-3 px-3 text-center">SL mặt hàng</th>
                  <th className="py-3 px-4 text-right">Lệch thừa / thiếu</th>
                  <th className="py-3 px-4 text-right">Chênh lệch ròng</th>
                  <th className="py-3 px-4 text-center">Trạng thái</th>
                  <th className="py-3 px-4">Người lập</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {counts.map((c) => {
                  const statusMeta = STOCK_COUNT_STATUS_META[c.status];
                  const typeMeta = STOCK_COUNT_TYPE_META[c.countType];

                  return (
                    <tr key={c.id} className="hover:bg-slate-800/30 transition-colors">
                      <td className="py-3 px-4 font-mono font-bold text-slate-200">
                        {c.countNumber}
                      </td>
                      <td className="py-3 px-4 text-slate-300">
                        {c.countedOn}
                      </td>
                      <td className="py-3 px-3">
                        <span className={`text-[10px] font-medium px-2 py-0.5 rounded-full border ${typeMeta.badge}`}>
                          {typeMeta.label}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-center text-slate-200 font-semibold">
                        {c.totalItems}
                      </td>
                      <td className="py-3 px-4 text-right font-mono">
                        <div className="text-emerald-400 font-medium">+{c.totalSurplusQty}</div>
                        <div className="text-rose-400 font-medium">-{c.totalShortageQty}</div>
                      </td>
                      <td className="py-3 px-4 text-right font-mono font-bold">
                        <span className={c.netVarianceValue >= 0 ? 'text-emerald-400' : 'text-rose-400'}>
                          {c.netVarianceValue >= 0 ? '+' : ''}{formatVnd(c.netVarianceValue)}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-center">
                        <span className={`text-[11px] font-medium px-2.5 py-0.5 rounded-full border ${statusMeta.bg} ${statusMeta.text} ${statusMeta.border}`}>
                          {statusMeta.label}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-slate-400">
                        {c.createdByName}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleOpenDetail(c)}
                            className="p-1.5 text-blue-400 hover:text-blue-300 hover:bg-blue-500/10 rounded-lg transition-colors flex items-center gap-1"
                            title="Xem chi tiết & Cập nhật số đếm"
                          >
                            <Eye className="w-4 h-4" />
                            <span className="text-[11px] font-medium">Chi tiết</span>
                          </button>

                          {c.status === 'SUBMITTED' && (
                            <button
                              onClick={() => handleOpenApprove(c)}
                              className="px-2.5 py-1 text-xs font-semibold text-emerald-300 hover:text-white bg-emerald-500/10 hover:bg-emerald-600 border border-emerald-500/30 rounded-lg transition-all flex items-center gap-1"
                              title="Duyệt chốt kho (Owner)"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Duyệt</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-800 bg-slate-900/40 text-xs text-slate-400">
            <span>Trang {page + 1} / {totalPages}</span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-50 rounded text-slate-300"
              >
                Trước
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 bg-slate-800 hover:bg-slate-700 disabled:opacity-50 rounded text-slate-300"
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateStockCountModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSubmit={handleCreateSubmit}
        branchName={activeBranchName}
      />

      <StockCountDetailModal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        count={selectedCount}
        onUpdateCounts={handleUpdateCounts}
        onSubmitCount={handleSubmitCount}
        onOpenApproveModal={() => {
          setIsDetailModalOpen(false);
          setIsApproveModalOpen(true);
        }}
        onCancelCount={handleCancelCount}
      />

      <ApproveStockCountModal
        isOpen={isApproveModalOpen}
        onClose={() => setIsApproveModalOpen(false)}
        count={selectedCount}
        onApprove={handleApproveCount}
      />
    </div>
  );
};

export default StockCountManagementPage;
