import React from 'react';
import { AlertTriangle, Eye } from 'lucide-react';
import { StockTransfer } from '../types/movement.types';

interface Props {
  transfers: StockTransfer[];
  onOpenDetail: (transfer: StockTransfer) => void;
}

export const DiscrepanciesTab: React.FC<Props> = ({
  transfers,
  onOpenDetail
}) => {
  const discrepancies = transfers.filter(
    (t) => t.status === 'PARTIAL_RECEIPT' || t.totalTransitVarianceQuantity > 0
  );

  return (
    <div className="space-y-4">
      {/* TC-027 Banner */}
      <div className="rounded-xl border border-amber-300 bg-amber-50 p-4 shadow-sm dark:border-amber-900 dark:bg-amber-950/40">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-amber-100 p-2 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <div className="space-y-1">
            <h4 className="text-sm font-semibold text-amber-900 dark:text-amber-200">
              ĐỐI SOÁT HAO HỤT TRÊN ĐƯỜNG VẬN CHUYỂN (BẢO LƯU CHÊNH LỆCH)
            </h4>
            <p className="text-xs leading-relaxed text-amber-800 dark:text-amber-300">
              Các chuyến hàng dưới đây có sự chênh lệch giữa số lượng xuất gửi và số lượng thực nhận. Hệ thống yêu cầu bảo lưu toàn bộ số lượng thiếu dở dang (<code>transit_variance_qty</code>) trên hệ thống, kèm lý do và chứng từ giải trình, phục vụ cho quy trình giải quyết tranh chấp hao hụt giữa hai chi nhánh.
            </p>
          </div>
        </div>
      </div>

      {/* Discrepancies Table */}
      <div className="overflow-x-auto rounded-xl border border-neutral-200 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-900">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-neutral-200 bg-neutral-50 text-xs font-semibold text-neutral-500 dark:border-neutral-800 dark:bg-neutral-800 dark:text-neutral-400">
            <tr>
              <th className="px-4 py-3">Mã phiếu</th>
              <th className="px-4 py-3">Chi nhánh gửi</th>
              <th className="px-4 py-3">Chi nhánh nhận</th>
              <th className="px-4 py-3 text-right">SL Xuất</th>
              <th className="px-4 py-3 text-right">SL Đã nhận</th>
              <th className="px-4 py-3 text-right">Hao hụt dở dang</th>
              <th className="px-4 py-3">Ghi chú nhận hàng</th>
              <th className="px-4 py-3 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
            {discrepancies.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-sm text-neutral-400">
                  Không có chuyến hàng nào bị hao hụt hoặc dở dang. Toàn bộ các chuyến điều chuyển đều đã được nhận đủ 100%.
                </td>
              </tr>
            ) : (
              discrepancies.map((t) => (
                <tr key={t.id} className="hover:bg-neutral-50/50 dark:hover:bg-neutral-850">
                  <td className="px-4 py-3 font-semibold text-neutral-900 dark:text-neutral-100">
                    {t.transferNumber}
                  </td>
                  <td className="px-4 py-3 text-neutral-700 dark:text-neutral-300">{t.fromBranchName}</td>
                  <td className="px-4 py-3 text-neutral-700 dark:text-neutral-300">{t.toBranchName}</td>
                  <td className="px-4 py-3 text-right font-medium text-neutral-900 dark:text-neutral-100">
                    {t.totalDispatchedQuantity}
                  </td>
                  <td className="px-4 py-3 text-right font-semibold text-emerald-600 dark:text-emerald-400">
                    {t.totalReceivedQuantity}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <span className="font-bold text-amber-600 dark:text-amber-400">
                      {t.totalTransitVarianceQuantity}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-neutral-600 dark:text-neutral-400">
                    {t.lines.find((l) => l.transitVarianceQty > 0)?.receiverNote || t.notes || 'Hao hụt vận chuyển'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => onOpenDetail(t)}
                      className="rounded-lg p-1.5 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 dark:hover:bg-neutral-800"
                    >
                      <Eye className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
