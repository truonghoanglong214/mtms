import React, { useState } from 'react';
import { X, Percent, AlertCircle, ShieldAlert } from 'lucide-react';
import { CreateProfitTermRequest, InvestorAgreement } from '../types/capital.types';
import { capitalApi } from '../api/capitalApi';

interface Props {
  branchId: string;
  agreements: InvestorAgreement[];
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateProfitTermModal: React.FC<Props> = ({
  branchId,
  agreements,
  isOpen,
  onClose,
  onSuccess
}) => {
  const [agreementId, setAgreementId] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(new Date().toISOString().split('T')[0]);
  const [effectiveTo, setEffectiveTo] = useState('');
  const [profitSharePercent, setProfitSharePercent] = useState('');
  const [eligibilityConditions, setEligibilityConditions] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!agreementId) {
      setError('Vui lòng chọn hợp đồng thỏa thuận');
      return;
    }
    const percent = parseFloat(profitSharePercent);
    if (isNaN(percent) || percent < 0 || percent > 100) {
      setError('Tỷ lệ phần trăm chia lợi nhuận phải từ 0% đến 100%');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const ratio = percent / 100.0;
      const payload: CreateProfitTermRequest = {
        agreementId,
        effectiveFrom,
        effectiveTo: effectiveTo || undefined,
        profitShareRatio: ratio,
        eligibilityConditions: eligibilityConditions.trim() || undefined
      };
      await capitalApi.configureProfitTerms(branchId, payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.message || 'Lỗi khi cấu hình tỷ lệ lợi nhuận');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl dark:bg-neutral-900">
        <div className="flex items-center justify-between border-b pb-3 dark:border-neutral-800">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-100 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400">
              <Percent className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-semibold text-neutral-900 dark:text-neutral-100">Cấu hình tỷ lệ chia lợi nhuận</h3>
              <p className="text-xs text-neutral-500">Xác lập tỷ lệ hưởng lợi nhuận theo kỳ hiệu lực của hợp đồng</p>
            </div>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-600 dark:hover:bg-neutral-800">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* G5 Gate Callout */}
        <div className="mt-3 flex items-start gap-2.5 rounded-lg border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900 dark:border-amber-900 dark:bg-amber-950/60 dark:text-amber-200">
          <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
          <div>
            <strong className="font-semibold">LƯU Ý QUAN TRỌNG:</strong>
            <p className="mt-0.5 leading-relaxed">
              Tỷ lệ này chỉ mang tính <strong>tham chiếu nội bộ</strong>. Phase 0 tuyệt đối <strong>chưa thực hiện chi trả lợi nhuận thực tế</strong> cho đến khi có xác nhận của chuyên gia độc lập tại cổng kiểm soát G5.
            </p>
          </div>
        </div>

        {error && (
          <div className="mt-3 flex items-center gap-2 rounded-lg bg-rose-50 p-3 text-sm text-rose-700 dark:bg-rose-950 dark:text-rose-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
              Hợp đồng thỏa thuận đầu tư <span className="text-rose-500">*</span>
            </label>
            <select
              required
              value={agreementId}
              onChange={(e) => setAgreementId(e.target.value)}
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            >
              <option value="">-- Chọn hợp đồng cổ đông --</option>
              {agreements.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.agreementRef} - {a.investorName}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Tỷ lệ chia lợi nhuận (%) <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                required
                min="0"
                max="100"
                step="0.01"
                value={profitSharePercent}
                onChange={(e) => setProfitSharePercent(e.target.value)}
                placeholder="VD: 35.00"
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm font-semibold text-indigo-600 focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800 dark:text-indigo-400"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Ngày bắt đầu hiệu lực <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                required
                value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)}
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">Ngày kết thúc hiệu lực</label>
            <input
              type="date"
              value={effectiveTo}
              onChange={(e) => setEffectiveTo(e.target.value)}
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">Điều kiện đủ điều kiện chia lợi nhuận</label>
            <textarea
              rows={2}
              value={eligibilityConditions}
              onChange={(e) => setEligibilityConditions(e.target.value)}
              placeholder="VD: Phải góp đủ 100% cam kết trước kỳ kết chuyển; chỉ tính sau khi trích quỹ dự phòng..."
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 dark:border-neutral-700 dark:text-neutral-300 dark:hover:bg-neutral-800"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {submitting ? 'Đang lưu...' : 'Lưu tỷ lệ lợi nhuận'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
