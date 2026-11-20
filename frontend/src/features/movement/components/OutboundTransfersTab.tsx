import React, { useState } from 'react';
import { Search, Plus, Eye, Send, XCircle } from 'lucide-react';
import { StockTransfer, TRANSFER_STATUS_LABELS } from '../types/movement.types';

interface Props {
  transfers: StockTransfer[];
  onOpenCreate: () => void;
  onOpenDetail: (transfer: StockTransfer) => void;
  onDispatch: (transferId: string) => void;
  onCancel: (transferId: string) => void;
}

export const OutboundTransfersTab: React.FC<Props> = ({
  transfers,
  onOpenCreate,
  onOpenDetail,
  onDispatch,
  onCancel
}) => {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const filtered = transfers.filter((t) => {
    const matchesSearch =
      t.transferNumber.toLowerCase().includes(search.toLowerCase()) ||
      t.toBranchName.toLowerCase().includes(search.toLowerCase()) ||
      (t.notes && t.notes.toLowerCase().includes(search.toLowerCase()));
    const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const formatVnd = (val?: number) => {
    if (val === undefined || val === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div className="space-y-4">
      {/* Control bar */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-neutral-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo số phiếu, chi nhánh nhận..."
              className="w-full rounded-lg border border-neutral-300 py-2 pl-9 pr-3 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="DISPATCHED">Đang vận chuyển</option>
            <option value="PARTIAL_RECEIPT">Nhận một phần</option>
            <option value="COMPLETED">Hoàn tất 100%</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>

        <button
          onClick={onOpenCreate}
          className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700"
        >
          <Plus className="h-4 w-4" />
          Tạo phiếu điều chuyển
        </button>
      </div>

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-neutral-200 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-neutral-200 bg-neutral-50 text-xs font-semibold text-neutral-500 dark:border-neutral-800 dark:bg-neutral-800 dark:text-neutral-400">
            <tr>
              <th className="px-4 py-3">Mã phiếu</th>
              <th className="px-4 py-3">Chi nhánh nhận</th>
              <th className="px-4 py-3">Ngày yêu cầu</th>
              <th className="px-4 py-3 text-right">SL Xuất</th>
              <th className="px-4 py-3 text-right">Giá trị xuất</th>
              <th className="px-4 py-3 text-center">Trạng thái</th>
              <th className="px-4 py-3 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-sm text-neutral-400">
                  Không tìm thấy chuyến hàng chuyển đi nào.
                </td>
              </tr>
            ) : (
              filtered.map((t) => {
                const statusCfg = TRANSFER_STATUS_LABELS[t.status] || {
                  label: t.status,
                  color: 'bg-neutral-100 text-neutral-700'
                };

                return (
                  <tr key={t.id} className="hover:bg-neutral-50/50 dark:hover:bg-neutral-850">
                    <td className="px-4 py-3 font-semibold text-neutral-900 dark:text-neutral-100">
                      {t.transferNumber}
                    </td>
                    <td className="px-4 py-3 font-medium text-neutral-700 dark:text-neutral-300">
                      {t.toBranchName}
                    </td>
                    <td className="px-4 py-3 text-xs text-neutral-500">{t.requestedOn}</td>
                    <td className="px-4 py-3 text-right font-medium text-neutral-900 dark:text-neutral-100">
                      {t.totalDispatchedQuantity}
                    </td>
                    <td className="px-4 py-3 text-right text-xs text-neutral-600 dark:text-neutral-300">
                      {formatVnd(t.totalDispatchedCost)}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusCfg.color}`}>
                        {statusCfg.label}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {t.status === 'DRAFT' && (
                          <>
                            <button
                              onClick={() => onDispatch(t.id)}
                              title="Xuất kho vận chuyển"
                              className="rounded-lg p-1.5 text-primary-600 hover:bg-primary-50 dark:text-primary-400 dark:hover:bg-primary-950"
                            >
                              <Send className="h-4 w-4" />
                            </button>
                            <button
                              onClick={() => onCancel(t.id)}
                              title="Hủy phiếu nháp"
                              className="rounded-lg p-1.5 text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-950"
                            >
                              <XCircle className="h-4 w-4" />
                            </button>
                          </>
                        )}
                        <button
                          onClick={() => onOpenDetail(t)}
                          title="Xem chi tiết"
                          className="rounded-lg p-1.5 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 dark:hover:bg-neutral-800"
                        >
                          <Eye className="h-4 w-4" />
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
    </div>
  );
};
