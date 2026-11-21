import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  X,
  PackagePlus,
  AlertTriangle,
  Calendar,
  DollarSign,
  MapPin,
  Tag,
  Hash,
  Sparkles,
  Loader2,
  ShieldCheck,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone';
import { assetApi } from '../api/assetApi';
import {
  ASSET_CATEGORY_LABELS,
  AssetCategory,
  AssetStatus,
  CreateEquipmentAssetRequest,
  EquipmentAsset,
} from '../types/asset.types';

interface CreateAssetModalProps {
  isOpen: boolean;
  branchId: string;
  onClose: () => void;
  onSuccess: (asset: EquipmentAsset) => void;
}

const CATEGORY_PREFIXES: Record<AssetCategory, string> = {
  COFFEE_MACHINE: 'CF',
  REFRIGERATION: 'RF',
  SEALING_MACHINE: 'SL',
  BLENDER: 'BL',
  POS_HARDWARE: 'POS',
  FURNITURE: 'FUR',
  KITCHEN_EQUIPMENT: 'KT',
  OTHER: 'OTH',
};

export const CreateAssetModal: React.FC<CreateAssetModalProps> = ({
  isOpen,
  branchId,
  onClose,
  onSuccess,
}) => {
  const [assetCode, setAssetCode] = useState('');
  const [name, setName] = useState('');
  const [category, setCategory] = useState<AssetCategory>('COFFEE_MACHINE');
  const [serialNumber, setSerialNumber] = useState('');
  const [model, setModel] = useState('');
  const [purchasedOn, setPurchasedOn] = useState(new Date().toISOString().split('T')[0]);
  const [acquisitionCostStr, setAcquisitionCostStr] = useState('');
  const [warrantyUntil, setWarrantyUntil] = useState('');
  const [status, setStatus] = useState<AssetStatus>('IN_USE');
  const [locationInBranch, setLocationInBranch] = useState('Quầy pha chế');
  const [maintenanceNote, setMaintenanceNote] = useState('');
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generateAssetCode = (cat: AssetCategory) => {
    const prefix = CATEGORY_PREFIXES[cat] || 'AST';
    const rand = Math.floor(100 + Math.random() * 900);
    setAssetCode(`AST-${prefix}-${rand}`);
  };

  const handleAcquisitionCostChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value.replace(/[^0-9]/g, '');
    if (!raw) {
      setAcquisitionCostStr('');
      return;
    }
    const num = parseInt(raw, 10);
    setAcquisitionCostStr(num.toLocaleString('vi-VN'));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!assetCode.trim()) {
      setError('Vui lòng nhập hoặc sinh mã tài sản');
      return;
    }
    if (!name.trim()) {
      setError('Vui lòng nhập tên thiết bị');
      return;
    }

    const cleanCost = acquisitionCostStr
      ? parseFloat(acquisitionCostStr.replace(/\./g, ''))
      : 0;

    if (cleanCost < 0) {
      setError('Nguyên giá không được âm');
      return;
    }

    const payload: CreateEquipmentAssetRequest = {
      assetCode: assetCode.trim(),
      name: name.trim(),
      category,
      serialNumber: serialNumber.trim() || undefined,
      model: model.trim() || undefined,
      purchasedOn: purchasedOn || undefined,
      acquisitionCost: cleanCost,
      warrantyUntil: warrantyUntil || undefined,
      status,
      locationInBranch: locationInBranch.trim() || undefined,
      maintenanceNote: maintenanceNote.trim() || undefined,
      evidenceFileId: evidenceFileId || undefined,
    };

    setLoading(true);
    try {
      const created = await assetApi.createAsset(branchId, payload);
      onSuccess(created);
      onClose();
    } catch (err: any) {
      const detail = err.response?.data?.detail || err.response?.data?.message || err.message;
      setError(detail || 'Không thể tạo mới tài sản');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="relative w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden my-8"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-primary/10 text-primary">
                <PackagePlus className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-900 dark:text-white">
                  Đăng ký thiết bị & tài sản mới
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Chuẩn hóa danh mục thiết bị, số serial và hồ sơ bảo hành
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              disabled={loading}
              className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-4 max-h-[80vh] overflow-y-auto">
            {error && (
              <div className="p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 flex items-start gap-2.5 text-rose-700 dark:text-rose-300 text-sm">
                <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Row 1: Code and Name */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
                    Mã tài sản <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => generateAssetCode(category)}
                    className="text-[11px] text-primary hover:underline flex items-center gap-1 font-medium"
                  >
                    <Sparkles className="w-3 h-3" /> Gợi ý
                  </button>
                </div>
                <div className="relative">
                  <Hash className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    required
                    placeholder="VD: AST-CF-001"
                    value={assetCode}
                    onChange={(e) => setAssetCode(e.target.value.toUpperCase())}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white uppercase font-mono"
                  />
                </div>
              </div>

              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Tên thiết bị / tài sản <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <Tag className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    required
                    placeholder="VD: Máy pha cà phê Nuova Simonelli Appia II"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>
            </div>

            {/* Row 2: Category and Status */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Phân loại nhóm tài sản <span className="text-rose-500">*</span>
                </label>
                <select
                  value={category}
                  onChange={(e) => {
                    const next = e.target.value as AssetCategory;
                    setCategory(next);
                    if (!assetCode || assetCode.startsWith('AST-')) {
                      generateAssetCode(next);
                    }
                  }}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  {Object.entries(ASSET_CATEGORY_LABELS).map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Trạng thái ban đầu
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as AssetStatus)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  <option value="IN_USE">Đang hoạt động (IN_USE)</option>
                  <option value="IN_REPAIR">Đang sửa chữa / Bảo hành (IN_REPAIR)</option>
                  <option value="STORED">Lưu kho / Tạm ngưng (STORED)</option>
                </select>
              </div>
            </div>

            {/* Row 3: Serial & Model */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Số Serial (Nếu có)
                </label>
                <input
                  type="text"
                  placeholder="VD: SN-2026-99881"
                  value={serialNumber}
                  onChange={(e) => setSerialNumber(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Model / Đời máy
                </label>
                <input
                  type="text"
                  placeholder="VD: Appia II 2 Groups"
                  value={model}
                  onChange={(e) => setModel(e.target.value)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            {/* Row 4: Purchased Date & Cost */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Ngày mua sắm
                </label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="date"
                    value={purchasedOn}
                    onChange={(e) => setPurchasedOn(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Nguyên giá ban đầu (VNĐ)
                </label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    placeholder="0"
                    value={acquisitionCostStr}
                    onChange={handleAcquisitionCostChange}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-medium text-emerald-600 dark:text-emerald-400"
                  />
                </div>
              </div>
            </div>

            {/* Row 5: Warranty & Location */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Hạn bảo hành đến ngày
                </label>
                <div className="relative">
                  <ShieldCheck className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="date"
                    value={warrantyUntil}
                    onChange={(e) => setWarrantyUntil(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Vị trí lắp đặt tại quán
                </label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    placeholder="VD: Quầy pha chế, Quầy thu ngân, Bếp..."
                    value={locationInBranch}
                    onChange={(e) => setLocationInBranch(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>
            </div>

            {/* Maintenance note */}
            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                Ghi chú bảo trì / Hướng dẫn sử dụng
              </label>
              <textarea
                rows={2}
                placeholder="VD: Chu kỳ thay lọc nước 6 tháng/lần, số điện thoại kỹ thuật viên bảo dưỡng..."
                value={maintenanceNote}
                onChange={(e) => setMaintenanceNote(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white resize-none"
              />
            </div>

            {/* Evidence attachment */}
            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                Chứng từ đính kèm (Hóa đơn mua / Phiếu bảo hành)
              </label>
              <EvidenceDropzone
                branchId={branchId}
                onUploadSuccess={(file) => setEvidenceFileId(file.id)}
              />
            </div>

            {/* Boundary Notice */}
            <div className="p-3 rounded-xl bg-amber-50/60 dark:bg-amber-950/30 border border-amber-200/60 dark:border-amber-800/40 text-xs text-amber-800 dark:text-amber-300">
              <span className="font-semibold">Lưu ý nghiệp vụ:</span> Giai đoạn hiện tại ghi nhận nguyên giá và bảo hành để quản lý hiện vật, <strong>chưa tự động trích chi phí khấu hao</strong> vào Báo cáo P&L tạm tính.
            </div>

            {/* Footer Buttons */}
            <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-800">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={loading}
              >
                Hủy
              </Button>
              <Button type="submit" disabled={loading} className="gap-2">
                {loading && <Loader2 className="w-4 h-4 animate-spin" />}
                Lưu tài sản
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
