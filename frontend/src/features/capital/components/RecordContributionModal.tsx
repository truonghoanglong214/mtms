import React, { useState, useEffect } from 'react';
import { X, DollarSign, AlertCircle, ShieldCheck } from 'lucide-react';
import { CapitalCommitment, ContributionType, RecordContributionRequest } from '../types/capital.types';
import { capitalApi } from '../api/capitalApi';
import { financeApi } from '@/features/finance/api/financeApi';
import { FinancialAccountDto } from '@/features/finance/types/finance.types';

interface Props {
  branchId: string;
  commitment: CapitalCommitment | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const RecordContributionModal: React.FC<Props> = ({
  branchId,
  commitment,
  isOpen,
  onClose,
  onSuccess
}) => {
  const [contributedOn, setContributedOn] = useState(new Date().toISOString().split('T')[0]);
  const [contributionType, setContributionType] = useState<ContributionType>('CASH');
  const [recognizedValue, setRecognizedValue] = useState('');
  const [accountId, setAccountId] = useState('');
  const [accounts, setAccounts] = useState<FinancialAccountDto[]>([]);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen && branchId) {
      financeApi.getAccounts(branchId).then((accs) => {
        setAccounts(accs);
        if (accs.length > 0) setAccountId(accs[0].id);
      }).catch(console.error);
    }
  }, [isOpen, branchId]);

  if (!isOpen || !commitment) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseFloat(recognizedValue);
    if (isNaN(val) || val <= 0) {
      setError('Số tiền/giá trị góp vốn phải lớn hơn 0');
      return;
    }
    if (contributionType === 'CASH' && !accountId) {
      setError('Vui lòng chọn tài khoản quỹ chi nhánh nhận tiền');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const payload: RecordContributionRequest = {
        contributedOn,
        contributionType,
        recognizedValue: val,
        accountId: contributionType === 'CASH' ? accountId : undefined,
        notes: notes.trim() || undefined
      };
      await capitalApi.recordContribution(branchId, commitment.id, payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.message || 'Lỗi khi ghi nhận góp vốn');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-xl bg-white p-6 shadow-xl dark:bg-neutral-900">
        <div className="flex items-center justify-between border-b pb-3 dark:border-neutral-800">
          <div className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-100 text-emerald-600 dark:bg-emerald-950 dark:text-emerald-400">
              <DollarSign className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-semibold text-neutral-900 dark:text-neutral-100">Ghi nhận đợt góp vốn thực tế</h3>
              <p className="text-xs text-neutral-500">{commitment.investorName} - HĐ: {commitment.agreementRef}</p>
            </div>
          </div>
          <button onClick={onClose} className="rounded-lg p-1 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-600 dark:hover:bg-neutral-800">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Status Callout */}
        <div className="mt-3 rounded-lg bg-neutral-50 p-3 text-xs text-neutral-600 dark:bg-neutral-800/60 dark:text-neutral-400">
          <div className="flex justify-between">
            <span>Tổng cam kết: <strong>{new Intl.NumberFormat('vi-VN').format(commitment.committedValue)} ₫</strong></span>
            <span>Đã góp: <strong className="text-emerald-600">{new Intl.NumberFormat('vi-VN').format(commitment.actualContributedValue)} ₫</strong></span>
            <span>Còn nợ: <strong className="text-amber-600">{new Intl.NumberFormat('vi-VN').format(commitment.outstandingValue)} ₫</strong></span>
          </div>
        </div>

        {error && (
          <div className="mt-3 flex items-center gap-2 rounded-lg bg-rose-50 p-3 text-sm text-rose-700 dark:bg-rose-950 dark:text-rose-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Ngày góp vốn <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                required
                value={contributedOn}
                onChange={(e) => setContributedOn(e.target.value)}
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Hình thức góp <span className="text-rose-500">*</span>
              </label>
              <select
                value={contributionType}
                onChange={(e) => setContributionType(e.target.value as ContributionType)}
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              >
                <option value="CASH">Tiền mặt / Ngân hàng (Sổ quỹ)</option>
                <option value="ASSET">Tài sản / Thiết bị (Không tăng tiền)</option>
                <option value="WORK">Công sức / Trí tuệ (Định giá)</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
              Giá trị góp thực tế (VND) <span className="text-rose-500">*</span>
            </label>
            <input
              type="number"
              required
              min="1000"
              step="100000"
              value={recognizedValue}
              onChange={(e) => setRecognizedValue(e.target.value)}
              placeholder="VD: 60000000 (TC-032)"
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm font-semibold text-emerald-600 focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800 dark:text-emerald-400"
            />
          </div>

          {contributionType === 'CASH' && (
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Tài khoản quỹ chi nhánh nhận tiền <span className="text-rose-500">*</span>
              </label>
              <select
                required
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              >
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountName} ({acc.accountKind}) - Số dư: {new Intl.NumberFormat('vi-VN').format(Number(acc.currentBalance || 0))} ₫
                  </option>
                ))}
              </select>
              <p className="mt-1 text-[11px] text-neutral-500">
                * Tự động sinh bút toán sổ quỹ dòng vốn (CAP_INJECTION / INFLOW), không ghi nhận vào doanh thu P&L.
              </p>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">Nội dung ghi chú đợt góp</label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Số chứng từ chuyển khoản, ủy nhiệm chi, biên lai..."
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>

          <div className="flex items-center gap-2 rounded-lg bg-emerald-50 p-2.5 text-xs text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
            <ShieldCheck className="h-4 w-4 shrink-0" />
            <span>Tiến độ cam kết tự động tính toán lại ngay sau khi ghi nhận thành công.</span>
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
              className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
            >
              {submitting ? 'Đang ghi nhận...' : 'Lưu đợt góp vốn'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
