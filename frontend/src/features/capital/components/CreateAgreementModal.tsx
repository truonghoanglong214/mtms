import React, { useState } from 'react';
import { X, FileSignature, AlertCircle } from 'lucide-react';
import { ContributionType, CreateAgreementRequest, Investor } from '../types/capital.types';
import { capitalApi } from '../api/capitalApi';

interface Props {
  branchId: string;
  investors: Investor[];
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateAgreementModal: React.FC<Props> = ({
  branchId,
  investors,
  isOpen,
  onClose,
  onSuccess
}) => {
  const [investorId, setInvestorId] = useState('');
  const [agreementRef, setAgreementRef] = useState('');
  const [contributionType, setContributionType] = useState<ContributionType>('CASH');
  const [effectiveFrom, setEffectiveFrom] = useState(new Date().toISOString().split('T')[0]);
  const [effectiveTo, setEffectiveTo] = useState('');
  const [eligibilityTerms, setEligibilityTerms] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!investorId) {
      setError('Vui lòng chọn nhà đầu tư');
      return;
    }
    if (!agreementRef.trim()) {
      setError('Số hợp đồng/thỏa thuận không được để trống');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const payload: CreateAgreementRequest = {
        investorId,
        agreementRef: agreementRef.trim(),
        contributionType,
        effectiveFrom,
        effectiveTo: effectiveTo || undefined,
        eligibilityTerms: eligibilityTerms.trim() || undefined
      };
      await capitalApi.createAgreement(branchId, payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.detail || err.message || 'Lỗi khi ký kết thỏa thuận');
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
              <FileSignature className="h-5 w-5" />
            </div>
            <div>
              <h3 className="font-semibold text-neutral-900 dark:text-neutral-100">Ký hợp đồng thỏa thuận đầu tư</h3>
              <p className="text-xs text-neutral-500">Xác lập thỏa thuận góp vốn gắn chặt theo chi nhánh (TC-001)</p>
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
              Chọn Cổ đông / Nhà đầu tư <span className="text-rose-500">*</span>
            </label>
            <select
              required
              value={investorId}
              onChange={(e) => setInvestorId(e.target.value)}
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            >
              <option value="">-- Chọn cổ đông --</option>
              {investors.map((inv) => (
                <option key={inv.id} value={inv.id}>
                  {inv.displayName} {inv.contactPhone ? `(${inv.contactPhone})` : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Số / Mã hợp đồng <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={agreementRef}
                onChange={(e) => setAgreementRef(e.target.value)}
                placeholder="VD: TTDT-Q1-001/2026"
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Hình thức đóng góp <span className="text-rose-500">*</span>
              </label>
              <select
                value={contributionType}
                onChange={(e) => setContributionType(e.target.value as ContributionType)}
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              >
                <option value="CASH">Tiền mặt / Chuyển khoản</option>
                <option value="ASSET">Tài sản / Thiết bị</option>
                <option value="WORK">Công sức / Trí tuệ</option>
                <option value="MIXED">Hỗn hợp</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
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
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">Ngày kết thúc hiệu lực</label>
              <input
                type="date"
                value={effectiveTo}
                onChange={(e) => setEffectiveTo(e.target.value)}
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">Điều khoản & Phạm vi trách nhiệm</label>
            <textarea
              rows={3}
              value={eligibilityTerms}
              onChange={(e) => setEligibilityTerms(e.target.value)}
              placeholder="Quyền biểu quyết, trách nhiệm góp vốn, thỏa thuận bảo mật..."
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
              {submitting ? 'Đang tạo...' : 'Ký hợp đồng'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
