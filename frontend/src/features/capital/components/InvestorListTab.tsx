import React, { useState } from 'react';
import { Search, UserPlus, FileSignature, Users, CheckCircle, ShieldAlert } from 'lucide-react';
import { AGREEMENT_STATUS_LABELS, CONTRIBUTION_TYPE_LABELS, Investor, InvestorAgreement } from '../types/capital.types';

interface Props {
  investors: Investor[];
  agreements: InvestorAgreement[];
  onOpenCreateInvestor: () => void;
  onOpenCreateAgreement: () => void;
}

export const InvestorListTab: React.FC<Props> = ({
  agreements,
  onOpenCreateInvestor,
  onOpenCreateAgreement
}) => {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const filteredAgreements = agreements.filter((a) => {
    const matchesSearch =
      a.agreementRef.toLowerCase().includes(search.toLowerCase()) ||
      a.investorName.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || a.status === statusFilter;
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
              placeholder="Tìm theo tên cổ đông, số hợp đồng..."
              className="w-full rounded-lg border border-neutral-300 py-2 pl-9 pr-3 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang hiệu lực</option>
            <option value="TERMINATED">Đã chấm dứt</option>
            <option value="EXPIRED">Hết hạn</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={onOpenCreateInvestor}
            className="flex items-center gap-1.5 rounded-lg border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
          >
            <UserPlus className="h-4 w-4" />
            <span>Thêm Cổ đông</span>
          </button>
          <button
            onClick={onOpenCreateAgreement}
            className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-2 text-sm font-medium text-white hover:bg-primary-700"
          >
            <FileSignature className="h-4 w-4" />
            <span>Ký Hợp đồng Chi nhánh</span>
          </button>
        </div>
      </div>

      {/* Agreements Table */}
      <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b bg-neutral-50 text-xs font-medium uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900/60 dark:text-neutral-400">
              <tr>
                <th className="px-4 py-3">Số HĐ / Ký hiệu</th>
                <th className="px-4 py-3">Cổ đông / Nhà đầu tư</th>
                <th className="px-4 py-3">Hình thức góp</th>
                <th className="px-4 py-3">Hiệu lực</th>
                <th className="px-4 py-3">Điều khoản tóm tắt</th>
                <th className="px-4 py-3">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
              {filteredAgreements.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-neutral-500">
                    <div className="flex flex-col items-center justify-center">
                      <Users className="h-8 w-8 text-neutral-300 dark:text-neutral-700" />
                      <p className="mt-2 text-sm">Chưa có hợp đồng thỏa thuận đầu tư nào cho chi nhánh này</p>
                      <button
                        onClick={onOpenCreateAgreement}
                        className="mt-3 text-xs font-semibold text-primary-600 hover:underline dark:text-primary-400"
                      >
                        + Ký kết hợp đồng đầu tư đầu tiên
                      </button>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredAgreements.map((a) => {
                  const statusInfo = AGREEMENT_STATUS_LABELS[a.status] || {
                    label: a.status,
                    color: 'bg-neutral-100 text-neutral-800'
                  };
                  return (
                    <tr key={a.id} className="hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                      <td className="px-4 py-3 font-semibold text-neutral-900 dark:text-neutral-100">
                        {a.agreementRef}
                      </td>
                      <td className="px-4 py-3">
                        <div className="font-medium text-neutral-900 dark:text-neutral-100">{a.investorName}</div>
                      </td>
                      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-300">
                        <span className="rounded-md bg-neutral-100 px-2 py-0.5 text-xs font-medium dark:bg-neutral-800">
                          {CONTRIBUTION_TYPE_LABELS[a.contributionType]}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-neutral-600 dark:text-neutral-300">
                        {a.effectiveFrom} {a.effectiveTo ? `→ ${a.effectiveTo}` : '→ Không thời hạn'}
                      </td>
                      <td className="px-4 py-3 text-xs text-neutral-500 dark:text-neutral-400 max-w-xs truncate">
                        {a.eligibilityTerms || 'Theo thỏa thuận thành viên'}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${statusInfo.color}`}>
                          <CheckCircle className="h-3 w-3" />
                          {statusInfo.label}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Branch Isolation Guard Note */}
      <div className="flex items-start gap-2 rounded-lg border border-neutral-200 bg-neutral-50 p-3 text-xs text-neutral-600 dark:border-neutral-800 dark:bg-neutral-900/60 dark:text-neutral-400">
        <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-primary-600 dark:text-primary-400" />
        <p>
          <strong>Ranh giới bảo mật:</strong> Hợp đồng thỏa thuận đầu tư được quản lý biệt lập theo từng chi nhánh. Nhà đầu tư tại chi nhánh này không tự động có quyền sở hữu hoặc quyền xem số liệu ở chi nhánh khác.
        </p>
      </div>
    </div>
  );
};
