import React, { useState } from 'react';
import { X, Calendar, ClipboardCheck, AlertCircle } from 'lucide-react';
import { CreateStockCountRequest, StockCountType } from '../types/stockCount.types';

interface CreateStockCountModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (request: CreateStockCountRequest) => Promise<void>;
  branchName: string;
}

export const CreateStockCountModal: React.FC<CreateStockCountModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  branchName
}) => {
  const [countedOn, setCountedOn] = useState<string>(new Date().toISOString().split('T')[0]);
  const [countType, setCountType] = useState<StockCountType>('PERIODIC');
  const [notes, setNotes] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      await onSubmit({
        countedOn,
        countType,
        notes: notes.trim() ? notes.trim() : null
      });
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lỗi khi tạo đợt kiểm kê';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-blue-500/10 text-blue-400 rounded-lg">
              <ClipboardCheck className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-slate-100">Khởi tạo Đợt Kiểm kê Kho</h2>
              <p className="text-xs text-slate-400">Chi nhánh: <span className="text-blue-400 font-medium">{branchName}</span></p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-lg flex items-start gap-2.5 text-xs text-rose-400">
              <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg text-xs text-amber-300 leading-relaxed">
            <span className="font-semibold text-amber-200">Nguyên tắc kiểm kê:</span> Hệ thống sẽ tự động chụp ảnh snapshot toàn bộ số lượng tồn sổ sách và giá vốn Moving WAC hiện tại. Việc lập đợt kiểm kê ở trạng thái Bản nháp sẽ <strong>không làm thay đổi số dư tồn kho</strong> cho đến khi có sự phê duyệt của Chủ sở hữu (Owner).
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">
                Ngày kiểm kê <span className="text-rose-400">*</span>
              </label>
              <div className="relative">
                <input
                  type="date"
                  value={countedOn}
                  onChange={(e) => setCountedOn(e.target.value)}
                  required
                  className="w-full px-3 py-2 bg-slate-800/80 border border-slate-700 rounded-lg text-slate-200 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                />
                <Calendar className="w-4 h-4 text-slate-400 absolute right-3 top-2.5 pointer-events-none" />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">
                Hình thức kiểm kê <span className="text-rose-400">*</span>
              </label>
              <select
                value={countType}
                onChange={(e) => setCountType(e.target.value as StockCountType)}
                className="w-full px-3 py-2 bg-slate-800/80 border border-slate-700 rounded-lg text-slate-200 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              >
                <option value="PERIODIC">Định kỳ (Cuối ngày / tuần / tháng)</option>
                <option value="SURPRISE">Đột xuất (Kiểm tra kho bất thường)</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1.5">
              Ghi chú đợt kiểm kê
            </label>
            <textarea
              rows={3}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="VD: Kiểm kê định kỳ chốt tuần quầy bar và kho mát..."
              className="w-full px-3 py-2 bg-slate-800/80 border border-slate-700 rounded-lg text-slate-200 text-sm placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-500 rounded-lg shadow-lg shadow-blue-500/20 disabled:opacity-50 transition-all flex items-center gap-2"
            >
              {isSubmitting ? 'Đang tạo...' : 'Tạo đợt kiểm kê'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
