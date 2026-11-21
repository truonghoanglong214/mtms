import React from 'react';
import { X, ArrowRightLeft, Building2 } from 'lucide-react';
import { StockTransfer, TRANSFER_STATUS_LABELS } from '../types/movement.types';

interface Props {
  transfer: StockTransfer | null;
  isOpen: boolean;
  onClose: () => void;
  onDispatch?: (transferId: string) => void;
  onReceive?: (transfer: StockTransfer) => void;
}

export const TransferDetailModal: React.FC<Props> = ({
  transfer,
  isOpen,
  onClose,
  onDispatch,
  onReceive
}) => {
  if (!isOpen || !transfer) return null;

  const statusConfig = TRANSFER_STATUS_LABELS[transfer.status] || {
    label: transfer.status,
    color: 'bg-neutral-100 text-neutral-700'
  };

  const formatVnd = (amount?: number) => {
    if (amount === undefined || amount === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-2xl bg-white shadow-2xl dark:bg-neutral-900">
        <div className="flex items-center justify-between border-b border-neutral-200 px-6 py-4 dark:border-neutral-800">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-primary-100 p-2 text-primary-600 dark:bg-primary-950 dark:text-primary-400">
              <ArrowRightLeft className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
                  {transfer.transferNumber}
                </h3>
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusConfig.color}`}>
                  {statusConfig.label}
                </span>
              </div>
              <p className="text-xs text-neutral-500">Mã định danh: {transfer.id}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-600 dark:hover:bg-neutral-800"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-6 p-6">
          {/* Metadata Route Overview */}
          <div className="grid grid-cols-1 gap-4 rounded-xl border border-neutral-200 bg-neutral-50/50 p-4 dark:border-neutral-800 dark:bg-neutral-850 sm:grid-cols-2">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-xs font-semibold text-neutral-500">
                <Building2 className="h-4 w-4" />
                CHI NHÁNH GỬI (FROM)
              </div>
              <div className="text-sm font-bold text-neutral-900 dark:text-neutral-100">
                {transfer.fromBranchName}
              </div>
              <div className="text-xs text-neutral-500">
                Người yêu cầu: <strong>{transfer.requestedByName || 'N/A'}</strong>
              </div>
              {transfer.dispatchedAt && (
                <div className="text-xs text-neutral-500">
                  Xuất kho lúc: {new Date(transfer.dispatchedAt).toLocaleString('vi-VN')}
                </div>
              )}
            </div>

            <div className="space-y-2">
              <div className="flex items-center gap-2 text-xs font-semibold text-neutral-500">
                <Building2 className="h-4 w-4" />
                CHI NHÁNH NHẬN (TO)
              </div>
              <div className="text-sm font-bold text-neutral-900 dark:text-neutral-100">
                {transfer.toBranchName}
              </div>
              {transfer.receivedByName && (
                <div className="text-xs text-neutral-500">
                  Người tiếp nhận: <strong>{transfer.receivedByName}</strong>
                </div>
              )}
              {transfer.completedAt && (
                <div className="text-xs text-neutral-500">
                  Tiếp nhận lúc: {new Date(transfer.completedAt).toLocaleString('vi-VN')}
                </div>
              )}
            </div>
          </div>

          {/* KPI metrics */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="rounded-xl border border-neutral-200 bg-white p-3.5 dark:border-neutral-800 dark:bg-neutral-800">
              <span className="text-xs text-neutral-500">Tổng xuất gửi</span>
              <div className="mt-1 text-base font-bold text-neutral-900 dark:text-neutral-100">
                {transfer.totalDispatchedQuantity}
              </div>
              <div className="text-[11px] text-neutral-400">{formatVnd(transfer.totalDispatchedCost)}</div>
            </div>

            <div className="rounded-xl border border-neutral-200 bg-white p-3.5 dark:border-neutral-800 dark:bg-neutral-800">
              <span className="text-xs text-neutral-500">Tổng thực nhận</span>
              <div className="mt-1 text-base font-bold text-emerald-600 dark:text-emerald-400">
                {transfer.totalReceivedQuantity}
              </div>
              <div className="text-[11px] text-neutral-400">{formatVnd(transfer.totalReceivedCost)}</div>
            </div>

            <div className="rounded-xl border border-neutral-200 bg-white p-3.5 dark:border-neutral-800 dark:bg-neutral-800">
              <span className="text-xs text-neutral-500">Chênh lệch dở dang</span>
              <div className={`mt-1 text-base font-bold ${transfer.totalTransitVarianceQuantity > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-neutral-900 dark:text-neutral-100'}`}>
                {transfer.totalTransitVarianceQuantity}
              </div>
              <div className="text-[11px] text-neutral-400">
                {transfer.totalTransitVarianceQuantity > 0 ? 'Bảo lưu đối soát' : 'Nhận đủ 100%'}
              </div>
            </div>

            <div className="rounded-xl border border-neutral-200 bg-white p-3.5 dark:border-neutral-800 dark:bg-neutral-800">
              <span className="text-xs text-neutral-500">Số dòng mặt hàng</span>
              <div className="mt-1 text-base font-bold text-neutral-900 dark:text-neutral-100">
                {transfer.totalLines} dòng
              </div>
              <div className="text-[11px] text-neutral-400">Ngày yêu cầu: {transfer.requestedOn}</div>
            </div>
          </div>

          {/* Transfer Lines Table */}
          <div className="space-y-2">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-neutral-500">
              Chi tiết mặt hàng điều chuyển
            </h4>

            <div className="overflow-x-auto rounded-xl border border-neutral-200 dark:border-neutral-800">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-neutral-200 bg-neutral-50 text-xs font-semibold text-neutral-500 dark:border-neutral-800 dark:bg-neutral-800 dark:text-neutral-400">
                  <tr>
                    <th className="px-4 py-3">STT</th>
                    <th className="px-4 py-3">Mặt hàng</th>
                    <th className="px-4 py-3 text-right">SL Xuất</th>
                    <th className="px-4 py-3 text-right">Đơn giá WAC</th>
                    <th className="px-4 py-3 text-right">SL Thực nhận</th>
                    <th className="px-4 py-3 text-right">Hao hụt</th>
                    <th className="px-4 py-3">Lý do / Ghi chú nhận</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
                  {transfer.lines.map((line, idx) => (
                    <tr key={line.id} className="hover:bg-neutral-50/50 dark:hover:bg-neutral-850">
                      <td className="px-4 py-3 font-medium text-neutral-500">{idx + 1}</td>
                      <td className="px-4 py-3">
                        <div className="font-semibold text-neutral-900 dark:text-neutral-100">
                          {line.itemName}
                        </div>
                        <div className="text-xs text-neutral-500">{line.itemCode}</div>
                      </td>
                      <td className="px-4 py-3 text-right font-medium text-neutral-900 dark:text-neutral-100">
                        {line.dispatchedBaseQty} {line.baseUnit}
                      </td>
                      <td className="px-4 py-3 text-right text-xs text-neutral-500">
                        {formatVnd(line.senderUnitCost)}
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-emerald-600 dark:text-emerald-400">
                        {line.receivedBaseQty} {line.baseUnit}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {line.transitVarianceQty > 0 ? (
                          <span className="font-bold text-amber-600 dark:text-amber-400">
                            {line.transitVarianceQty} {line.baseUnit}
                          </span>
                        ) : (
                          <span className="text-xs text-neutral-400">0</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-xs text-neutral-600 dark:text-neutral-300">
                        {line.receiverNote || line.notes || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {transfer.notes && (
            <div className="rounded-xl border border-neutral-200 bg-neutral-50/50 p-3.5 text-xs text-neutral-600 dark:border-neutral-800 dark:bg-neutral-850 dark:text-neutral-400">
              <strong>Ghi chú:</strong> {transfer.notes}
            </div>
          )}

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-3 border-t border-neutral-200 pt-4 dark:border-neutral-800">
            {transfer.status === 'DRAFT' && onDispatch && (
              <button
                type="button"
                onClick={() => onDispatch(transfer.id)}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700"
              >
                Xuất kho vận chuyển
              </button>
            )}

            {transfer.status === 'DISPATCHED' && onReceive && (
              <button
                type="button"
                onClick={() => onReceive(transfer)}
                className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
              >
                Tiếp nhận hàng
              </button>
            )}

            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
            >
              Đóng
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
