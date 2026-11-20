import React, { useState, useEffect } from 'react';
import { X, Save, Send, CheckCircle2, Ban, AlertCircle, TrendingUp, TrendingDown, ShieldCheck } from 'lucide-react';
import {
  StockCountDetailDto,
  STOCK_COUNT_STATUS_META,
  STOCK_COUNT_TYPE_META,
  UpdateCountLineRequest
} from '../types/stockCount.types';

interface StockCountDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  count: StockCountDetailDto | null;
  onUpdateCounts: (countId: string, lines: UpdateCountLineRequest[]) => Promise<void>;
  onSubmitCount: (countId: string) => Promise<void>;
  onOpenApproveModal: () => void;
  onCancelCount: (countId: string) => Promise<void>;
}

export const StockCountDetailModal: React.FC<StockCountDetailModalProps> = ({
  isOpen,
  onClose,
  count,
  onUpdateCounts,
  onSubmitCount,
  onOpenApproveModal,
  onCancelCount
}) => {
  const [lineInputs, setLineInputs] = useState<Record<string, { countedQty: string; discrepancyReason: string }>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    if (count) {
      const inputs: Record<string, { countedQty: string; discrepancyReason: string }> = {};
      count.lines.forEach((line) => {
        inputs[line.id] = {
          countedQty: line.countedQty.toString(),
          discrepancyReason: line.discrepancyReason || ''
        };
      });
      setLineInputs(inputs);
      setError(null);
      setSuccessMsg(null);
    }
  }, [count]);

  if (!isOpen || !count) return null;

  const isEditable = count.status === 'DRAFT' || count.status === 'SUBMITTED';

  const formatVnd = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  const handleInputChange = (lineId: string, field: 'countedQty' | 'discrepancyReason', value: string) => {
    setLineInputs((prev) => ({
      ...prev,
      [lineId]: {
        ...prev[lineId],
        [field]: value
      }
    }));
  };

  const handleSaveDraft = async () => {
    setIsSaving(true);
    setError(null);
    setSuccessMsg(null);
    try {
      const requests: UpdateCountLineRequest[] = count.lines.map((l) => {
        const inp = lineInputs[l.id];
        const parsedQty = parseFloat(inp.countedQty);
        return {
          lineId: l.id,
          countedQty: isNaN(parsedQty) ? 0 : parsedQty,
          discrepancyReason: inp.discrepancyReason.trim() ? inp.discrepancyReason.trim() : null
        };
      });

      await onUpdateCounts(count.id, requests);
      setSuccessMsg('Đã lưu kết quả kiểm đếm thành công!');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lỗi khi lưu kết quả kiểm kê';
      setError(msg);
    } finally {
      setIsSaving(false);
    }
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setError(null);
    try {
      await onSubmitCount(count.id);
      setSuccessMsg('Đã nộp biên bản kiểm kê lên Chủ sở hữu (Owner) chờ phê duyệt!');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lỗi khi nộp biên bản kiểm kê';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đợt kiểm kê này không?')) return;
    setIsCancelling(true);
    setError(null);
    try {
      await onCancelCount(count.id);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lỗi khi hủy đợt kiểm kê';
      setError(msg);
    } finally {
      setIsCancelling(false);
    }
  };

  const statusMeta = STOCK_COUNT_STATUS_META[count.status];
  const typeMeta = STOCK_COUNT_TYPE_META[count.countType];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-5xl max-h-[92vh] bg-slate-900 border border-slate-800 rounded-xl shadow-2xl flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/60">
          <div className="flex items-center gap-3">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono text-base font-bold text-slate-100">{count.countNumber}</span>
                <span className={`text-[11px] font-medium px-2 py-0.5 rounded-full border ${statusMeta.bg} ${statusMeta.text} ${statusMeta.border}`}>
                  {statusMeta.label}
                </span>
                <span className={`text-[11px] font-medium px-2 py-0.5 rounded-full border ${typeMeta.badge}`}>
                  {typeMeta.label}
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Chi nhánh: <span className="text-slate-200 font-medium">{count.branchName}</span> • Ngày kiểm kê: <span className="text-slate-200 font-medium">{count.countedOn}</span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content & Table */}
        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {error && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-lg flex items-start gap-2.5 text-xs text-rose-400">
              <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {successMsg && (
            <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-lg flex items-start gap-2.5 text-xs text-emerald-400">
              <ShieldCheck className="w-4 h-4 mt-0.5 shrink-0" />
              <span>{successMsg}</span>
            </div>
          )}

          {/* Quick Metrics Bar */}
          <div className="grid grid-cols-4 gap-3">
            <div className="p-3 bg-slate-800/50 border border-slate-700/50 rounded-lg">
              <span className="text-[11px] text-slate-400 block">Số lượng mặt hàng</span>
              <span className="text-sm font-bold text-slate-200">{count.totalItems} NVL</span>
            </div>
            <div className="p-3 bg-slate-800/50 border border-slate-700/50 rounded-lg">
              <div className="flex items-center gap-1 text-[11px] text-emerald-400">
                <TrendingUp className="w-3 h-3" />
                <span>Tổng giá trị THỪA</span>
              </div>
              <span className="text-sm font-bold text-emerald-400">+{formatVnd(count.totalSurplusValue)}</span>
            </div>
            <div className="p-3 bg-slate-800/50 border border-slate-700/50 rounded-lg">
              <div className="flex items-center gap-1 text-[11px] text-rose-400">
                <TrendingDown className="w-3 h-3" />
                <span>Tổng giá trị THIẾU</span>
              </div>
              <span className="text-sm font-bold text-rose-400">-{formatVnd(count.totalShortageValue)}</span>
            </div>
            <div className="p-3 bg-slate-800/50 border border-slate-700/50 rounded-lg">
              <span className="text-[11px] text-slate-400 block">Chênh lệch ròng</span>
              <span className={`text-sm font-extrabold ${count.netVarianceValue >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                {count.netVarianceValue >= 0 ? '+' : ''}{formatVnd(count.netVarianceValue)}
              </span>
            </div>
          </div>

          {/* Lines Table */}
          <div className="border border-slate-800 rounded-lg overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-800/80 text-slate-400 font-medium border-b border-slate-700">
                  <tr>
                    <th className="py-2.5 px-3 w-10 text-center">#</th>
                    <th className="py-2.5 px-3">Mặt hàng</th>
                    <th className="py-2.5 px-3 w-16 text-center">ĐVT</th>
                    <th className="py-2.5 px-3 w-24 text-right">Sổ sách</th>
                    <th className="py-2.5 px-3 w-32 text-right">Thực đếm</th>
                    <th className="py-2.5 px-3 w-24 text-right">Chênh lệch</th>
                    <th className="py-2.5 px-3 w-28 text-right">Đơn giá vốn</th>
                    <th className="py-2.5 px-3 w-28 text-right">Giá trị lệch</th>
                    <th className="py-2.5 px-4 min-w-[200px]">Lý do giải trình (Bắt buộc nếu lệch)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60 bg-slate-900/40">
                  {count.lines.map((line, idx) => {
                    const input = lineInputs[line.id] || { countedQty: line.countedQty.toString(), discrepancyReason: '' };
                    const currentCounted = parseFloat(input.countedQty);
                    const currentVariance = !isNaN(currentCounted) ? currentCounted - line.expectedQty : 0;
                    const hasDiscrepancy = Math.abs(currentVariance) > 0.0001;

                    return (
                      <tr key={line.id} className="hover:bg-slate-800/30 transition-colors">
                        <td className="py-2.5 px-3 text-center text-slate-500">{idx + 1}</td>
                        <td className="py-2.5 px-3">
                          <div className="font-medium text-slate-200">{line.itemName}</div>
                          <div className="text-[10px] font-mono text-slate-400">{line.itemCode}</div>
                        </td>
                        <td className="py-2.5 px-3 text-center text-slate-300 font-medium">
                          {line.baseUnit}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono text-slate-300 font-semibold">
                          {line.expectedQty}
                        </td>
                        <td className="py-2.5 px-3 text-right">
                          {isEditable ? (
                            <input
                              type="number"
                              step="0.001"
                              min="0"
                              value={input.countedQty}
                              onChange={(e) => handleInputChange(line.id, 'countedQty', e.target.value)}
                              className="w-24 px-2 py-1 text-right bg-slate-800 border border-slate-700 rounded text-slate-100 font-mono text-xs focus:outline-none focus:border-blue-500"
                            />
                          ) : (
                            <span className="font-mono text-slate-200 font-semibold">{line.countedQty}</span>
                          )}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono font-bold">
                          {currentVariance > 0 && <span className="text-emerald-400">+{currentVariance.toFixed(3)}</span>}
                          {currentVariance < 0 && <span className="text-rose-400">{currentVariance.toFixed(3)}</span>}
                          {currentVariance === 0 && <span className="text-slate-500">0.000</span>}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono text-slate-400">
                          {formatVnd(line.snapshotUnitCost)}
                        </td>
                        <td className="py-2.5 px-3 text-right font-mono font-semibold">
                          {line.varianceValue > 0 && <span className="text-emerald-400">+{formatVnd(line.varianceValue)}</span>}
                          {line.varianceValue < 0 && <span className="text-rose-400">{formatVnd(line.varianceValue)}</span>}
                          {line.varianceValue === 0 && <span className="text-slate-500">0 ₫</span>}
                        </td>
                        <td className="py-2.5 px-4">
                          {isEditable ? (
                            <input
                              type="text"
                              value={input.discrepancyReason}
                              onChange={(e) => handleInputChange(line.id, 'discrepancyReason', e.target.value)}
                              placeholder={hasDiscrepancy ? 'Nhập lý do chênh lệch (bắt buộc)...' : 'Không có chênh lệch'}
                              className={`w-full px-2 py-1 bg-slate-800 border rounded text-xs focus:outline-none ${
                                hasDiscrepancy && !input.discrepancyReason.trim()
                                  ? 'border-amber-500/80 text-amber-200 placeholder-amber-400/60'
                                  : 'border-slate-700 text-slate-200 placeholder-slate-500 focus:border-blue-500'
                              }`}
                            />
                          ) : (
                            <span className="text-slate-300 italic">
                              {line.discrepancyReason || <span className="text-slate-600">—</span>}
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Audit trail */}
          <div className="flex flex-wrap items-center gap-x-6 gap-y-1.5 pt-2 text-[11px] text-slate-500 border-t border-slate-800">
            <span>Người lập: <strong className="text-slate-400">{count.createdByName}</strong></span>
            {count.submittedByName && (
              <span>Người nộp: <strong className="text-slate-400">{count.submittedByName}</strong></span>
            )}
            {count.approvedByName && (
              <span>Người duyệt (Owner): <strong className="text-emerald-400">{count.approvedByName}</strong> lúc {new Date(count.postedAt!).toLocaleString('vi-VN')}</span>
            )}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-slate-800 bg-slate-900/70">
          <div>
            {isEditable && (
              <button
                type="button"
                onClick={handleCancel}
                disabled={isCancelling}
                className="px-3 py-1.5 text-xs text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors flex items-center gap-1.5"
              >
                <Ban className="w-3.5 h-3.5" />
                <span>{isCancelling ? 'Đang hủy...' : 'Hủy đợt kiểm kê'}</span>
              </button>
            )}
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition-colors"
            >
              Đóng
            </button>

            {isEditable && (
              <button
                type="button"
                onClick={handleSaveDraft}
                disabled={isSaving}
                className="px-4 py-2 text-xs font-medium text-slate-200 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg transition-colors flex items-center gap-1.5"
              >
                <Save className="w-3.5 h-3.5 text-blue-400" />
                <span>{isSaving ? 'Đang lưu...' : 'Lưu kết quả đếm'}</span>
              </button>
            )}

            {count.status === 'DRAFT' && (
              <button
                type="button"
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="px-4 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-500 rounded-lg shadow-lg shadow-blue-600/20 transition-all flex items-center gap-1.5"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{isSubmitting ? 'Đang nộp...' : 'Nộp duyệt (TC-026)'}</span>
              </button>
            )}

            {count.status === 'SUBMITTED' && (
              <button
                type="button"
                onClick={onOpenApproveModal}
                className="px-5 py-2 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-500 rounded-lg shadow-lg shadow-emerald-600/20 transition-all flex items-center gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Chủ sở hữu Phê duyệt & Hạch toán</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
