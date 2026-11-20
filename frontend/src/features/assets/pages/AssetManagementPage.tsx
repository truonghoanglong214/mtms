import React, { useState, useEffect, useCallback } from 'react';
import {
  Package,
  Plus,
  Search,
  DollarSign,
  Activity,
  ShieldAlert,
  ShieldCheck,
  ShieldX,
  Eye,
  Edit2,
  Trash2,
  Loader2,
  Info,
  Building2,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { useAuthStore } from '@/features/auth/authStore';
import { apiClient } from '@/shared/api/apiClient';
import { assetApi } from '../api/assetApi';
import {
  ASSET_CATEGORY_LABELS,
  ASSET_STATUS_LABELS,
  AssetCategory,
  AssetStatus,
  AssetSummary,
  EquipmentAsset,
} from '../types/asset.types';
import { CreateAssetModal } from '../components/CreateAssetModal';
import { EditAssetModal } from '../components/EditAssetModal';
import { AssetDetailModal } from '../components/AssetDetailModal';

interface BranchOption {
  id: string;
  name: string;
  code?: string;
}

export const AssetManagementPage: React.FC = () => {
  const activeBranchId = useAuthStore((state) => state.activeBranchId);
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch);

  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '');

  const [assets, setAssets] = useState<EquipmentAsset[]>([]);
  const [summary, setSummary] = useState<AssetSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [selectedAsset, setSelectedAsset] = useState<EquipmentAsset | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  // Load branches
  useEffect(() => {
    const fetchBranches = async () => {
      try {
        const res = await apiClient.get<BranchOption[]>('/branches');
        const list = Array.isArray(res.data) ? res.data : (res.data as any)?.data || [];
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

  const currentBranch = selectedBranchId || activeBranchId || '';

  const fetchSummary = useCallback(async () => {
    if (!currentBranch) return;
    try {
      const data = await assetApi.getAssetSummary(currentBranch);
      setSummary(data);
    } catch (err) {
      console.error('Failed to load asset summary:', err);
    }
  }, [currentBranch]);

  const fetchAssets = useCallback(async () => {
    if (!currentBranch) return;
    setLoading(true);
    try {
      const res = await assetApi.getAssets(currentBranch, {
        category: categoryFilter !== 'ALL' ? (categoryFilter as AssetCategory) : undefined,
        status: statusFilter !== 'ALL' ? (statusFilter as AssetStatus) : undefined,
        search: search.trim() || undefined,
        page,
        size: 15,
      });
      setAssets(res.content);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error('Failed to load assets:', err);
    } finally {
      setLoading(false);
    }
  }, [currentBranch, categoryFilter, statusFilter, search, page]);

  useEffect(() => {
    if (currentBranch) {
      fetchSummary();
      fetchAssets();
    }
  }, [currentBranch, fetchSummary, fetchAssets]);

  const handleBranchChange = (newBranchId: string) => {
    const found = branches.find((b) => b.id === newBranchId);
    setSelectedBranchId(newBranchId);
    setActiveBranch(newBranchId, found?.name || null);
    setPage(0);
  };

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xóa tài sản "${name}"? Hành động này không thể hoàn tác.`)) {
      return;
    }
    setDeletingId(id);
    try {
      await assetApi.deleteAsset(currentBranch, id);
      await fetchSummary();
      await fetchAssets();
    } catch (err) {
      console.error('Failed to delete asset:', err);
      alert('Không thể xóa tài sản. Vui lòng thử lại.');
    } finally {
      setDeletingId(null);
    }
  };

  const getWarrantyBadge = (warrantyUntil?: string | null) => {
    if (!warrantyUntil) {
      return (
        <span className="text-xs text-slate-400">
          Chưa rõ
        </span>
      );
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const date = new Date(warrantyUntil);
    date.setHours(0, 0, 0, 0);

    const diffDays = Math.ceil((date.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
      return (
        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-rose-600 dark:text-rose-400">
          <ShieldX className="w-3.5 h-3.5" /> Hết hạn
        </span>
      );
    } else if (diffDays <= 30) {
      return (
        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-amber-600 dark:text-amber-400">
          <ShieldAlert className="w-3.5 h-3.5" /> Còn {diffDays} ngày
        </span>
      );
    } else {
      return (
        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
          <ShieldCheck className="w-3.5 h-3.5" /> Còn hạn
        </span>
      );
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header with Branch Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2.5 rounded-xl bg-primary/10 text-primary">
              <Package className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
                Sổ Tài sản & Thiết bị (TC-051)
              </h1>
              <p className="text-sm text-slate-500 dark:text-slate-400">
                Đăng ký danh mục máy móc, số serial, bảo hành và nguyên giá ban đầu
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          {branches.length > 0 && (
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-xs">
              <Building2 className="w-4 h-4 text-primary shrink-0" />
              <select
                value={selectedBranchId}
                onChange={(e) => handleBranchChange(e.target.value)}
                className="bg-transparent font-medium text-slate-700 dark:text-slate-200 focus:outline-none"
              >
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          <Button
            onClick={() => setIsCreateOpen(true)}
            className="gap-2 shadow-sm"
          >
            <Plus className="w-4 h-4" />
            Đăng ký thiết bị
          </Button>
        </div>
      </div>

      {/* TC-051 / ADR-023 Business Disclaimer Banner */}
      <div className="p-4 rounded-2xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/50 flex items-start gap-3.5">
        <div className="p-2 rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5">
          <Info className="w-5 h-5" />
        </div>
        <div className="space-y-1">
          <h4 className="text-sm font-bold text-amber-900 dark:text-amber-200">
            Ranh giới nghiệp vụ Phase 0 (ADR-023 & TC-051)
          </h4>
          <p className="text-xs text-amber-800/90 dark:text-amber-300/90 leading-relaxed">
            Hệ thống MTMS giai đoạn P0 quản lý nguyên giá và hồ sơ bảo hành thiết bị phục vụ kiểm kê hiện vật. Theo quyết định <strong>ADR-023</strong> và test case <strong>TC-051</strong>, <strong>chi phí khấu hao tài sản chưa được trích vào Báo cáo P&L quản trị tạm tính</strong> (P0 P&L clearly excludes depreciation). Chính sách khấu hao tự động sẽ được kích hoạt ở Phase 1 (P1-AST-02 / TC-054) sau khi có phê duyệt quy chế kế toán.
          </p>
        </div>
      </div>

      {/* 4 Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Total Assets */}
        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Tổng thiết bị</span>
            <Package className="w-4 h-4 text-primary" />
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary?.totalAssets ?? 0}
          </div>
          <span className="text-xs text-slate-400 mt-1 block">
            Thiết bị đang quản lý tại quán
          </span>
        </div>

        {/* Card 2: Total Acquisition Cost */}
        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Tổng nguyên giá</span>
            <DollarSign className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
            {Number(summary?.totalAcquisitionCost ?? 0).toLocaleString('vi-VN')} đ
          </div>
          <span className="text-xs text-slate-400 mt-1 block">
            Nguyên giá mua sắm ban đầu
          </span>
        </div>

        {/* Card 3: Active In Use */}
        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Đang hoạt động</span>
            <Activity className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl font-bold text-slate-900 dark:text-white">
            {summary?.inUseCount ?? 0}
            <span className="text-xs font-normal text-slate-400 ml-2">
              ({summary?.inRepairCount ?? 0} đang sửa)
            </span>
          </div>
          <span className="text-xs text-slate-400 mt-1 block">
            Máy móc sẵn sàng phục vụ
          </span>
        </div>

        {/* Card 4: Warranty Alert */}
        <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 dark:text-slate-400 mb-2">
            <span className="text-xs font-medium uppercase tracking-wider">Cảnh báo bảo hành</span>
            <ShieldAlert className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-2xl font-bold text-amber-600 dark:text-amber-400">
            {summary?.warrantyExpiringCount ?? 0}
            <span className="text-xs font-normal text-rose-500 ml-2">
              ({summary?.warrantyExpiredCount ?? 0} hết hạn)
            </span>
          </div>
          <span className="text-xs text-slate-400 mt-1 block">
            Sắp hết hạn trong vòng 30 ngày
          </span>
        </div>
      </div>

      {/* Search & Filter Bar */}
      <div className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs flex flex-col md:flex-row items-center gap-3">
        <div className="relative flex-1 w-full">
          <Search className="absolute left-3.5 top-2.5 w-4 h-4 text-slate-400" />
          <input
            type="text"
            placeholder="Tìm theo mã tài sản, tên thiết bị, serial hoặc model..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="w-full pl-10 pr-4 py-2 text-sm rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/50 dark:bg-slate-800/50 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto">
          <div className="w-full md:w-56">
            <select
              value={categoryFilter}
              onChange={(e) => {
                setCategoryFilter(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/50 dark:bg-slate-800/50 text-slate-900 dark:text-white"
            >
              <option value="ALL">Tất cả nhóm tài sản</option>
              {Object.entries(ASSET_CATEGORY_LABELS).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div className="w-full md:w-48">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-slate-200 dark:border-slate-700 bg-slate-50/50 dark:bg-slate-800/50 text-slate-900 dark:text-white"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="IN_USE">Đang hoạt động</option>
              <option value="IN_REPAIR">Đang sửa chữa</option>
              <option value="STORED">Lưu kho</option>
              <option value="DISPOSED">Đã thanh lý</option>
              <option value="TRANSFERRED">Đã điều chuyển</option>
            </select>
          </div>
        </div>
      </div>

      {/* Assets Table */}
      <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600 dark:text-slate-300">
            <thead className="text-xs uppercase bg-slate-50/80 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400 border-b border-slate-100 dark:border-slate-800 font-semibold tracking-wider">
              <tr>
                <th className="px-5 py-3.5">Mã & Tên tài sản</th>
                <th className="px-4 py-3.5">Nhóm thiết bị</th>
                <th className="px-4 py-3.5">Serial / Model</th>
                <th className="px-4 py-3.5">Vị trí</th>
                <th className="px-4 py-3.5 text-right">Nguyên giá (đ)</th>
                <th className="px-4 py-3.5 text-center">Bảo hành</th>
                <th className="px-4 py-3.5 text-center">Trạng thái</th>
                <th className="px-5 py-3.5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-primary" />
                    Đang tải danh sách tài sản...
                  </td>
                </tr>
              ) : assets.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <Package className="w-8 h-8 mx-auto mb-2 opacity-40" />
                    Chưa có thiết bị nào phù hợp với bộ lọc tìm kiếm.
                  </td>
                </tr>
              ) : (
                assets.map((item) => {
                  const statusMeta = ASSET_STATUS_LABELS[item.status] || {
                    label: item.status,
                    color: 'text-slate-600',
                    bg: 'bg-slate-100',
                  };

                  return (
                    <tr
                      key={item.id}
                      className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors"
                    >
                      <td className="px-5 py-3.5">
                        <div className="font-semibold text-slate-900 dark:text-white">
                          {item.name}
                        </div>
                        <span className="text-xs font-mono text-primary font-medium">
                          {item.assetCode}
                        </span>
                      </td>

                      <td className="px-4 py-3.5 text-xs text-slate-600 dark:text-slate-300">
                        {ASSET_CATEGORY_LABELS[item.category] || item.category}
                      </td>

                      <td className="px-4 py-3.5 text-xs">
                        <div className="font-mono text-slate-700 dark:text-slate-200">
                          {item.serialNumber || '—'}
                        </div>
                        <div className="text-[11px] text-slate-400">
                          {item.model || ''}
                        </div>
                      </td>

                      <td className="px-4 py-3.5 text-xs text-slate-600 dark:text-slate-400">
                        {item.locationInBranch || '—'}
                      </td>

                      <td className="px-4 py-3.5 text-xs font-semibold text-right text-emerald-600 dark:text-emerald-400">
                        {Number(item.acquisitionCost || 0).toLocaleString('vi-VN')}
                      </td>

                      <td className="px-4 py-3.5 text-center">
                        {getWarrantyBadge(item.warrantyUntil)}
                      </td>

                      <td className="px-4 py-3.5 text-center">
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold border ${statusMeta.bg} ${statusMeta.color}`}
                        >
                          {statusMeta.label}
                        </span>
                      </td>

                      <td className="px-5 py-3.5 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            title="Xem chi tiết"
                            onClick={() => {
                              setSelectedAsset(item);
                              setIsDetailOpen(true);
                            }}
                            className="p-1.5 text-slate-400 hover:text-primary hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            title="Chỉnh sửa"
                            onClick={() => {
                              setSelectedAsset(item);
                              setIsEditOpen(true);
                            }}
                            className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button
                            title="Xóa tài sản"
                            disabled={deletingId === item.id}
                            onClick={() => handleDelete(item.id, item.name)}
                            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
                          >
                            {deletingId === item.id ? (
                              <Loader2 className="w-4 h-4 animate-spin text-rose-500" />
                            ) : (
                              <Trash2 className="w-4 h-4" />
                            )}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-5 py-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
            <span>
              Hiển thị {assets.length} trên tổng số {totalElements} thiết bị
            </span>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Trước
              </Button>
              <span className="px-2 py-1">
                Trang {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Sau
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateAssetModal
        isOpen={isCreateOpen}
        branchId={currentBranch}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={() => {
          fetchSummary();
          fetchAssets();
        }}
      />

      <EditAssetModal
        isOpen={isEditOpen}
        branchId={currentBranch}
        asset={selectedAsset}
        onClose={() => {
          setIsEditOpen(false);
          setSelectedAsset(null);
        }}
        onSuccess={() => {
          fetchSummary();
          fetchAssets();
        }}
      />

      <AssetDetailModal
        isOpen={isDetailOpen}
        asset={selectedAsset}
        onClose={() => {
          setIsDetailOpen(false);
          setSelectedAsset(null);
        }}
        onEdit={(asset) => {
          setSelectedAsset(asset);
          setIsEditOpen(true);
        }}
      />
    </div>
  );
};
