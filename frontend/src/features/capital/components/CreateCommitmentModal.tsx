import React, { useState } from 'react';
import { X, HandCoins, AlertCircle } from 'lucide-react';
import { CreateCommitmentRequest, InvestorAgreement } from '../types/capital.types';
import { capitalApi } from '../api/capitalApi';

interface Props {
  branchId: string;
  agreements: InvestorAgreement[];
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateCommitmentModal: React.FC<Props> = ({
  branchId,
  agreements,
  isOpen,
  onClose,
  onSuccess
}) => {
  const [agreementId, setAgreementId] = useState('');
  const [committedValue, setCommittedValue] = useState('');
  const [recognizedNonCashValue, setRecognizedNonCashValue] = useState('0');
  const [commitmentDueDate, setCommitmentDueDate] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!agreementId) {
      setError('Vui lòng chọn hợp đồng thỏa thuận');
      return;
    }
    const val = parseFloat(committedValue);
    if (isNaN(val) || val < 0) {
      setError('Giá trị cam kết vốn không hợp lệ');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const payload: CreateCommitmentRequest = {
        agreementId,
        committedValue: val,
        recognizedNonCashValue: parseFloat(recognizedNonCashValue) || 0,
        commitmentDueDate: commitmentDueDate || undefined,
        notes: notes.trim() || undefined
      };
      await capitalApi.createCommitment(branchId, payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.message || 'Lỗi khi tạo cam kết vốn');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl dark:bg-neutral-900">
        <div className="flex items-center justify-between border-b pb-3 dark:border-neutral-800">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-100 text-primary-600 dark:bg-primary-950 dark:text-primary-400">
              <HandCoins className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-semibold text-neutral-900 dark:text-neutral-100">Đăng ký cam kết vốn (TC-032)</h3>
              <p className="text-xs text-neutral-500">Xác lập hạn mức và hạn chót góp vốn theo hợp đồng</p>
            </div>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-600 dark:hover:bg-neutral-800">
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="mt-4 flex items-center gap-2 rounded-lg bg-rose-50 p-3 text-sm text-rose-700 dark:bg-rose-950 dark:text-rose-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
              Hợp đồng thỏa thuận <span className="text-rose-500">*</span>
            </label>
            <select
              required
              value={agreementId}
              onChange={(e) => setAgreementId(e.target.value)}
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            >
              <option value="">-- Chọn thỏa thuận đầu tư --</option>
              {agreements.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.agreementRef} - {a.investorName} ({a.contributionType})
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Tổng vốn cam kết (VND) <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                required
                min="0"
                step="1000000"
                value={committedValue}
                onChange={(e) => setCommittedValue(e.target.value)}
                placeholder="100,000,000"
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm font-semibold focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Định giá phi tiền mặt (VND)
              </label>
              <input
                type="number"
                min="0"
                step="1000000"
                value={recognizedNonCashValue}
                onChange={(e) => setRecognizedNonCashValue(e.target.value)}
                placeholder="0"
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">Hạn chót góp đủ vốn</label>
            <input
              type="date"
              value={commitmentDueDate}
              onChange={(e) => setCommitmentDueDate(e.target.value)}
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">Ghi chú kế hoạch giải ngân</label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="VD: Chia làm 2 đợt: đợt 1 60M ngày 15/01, đợt 2 40M ngày 28/02..."
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
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
            >
              {submitting ? 'Đang lưu...' : 'Xác nhận cam kết'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
