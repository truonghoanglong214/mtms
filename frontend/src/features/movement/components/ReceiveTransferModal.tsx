import React, { useState, useEffect } from 'react';
import { X, PackageCheck, AlertTriangle, AlertCircle, ShieldAlert } from 'lucide-react';
import { movementApi } from '../api/movementApi';
import { ReceiveTransferPayload, StockTransfer } from '../types/movement.types';

interface Props {
  branchId: string;
  transfer: StockTransfer | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface LineReceiptInput {
  lineId: string;
  dispatchedQty: number;
  receivedQty: string;
  receiverNote: string;
  itemName: string;
  baseUnit: string;
  senderUnitCost: number;
}

export const ReceiveTransferModal: React.FC<Props> = ({
  branchId,
  transfer,
  isOpen,
  onClose,
  onSuccess
}) => {
  const [lines, setLines] = useState<LineReceiptInput[]>([]);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (transfer && isOpen) {
      setLines(
        transfer.lines.map((l) => ({
          lineId: l.id,
          dispatchedQty: l.dispatchedBaseQty,
          receivedQty: l.dispatchedBaseQty.toString(),
          receiverNote: '',
          itemName: l.itemName,
          baseUnit: l.baseUnit,
          senderUnitCost: l.senderUnitCost
        }))
      );
      setNotes('');
      setError(null);
    }
  }, [transfer, isOpen]);

  if (!isOpen || !transfer) return null;

  const handleQtyChange = (index: number, val: string) => {
    const updated = [...lines];
    updated[index].receivedQty = val;
    setLines(updated);
  };

  const handleNoteChange = (index: number, val: string) => {
    const updated = [...lines];
    updated[index].receiverNote = val;
    setLines(updated);
  };

  const hasShortage = lines.some((l) => Number(l.receivedQty) < l.dispatchedQty);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    for (const l of lines) {
      const rec = Number(l.receivedQty);
      if (isNaN(rec) || rec < 0) {
        setError(`Số lượng nhận cho mặt hàng "${l.itemName}" không hợp lệ.`);
        return;
      }
      if (rec > l.dispatchedQty) {
        setError(
          `Số lượng thực nhận (${rec}) không được vượt quá số lượng xuất (${l.dispatchedQty}) cho mặt hàng "${l.itemName}".`
        );
        return;
      }
      if (rec < l.dispatchedQty && !l.receiverNote.trim()) {
        setError(
          `Vui lòng nhập lý do hao hụt / nhận thiếu cho mặt hàng "${l.itemName}" (TC-027).`
        );
        return;
      }
    }

    setSubmitting(true);
    try {
      const payload: ReceiveTransferPayload = {
        notes: notes.trim() || undefined,
        lines: lines.map((l) => ({
          lineId: l.lineId,
          receivedBaseQty: Number(l.receivedQty),
          receiverNote: l.receiverNote.trim() || undefined
        }))
      };

      await movementApi.receiveTransfer(branchId, transfer.id, payload);
      onSuccess();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Lỗi khi tiếp nhận hàng chuyển kho';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white shadow-2xl dark:bg-neutral-900">
        <div className="flex items-center justify-between border-b border-neutral-200 px-6 py-4 dark:border-neutral-800">
          <div className="flex items-center gap-2">
            <div className="rounded-lg bg-emerald-100 p-2 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-400">
              <PackageCheck className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
                Tiếp Nhận Hàng Chuyển Kho
              </h3>
              <p className="text-xs text-neutral-500">
                Phiếu: <strong>{transfer.transferNumber}</strong> từ <strong>{transfer.fromBranchName}</strong>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-600 dark:hover:bg-neutral-800"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 p-6">
          {/* TC-027 Notice */}
          {hasShortage ? (
            <div className="flex items-start gap-2.5 rounded-xl border border-amber-300 bg-amber-50 p-3.5 text-xs text-amber-900 dark:border-amber-900 dark:bg-amber-950/40 dark:text-amber-300">
              <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
              <span>
                <strong>CẢNH BÁO TIẾP NHẬN THIẾU (TC-027 / NO VANISH):</strong> Hệ thống phát hiện có mặt hàng nhận thiếu so với số lượng xuất. Số lượng thiếu sẽ được lưu trữ vào chênh lệch trên đường vận chuyển (<code>transit_variance_qty</code>) để giải quyết đối soát, tuyệt đối không tự động biến mất. Vui lòng ghi rõ lý do hao hụt.
              </span>
            </div>
          ) : (
            <div className="flex items-start gap-2.5 rounded-xl border border-blue-200 bg-blue-50 p-3.5 text-xs text-blue-900 dark:border-blue-900 dark:bg-blue-950/40 dark:text-blue-300">
              <ShieldAlert className="h-4 w-4 shrink-0 text-blue-600 dark:text-blue-400" />
              <span>
                Số lượng thực nhận sẽ được cộng vào kho chi nhánh đích và tự động tái tính đơn giá bình quân gia quyền Moving WAC theo đơn giá snapshot từ chi nhánh gửi.
              </span>
            </div>
          )}

          {error && (
            <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-3.5 text-xs text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Line items review table */}
          <div className="space-y-3">
            <label className="text-xs font-semibold uppercase tracking-wider text-neutral-500">
              Kiểm đếm số lượng thực nhận
            </label>

            <div className="space-y-3">
              {lines.map((line, index) => {
                const recVal = Number(line.receivedQty) || 0;
                const variance = line.dispatchedQty - recVal;
                const isShort = variance > 0;

                return (
                  <div
                    key={line.lineId}
                    className={`rounded-xl border p-4 transition-colors ${
                      isShort
                        ? 'border-amber-300 bg-amber-50/40 dark:border-amber-900 dark:bg-amber-950/20'
                        : 'border-neutral-200 bg-neutral-50/50 dark:border-neutral-800 dark:bg-neutral-850'
                    }`}
                  >
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <div className="font-semibold text-neutral-900 dark:text-neutral-100">
                          {line.itemName}
                        </div>
                        <div className="mt-0.5 text-xs text-neutral-500">
                          Xuất gửi: <strong>{line.dispatchedQty} {line.baseUnit}</strong> &bull; Đơn giá xuất WAC: {new Intl.NumberFormat('vi-VN').format(line.senderUnitCost)} ₫
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <label className="text-xs text-neutral-600 dark:text-neutral-400">Thực nhận:</label>
                        <input
                          type="number"
                          step="0.001"
                          min="0"
                          max={line.dispatchedQty}
                          required
                          value={line.receivedQty}
                          onChange={(e) => handleQtyChange(index, e.target.value)}
                          className="w-28 rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-semibold text-neutral-900 focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800 dark:text-neutral-100"
                        />
                        <span className="text-xs font-medium text-neutral-500">{line.baseUnit}</span>
                      </div>
                    </div>

                    {isShort && (
                      <div className="mt-3 space-y-2 border-t border-amber-200/60 pt-2.5 dark:border-amber-900/40">
                        <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-700 dark:text-amber-400">
                          <AlertTriangle className="h-3.5 w-3.5" />
                          Hao hụt / Chênh lệch: {variance.toFixed(3)} {line.baseUnit}
                        </div>
                        <input
                          type="text"
                          required
                          placeholder="Lý do hao hụt (vỡ, hỏng, đổ, rách bao bì...) *"
                          value={line.receiverNote}
                          onChange={(e) => handleNoteChange(index, e.target.value)}
                          className="w-full rounded-lg border border-amber-300 bg-white px-3 py-1.5 text-xs text-neutral-900 focus:border-amber-500 focus:outline-none dark:border-amber-800 dark:bg-neutral-800 dark:text-neutral-100"
                        />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
              Ghi chú tiếp nhận toàn bộ chuyến hàng
            </label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Ghi chú tổng thể tình trạng nhận hàng..."
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>

          <div className="flex items-center justify-end gap-3 border-t border-neutral-200 pt-4 dark:border-neutral-800">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-emerald-600 px-5 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
            >
              {submitting ? 'Đang ghi nhận...' : 'Xác nhận tiếp nhận'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
