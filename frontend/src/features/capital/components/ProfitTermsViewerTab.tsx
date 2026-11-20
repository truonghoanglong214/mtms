import React from 'react';
import { ShieldAlert, ArrowRightLeft, Info, Plus } from 'lucide-react';
import { InvestorCapitalBreakdown, ProfitEntitlementTerm } from '../types/capital.types';

interface Props {
  profitTerms: ProfitEntitlementTerm[];
  breakdowns: InvestorCapitalBreakdown[];
  onOpenCreateProfitTerm: () => void;
}

export const ProfitTermsViewerTab: React.FC<Props> = ({
  profitTerms,
  breakdowns,
  onOpenCreateProfitTerm
}) => {
  return (
    <div className="space-y-6">
      {/* G5 Gate & Provisional Boundary Banner */}
      <div className="rounded-xl border border-amber-300 bg-amber-50 p-4 shadow-sm dark:border-amber-900 dark:bg-amber-950/40">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-amber-100 p-2 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300">
            <ShieldAlert className="h-5 w-5" />
          </div>
          <div className="space-y-1">
            <h4 className="text-sm font-semibold text-amber-900 dark:text-amber-200">
              QUY ĐỊNH PHÂN PHỐI LỢI NHUẬN (G5 SPECIALIST GATE & ADR-007)
            </h4>
            <p className="text-xs leading-relaxed text-amber-800 dark:text-amber-300">
              Trong Phase 0, các tỷ lệ chia lợi nhuận chỉ mang tính <strong>THIẾT LẬP THAM CHIẾU NỘI BỘ (INFORMATIONAL ONLY)</strong>. Tuyệt đối <strong>chưa thực hiện chi trả lợi nhuận bằng tiền mặt</strong> cho đến khi có xác nhận tuân thủ độc lập của chuyên gia kế toán/pháp lý tại Cổng kiểm soát G5.
            </p>
          </div>
        </div>
      </div>

      {/* Reconciliation Table: CAP-INV-01 (Capital Ratio vs Profit Share Ratio) */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-base font-semibold text-neutral-900 dark:text-neutral-100">
              Đối chiếu Tỷ lệ Vốn góp vs Tỷ lệ Hưởng Lợi nhuận (CAP-INV-01)
            </h3>
            <p className="text-xs text-neutral-500">
              Nguyên tắc nghiệp vụ: Tỷ lệ chia lợi nhuận độc lập với số vốn thực góp theo thỏa thuận cổ đông
            </p>
          </div>
          <button
            onClick={onOpenCreateProfitTerm}
            className="flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700"
          >
            <Plus className="h-4 w-4" />
            <span>Cấu hình Tỷ lệ Lợi nhuận</span>
          </button>
        </div>

        <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b bg-neutral-50 text-xs font-medium uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900/60 dark:text-neutral-400">
                <tr>
                  <th className="px-4 py-3">Cổ đông / Nhà đầu tư</th>
                  <th className="px-4 py-3 text-right">Vốn đã góp (VND)</th>
                  <th className="px-4 py-3 text-right">Vốn còn nợ</th>
                  <th className="px-4 py-3 text-center">% Tỷ lệ Vốn thực góp</th>
                  <th className="px-4 py-3 text-center">% Tỷ lệ Chia Lợi nhuận</th>
                  <th className="px-4 py-3 text-center">Đặc thù Thỏa thuận (CAP-INV-01)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
                {breakdowns.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-neutral-500">
                      Chưa có dữ liệu cổ đông nào được thiết lập
                    </td>
                  </tr>
                ) : (
                  breakdowns.map((b) => (
                    <tr key={b.investorId} className="hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                      <td className="px-4 py-3 font-semibold text-neutral-900 dark:text-neutral-100">
                        {b.investorName}
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-emerald-600 dark:text-emerald-400">
                        {new Intl.NumberFormat('vi-VN').format(b.totalContributedValue)} ₫
                      </td>
                      <td className="px-4 py-3 text-right text-xs text-neutral-500">
                        {new Intl.NumberFormat('vi-VN').format(b.outstandingValue)} ₫
                      </td>
                      <td className="px-4 py-3 text-center font-bold text-neutral-800 dark:text-neutral-200">
                        {b.capitalSharePercent.toFixed(2)}%
                      </td>
                      <td className="px-4 py-3 text-center font-bold text-indigo-600 dark:text-indigo-400">
                        {b.profitSharePercent.toFixed(2)}%
                      </td>
                      <td className="px-4 py-3 text-center">
                        {b.isDifferentRatio ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-indigo-50 px-2.5 py-0.5 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                            <ArrowRightLeft className="h-3 w-3" />
                            Tỷ lệ lợi nhuận khác vốn góp
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full bg-neutral-100 px-2.5 py-0.5 text-xs font-medium text-neutral-600 dark:bg-neutral-800 dark:text-neutral-400">
                            Tương đương tỷ lệ vốn
                          </span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Active Profit Terms History Table */}
      <div className="space-y-3">
        <h3 className="text-base font-semibold text-neutral-900 dark:text-neutral-100">
          Danh mục Tỷ lệ Lợi nhuận Thỏa thuận Hiệu lực
        </h3>

        <div className="overflow-hidden rounded-xl border border-neutral-200 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b bg-neutral-50 text-xs font-medium uppercase text-neutral-500 dark:border-neutral-800 dark:bg-neutral-900/60 dark:text-neutral-400">
                <tr>
                  <th className="px-4 py-3">Hợp đồng</th>
                  <th className="px-4 py-3">Cổ đông</th>
                  <th className="px-4 py-3 text-center">Tỷ lệ hưởng</th>
                  <th className="px-4 py-3">Thời gian hiệu lực</th>
                  <th className="px-4 py-3">Điều kiện phân phối</th>
                  <th className="px-4 py-3">Chế độ hiển thị</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
                {profitTerms.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-neutral-500">
                      Chưa có tỷ lệ chia lợi nhuận nào được cấu hình
                    </td>
                  </tr>
                ) : (
                  profitTerms.map((t) => (
                    <tr key={t.id} className="hover:bg-neutral-50 dark:hover:bg-neutral-800/50">
                      <td className="px-4 py-3 font-semibold text-neutral-900 dark:text-neutral-100">
                        {t.agreementRef}
                      </td>
                      <td className="px-4 py-3 text-neutral-800 dark:text-neutral-200">
                        {t.investorName}
                      </td>
                      <td className="px-4 py-3 text-center font-bold text-indigo-600 dark:text-indigo-400">
                        {t.profitSharePercent.toFixed(2)}%
                      </td>
                      <td className="px-4 py-3 text-xs text-neutral-600 dark:text-neutral-400">
                        {t.effectiveFrom} {t.effectiveTo ? `→ ${t.effectiveTo}` : '→ Không thời hạn'}
                      </td>
                      <td className="px-4 py-3 text-xs text-neutral-500 max-w-xs truncate">
                        {t.eligibilityConditions || 'Không có điều kiện đặc thù'}
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                          <Info className="h-3 w-3" />
                          Tham chiếu (P0)
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
