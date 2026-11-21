import React, { useState } from 'react';
import { X, ShieldAlert, CheckCircle2, TrendingUp, TrendingDown, Scale } from 'lucide-react';
import { StockCountDetailDto } from '../types/stockCount.types';

interface ApproveStockCountModalProps {
  isOpen: boolean;
  onClose: () => void;
  count: StockCountDetailDto | null;
  onApprove: (countId: string) => Promise<void>;
}

export const ApproveStockCountModal: React.FC<ApproveStockCountModalProps> = ({
  isOpen,
  onClose,
  count,
  onApprove
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !count) return null;

  const handleApprove = async () => {
    setIsSubmitting(true);
    setError(null);
    try {
      await onApprove(count.id);
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lỗi khi phê duyệt đợt kiểm kê';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatVnd = (val: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-lg">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-slate-100">Phê duyệt & Hạch toán Kiểm kê</h2>
              <p className="text-xs text-slate-400">Phiếu: <span className="font-mono text-emerald-400">{count.countNumber}</span></p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-lg text-xs text-rose-400">
              {error}
            </div>
          )}

          {/* Governance Notice Banner */}
          <div className="p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl space-y-2">
            <div className="flex items-center gap-2 text-amber-300 font-semibold text-sm">
              <ShieldAlert className="w-4 h-4 shrink-0 text-amber-400" />
              <span>Nguyên tắc Quản trị & Phê duyệt Kho Hàng</span>
            </div>
            <p className="text-xs text-amber-200/90 leading-relaxed">
              Hành động này thực thi quyền phê duyệt của <strong>Chủ sở hữu (Owner)</strong>:
              Hệ thống sẽ trực tiếp điều chỉnh số lượng tồn kho trên sổ sách (`stock_balances`),
              ghi nhận các bút toán sổ kho điều chỉnh `ADJUSTMENT` và tự động cập nhật lại Moving WAC.
              Đợt kiểm kê sẽ được chuyển sang trạng thái <strong>ĐÃ CHỐT SỔ (POSTED)</strong> và không thể hoàn tác.
            </p>
          </div>

          {/* Variance Summary Cards */}
          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-slate-800/60 border border-slate-700/60 rounded-lg">
              <div className="flex items-center gap-1.5 text-xs text-slate-400 mb-1">
                <TrendingUp className="w-3.5 h-3.5 text-emerald-400" />
                <span>Tổng giá trị hàng THỪA</span>
              </div>
              <div className="text-sm font-bold text-emerald-400">
                +{formatVnd(count.totalSurplusValue)}
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                SL thừa: +{count.totalSurplusQty}
              </div>
            </div>

            <div className="p-3 bg-slate-800/60 border border-slate-700/60 rounded-lg">
              <div className="flex items-center gap-1.5 text-xs text-slate-400 mb-1">
                <TrendingDown className="w-3.5 h-3.5 text-rose-400" />
                <span>Tổng giá trị hàng THIẾU</span>
              </div>
              <div className="text-sm font-bold text-rose-400">
                -{formatVnd(count.totalShortageValue)}
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                SL thiếu: -{count.totalShortageQty}
              </div>
            </div>
          </div>

          {/* Net Impact */}
          <div className="p-3.5 bg-slate-800/40 border border-slate-700 rounded-lg flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs text-slate-300">
              <Scale className="w-4 h-4 text-blue-400" />
              <span>Chênh lệch giá trị tài sản ròng:</span>
            </div>
            <div className={`text-base font-extrabold ${count.netVarianceValue >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {count.netVarianceValue >= 0 ? '+' : ''}{formatVnd(count.netVarianceValue)}
            </div>
          </div>

          <div className="text-xs text-slate-400 italic">
            Tổng cộng: <strong className="text-slate-200">{count.totalItems}</strong> mặt hàng trong đợt kiểm kê ngày <strong>{count.countedOn}</strong>.
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition-colors"
            >
              Xem lại
            </button>
            <button
              type="button"
              onClick={handleApprove}
              disabled={isSubmitting}
              className="px-5 py-2 text-sm font-semibold text-white bg-emerald-600 hover:bg-emerald-500 rounded-lg shadow-lg shadow-emerald-600/20 disabled:opacity-50 transition-all flex items-center gap-2"
            >
              {isSubmitting ? 'Đang hạch toán...' : 'Phê duyệt & Hạch toán Sổ kho'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
