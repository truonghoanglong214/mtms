import React, { useState } from 'react';
import { HandCoins, DollarSign, Search, CheckCircle2, Clock, AlertTriangle } from 'lucide-react';
import {
  CapitalCommitment,
  COMMITMENT_STATUS_LABELS
} from '../types/capital.types';

interface Props {
  commitments: CapitalCommitment[];
  onOpenCreateCommitment: () => void;
  onOpenRecordContribution: (commitment: CapitalCommitment) => void;
}

export const CommitmentsProgressTab: React.FC<Props> = ({
  commitments,
  onOpenCreateCommitment,
  onOpenRecordContribution
}) => {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const filtered = commitments.filter((c) => {
    const matchesSearch =
      c.agreementRef.toLowerCase().includes(search.toLowerCase()) ||
      c.investorName.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

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
              placeholder="Tìm theo cổ đông, số hợp đồng..."
              className="w-full rounded-lg border border-neutral-300 py-2 pl-9 pr-3 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
          >
            <option value="ALL">Tất cả tiến độ</option>
            <option value="PENDING">Chờ góp vốn</option>
            <option value="PARTIALLY_FULFILLED">Đang góp dở dang</option>
            <option value="FULFILLED">Đã hoàn tất 100%</option>
          </select>
        </div>

        <button
          onClick={onOpenCreateCommitment}
          className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-2 text-sm font-medium text-white hover:bg-primary-700"
        >
          <HandCoins className="h-4 w-4" />
          <span>Đăng ký Cam kết Vốn</span>
        </button>
      </div>

      {/* Commitments Table */}
      <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b bg-neutral-50 text-xs font-medium uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900/60 dark:text-neutral-400">
              <tr>
                <th className="px-4 py-3">Cổ đông / Hợp đồng</th>
                <th className="px-4 py-3 text-right">Cam kết vốn</th>
                <th className="px-4 py-3 text-right">Đã thực góp</th>
                <th className="px-4 py-3 text-right">Còn nợ lại (TC-032)</th>
                <th className="px-4 py-3">Tiến độ hoàn thành</th>
                <th className="px-4 py-3">Hạn chót</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3 text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-neutral-500">
                    <p className="text-sm">Chưa có cam kết vốn nào phù hợp</p>
                  </td>
                </tr>
              ) : (
                filtered.map((c) => {
                  const statusInfo = COMMITMENT_STATUS_LABELS[c.status] || {
                    label: c.status,
                    color: 'bg-neutral-100 text-neutral-800'
                  };
                  const percent = c.committedValue > 0
                    ? Math.min(100, (c.actualContributedValue / c.committedValue) * 100)
                    : 0;

                  return (
                    <tr key={c.id} className="hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                      <td className="px-4 py-3">
                        <div className="font-semibold text-neutral-900 dark:text-neutral-100">{c.investorName}</div>
                        <div className="text-xs text-neutral-500">HĐ: {c.agreementRef}</div>
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-neutral-900 dark:text-neutral-100">
                        {new Intl.NumberFormat('vi-VN').format(c.committedValue)} ₫
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-emerald-600 dark:text-emerald-400">
                        {new Intl.NumberFormat('vi-VN').format(c.actualContributedValue)} ₫
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-amber-600 dark:text-amber-400">
                        {new Intl.NumberFormat('vi-VN').format(c.outstandingValue)} ₫
                      </td>
                      <td className="px-4 py-3 min-w-[140px]">
                        <div className="flex items-center gap-2">
                          <div className="h-2 flex-1 rounded-full bg-neutral-200 dark:bg-neutral-700">
                            <div
                              className={`h-2 rounded-full ${percent >= 100 ? 'bg-emerald-500' : 'bg-primary-500'}`}
                              style={{ width: `${percent}%` }}
                            />
                          </div>
                          <span className="text-xs font-semibold text-neutral-700 dark:text-neutral-300">
                            {percent.toFixed(1)}%
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-xs text-neutral-600 dark:text-neutral-400">
                        {c.commitmentDueDate ? c.commitmentDueDate : 'Chưa thiết lập'}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${statusInfo.color}`}>
                          {c.status === 'FULFILLED' ? (
                            <CheckCircle2 className="h-3 w-3" />
                          ) : c.status === 'PARTIALLY_FULFILLED' ? (
                            <Clock className="h-3 w-3" />
                          ) : (
                            <AlertTriangle className="h-3 w-3" />
                          )}
                          {statusInfo.label}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <button
                          onClick={() => onOpenRecordContribution(c)}
                          disabled={c.status === 'FULFILLED'}
                          className="inline-flex items-center gap-1 rounded-lg bg-emerald-600 px-2.5 py-1.5 text-xs font-medium text-white hover:bg-emerald-700 disabled:opacity-40 disabled:hover:bg-emerald-600"
                        >
                          <DollarSign className="h-3.5 w-3.5" />
                          <span>Góp vốn</span>
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
