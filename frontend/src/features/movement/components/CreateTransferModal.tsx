import React, { useState, useEffect } from 'react';
import { X, ArrowRightLeft, Plus, Trash2, AlertCircle, ShieldAlert, Building2 } from 'lucide-react';
import { apiClient } from '@/shared/api/apiClient';
import { movementApi } from '../api/movementApi';
import { CreateTransferPayload } from '../types/movement.types';

interface BranchOption {
  id: string;
  name: string;
  code?: string;
  businessEntityId?: string;
}

interface StockBalanceOption {
  itemId: string;
  itemName: string;
  itemCode: string;
  baseUnit: string;
  quantity: number;
  avgUnitCost: number;
}

interface LineItemInput {
  itemId: string;
  dispatchedBaseQty: string;
  notes: string;
  availableQty: number;
  avgUnitCost: number;
  baseUnit: string;
}

interface Props {
  fromBranchId: string;
  businessEntityId: string;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateTransferModal: React.FC<Props> = ({
  fromBranchId,
  businessEntityId,
  isOpen,
  onClose,
  onSuccess
}) => {
  const [branches, setBranches] = useState<BranchOption[]>([]);
  const [stockItems, setStockItems] = useState<StockBalanceOption[]>([]);
  const [toBranchId, setToBranchId] = useState('');
  const [requestedOn, setRequestedOn] = useState(new Date().toISOString().split('T')[0]);
  const [autoDispatch, setAutoDispatch] = useState(true);
  const [notes, setNotes] = useState('');
  const [lines, setLines] = useState<LineItemInput[]>([
    { itemId: '', dispatchedBaseQty: '', notes: '', availableQty: 0, avgUnitCost: 0, baseUnit: '' }
  ]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) return;

    const loadMetadata = async () => {
      try {
        const [branchRes, stockRes] = await Promise.all([
          apiClient.get<BranchOption[]>('/branches'),
          apiClient.get<{ items: StockBalanceOption[] }>(`/branches/${fromBranchId}/inventory/balances?size=100`).catch(() => ({ data: { items: [] } }))
        ]);

        const rawBranches = Array.isArray(branchRes.data)
          ? branchRes.data
          : (branchRes.data as unknown as { data: BranchOption[] })?.data || [];

        // TC-028 Filter: Only show branches of the SAME business entity
        const sameEntityBranches = rawBranches.filter(
          (b) => b.id !== fromBranchId && (!b.businessEntityId || b.businessEntityId === businessEntityId)
        );
        setBranches(sameEntityBranches);
        if (sameEntityBranches.length > 0) {
          setToBranchId(sameEntityBranches[0].id);
        }

        const rawStock = stockRes.data?.items || (stockRes.data as unknown as StockBalanceOption[]) || [];
        setStockItems(Array.isArray(rawStock) ? rawStock : []);
      } catch (err) {
        console.error('Failed to load branches or stock balances:', err);
      }
    };

    loadMetadata();
  }, [isOpen, fromBranchId, businessEntityId]);

  if (!isOpen) return null;

  const handleItemChange = (index: number, itemId: string) => {
    const found = stockItems.find((s) => s.itemId === itemId);
    const updated = [...lines];
    updated[index] = {
      ...updated[index],
      itemId,
      availableQty: found?.quantity || 0,
      avgUnitCost: found?.avgUnitCost || 0,
      baseUnit: found?.baseUnit || ''
    };
    setLines(updated);
  };

  const handleQtyChange = (index: number, qtyStr: string) => {
    const updated = [...lines];
    updated[index].dispatchedBaseQty = qtyStr;
    setLines(updated);
  };

  const handleLineNotesChange = (index: number, val: string) => {
    const updated = [...lines];
    updated[index].notes = val;
    setLines(updated);
  };

  const addLine = () => {
    setLines([...lines, { itemId: '', dispatchedBaseQty: '', notes: '', availableQty: 0, avgUnitCost: 0, baseUnit: '' }]);
  };

  const removeLine = (index: number) => {
    if (lines.length === 1) return;
    setLines(lines.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!toBranchId) {
      setError('Vui lòng chọn chi nhánh đích nhận hàng');
      return;
    }

    if (toBranchId === fromBranchId) {
      setError('Chi nhánh nhận không được trùng với chi nhánh gửi');
      return;
    }

    const validLines = lines.filter((l) => l.itemId && Number(l.dispatchedBaseQty) > 0);
    if (validLines.length === 0) {
      setError('Vui lòng chọn ít nhất một mặt hàng và nhập số lượng xuất hợp lệ');
      return;
    }

    // Check available stock (TC-021)
    for (const l of validLines) {
      const qty = Number(l.dispatchedBaseQty);
      if (qty > l.availableQty) {
        const item = stockItems.find((s) => s.itemId === l.itemId);
        setError(
          `Số lượng xuất cho mặt hàng "${item?.itemName || 'Đã chọn'}" (${qty}) vượt quá tồn kho khả dụng (${l.availableQty}) (TC-021)`
        );
        return;
      }
    }

    setSubmitting(true);
    try {
      const payload: CreateTransferPayload = {
        toBranchId,
        requestedOn,
        autoDispatch,
        notes: notes.trim() || undefined,
        lines: validLines.map((l) => ({
          itemId: l.itemId,
          dispatchedBaseQty: Number(l.dispatchedBaseQty),
          notes: l.notes.trim() || undefined
        }))
      };

      await movementApi.createTransfer(fromBranchId, payload);
      onSuccess();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Lỗi khi tạo phiếu điều chuyển';
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
            <div className="rounded-lg bg-primary-100 p-2 text-primary-600 dark:bg-primary-950 dark:text-primary-400">
              <ArrowRightLeft className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
                Tạo Phiếu Điều Chuyển Hàng Nội Bộ
              </h3>
              <p className="text-xs text-neutral-500">Chuyển nguyên vật liệu giữa các chi nhánh cùng pháp nhân (TC-028)</p>
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
          {/* TC-028 Notice */}
          <div className="flex items-start gap-2.5 rounded-xl border border-blue-200 bg-blue-50 p-3.5 text-xs text-blue-900 dark:border-blue-900 dark:bg-blue-950/40 dark:text-blue-300">
            <ShieldAlert className="h-4 w-4 shrink-0 text-blue-600 dark:text-blue-400" />
            <span>
              <strong>QUY ĐỊNH PHÁP NHÂN (TC-028):</strong> Điều chuyển kho nội bộ chỉ áp dụng cho các chi nhánh thuộc cùng pháp nhân kinh doanh. Đơn giá Moving WAC tại thời điểm xuất sẽ được chụp ảnh snapshot và chuyển sang chi nhánh nhận.
            </span>
          </div>

          {error && (
            <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-3.5 text-xs text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-400">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Chi nhánh đích nhận hàng <span className="text-rose-500">*</span>
              </label>
              <div className="mt-1 flex items-center gap-2 rounded-lg border border-neutral-300 px-3 py-2 text-sm dark:border-neutral-700 dark:bg-neutral-800">
                <Building2 className="h-4 w-4 text-neutral-400" />
                <select
                  required
                  value={toBranchId}
                  onChange={(e) => setToBranchId(e.target.value)}
                  className="w-full bg-transparent text-sm focus:outline-none dark:text-neutral-200"
                >
                  {branches.length === 0 ? (
                    <option value="">Không có chi nhánh cùng pháp nhân</option>
                  ) : (
                    branches.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name} ({b.code || 'BR'})
                      </option>
                    ))
                  )}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
                Ngày yêu cầu chuyển kho <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                required
                value={requestedOn}
                onChange={(e) => setRequestedOn(e.target.value)}
                className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
              />
            </div>
          </div>

          {/* Lines Table */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold uppercase tracking-wider text-neutral-500">
                Danh sách nguyên vật liệu chuyển đi
              </label>
              <button
                type="button"
                onClick={addLine}
                className="flex items-center gap-1 rounded-lg border border-primary-600 px-2.5 py-1 text-xs font-medium text-primary-600 hover:bg-primary-50 dark:border-primary-400 dark:text-primary-400"
              >
                <Plus className="h-3.5 w-3.5" />
                Thêm mặt hàng
              </button>
            </div>

            <div className="space-y-3">
              {lines.map((line, index) => (
                <div
                  key={index}
                  className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-neutral-50/50 p-3.5 dark:border-neutral-800 dark:bg-neutral-850 sm:flex-row sm:items-center"
                >
                  <div className="flex-1">
                    <select
                      value={line.itemId}
                      onChange={(e) => handleItemChange(index, e.target.value)}
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
                    >
                      <option value="">-- Chọn mặt hàng xuất kho --</option>
                      {stockItems.map((item) => (
                        <option key={item.itemId} value={item.itemId}>
                          {item.itemName} ({item.itemCode}) - Tồn: {item.quantity} {item.baseUnit}
                        </option>
                      ))}
                    </select>
                    {line.itemId && (
                      <div className="mt-1 flex items-center gap-3 text-[11px] text-neutral-500">
                        <span>
                          Tồn khả dụng: <strong>{line.availableQty} {line.baseUnit}</strong>
                        </span>
                        <span>
                          Đơn vị giá WAC: <strong>{new Intl.NumberFormat('vi-VN').format(line.avgUnitCost)} ₫</strong>
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="w-full sm:w-36">
                    <input
                      type="number"
                      step="0.001"
                      min="0.001"
                      placeholder="Số lượng"
                      value={line.dispatchedBaseQty}
                      onChange={(e) => handleQtyChange(index, e.target.value)}
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
                    />
                  </div>

                  <div className="flex-1">
                    <input
                      type="text"
                      placeholder="Ghi chú dòng (tùy chọn)"
                      value={line.notes}
                      onChange={(e) => handleLineNotesChange(index, e.target.value)}
                      className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
                    />
                  </div>

                  {lines.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removeLine(index)}
                      className="rounded-lg p-2 text-neutral-400 hover:bg-neutral-100 hover:text-rose-500 dark:hover:bg-neutral-800"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-neutral-700 dark:text-neutral-300">
              Ghi chú toàn bộ chuyến hàng
            </label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Nhập ghi chú lý do điều chuyển hàng..."
              className="mt-1 w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none dark:border-neutral-700 dark:bg-neutral-800"
            />
          </div>

          {/* Auto dispatch checkbox */}
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="autoDispatch"
              checked={autoDispatch}
              onChange={(e) => setAutoDispatch(e.target.checked)}
              className="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
            />
            <label htmlFor="autoDispatch" className="text-xs font-medium text-neutral-700 dark:text-neutral-300">
              Xuất kho vận chuyển ngay lập tức (Chuyển trạng thái sang ĐANG VẬN CHUYỂN, trừ kho và chụp ảnh WAC)
            </label>
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
              className="rounded-lg bg-primary-600 px-5 py-2 text-sm font-semibold text-white hover:bg-primary-700 disabled:opacity-50"
            >
              {submitting ? 'Đang tạo phiếu...' : autoDispatch ? 'Tạo & Xuất kho ngay' : 'Lưu bản nháp'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
