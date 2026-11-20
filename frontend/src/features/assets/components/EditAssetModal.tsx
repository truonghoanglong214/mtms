import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  X,
  Edit3,
  AlertTriangle,
  Calendar,
  DollarSign,
  MapPin,
  Tag,
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
  EquipmentAsset,
  UpdateEquipmentAssetRequest,
} from '../types/asset.types';

interface EditAssetModalProps {
  isOpen: boolean;
  branchId: string;
  asset: EquipmentAsset | null;
  onClose: () => void;
  onSuccess: (asset: EquipmentAsset) => void;
}

export const EditAssetModal: React.FC<EditAssetModalProps> = ({
  isOpen,
  branchId,
  asset,
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [category, setCategory] = useState<AssetCategory>('COFFEE_MACHINE');
  const [serialNumber, setSerialNumber] = useState('');
  const [model, setModel] = useState('');
  const [purchasedOn, setPurchasedOn] = useState('');
  const [acquisitionCostStr, setAcquisitionCostStr] = useState('');
  const [warrantyUntil, setWarrantyUntil] = useState('');
  const [status, setStatus] = useState<AssetStatus>('IN_USE');
  const [locationInBranch, setLocationInBranch] = useState('');
  const [maintenanceNote, setMaintenanceNote] = useState('');
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (asset) {
      setName(asset.name || '');
      setCategory(asset.category || 'COFFEE_MACHINE');
      setSerialNumber(asset.serialNumber || '');
      setModel(asset.model || '');
      setPurchasedOn(asset.purchasedOn || '');
      setAcquisitionCostStr(
        asset.acquisitionCost ? Number(asset.acquisitionCost).toLocaleString('vi-VN') : ''
      );
      setWarrantyUntil(asset.warrantyUntil || '');
      setStatus(asset.status || 'IN_USE');
      setLocationInBranch(asset.locationInBranch || '');
      setMaintenanceNote(asset.maintenanceNote || '');
      setEvidenceFileId(asset.evidenceFileId || null);
      setError(null);
    }
  }, [asset]);

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
    if (!asset) return;
    setError(null);

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

    const payload: UpdateEquipmentAssetRequest = {
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
      const updated = await assetApi.updateAsset(branchId, asset.id, payload);
      onSuccess(updated);
      onClose();
    } catch (err: any) {
      const detail = err.response?.data?.detail || err.response?.data?.message || err.message;
      setError(detail || 'Không thể cập nhật thông tin tài sản');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !asset) return null;

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
              <div className="p-2.5 rounded-xl bg-amber-500/10 text-amber-600 dark:text-amber-400">
                <Edit3 className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-slate-900 dark:text-white">
                  Chỉnh sửa thiết bị: <span className="font-mono text-primary">{asset.assetCode}</span>
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Cập nhật hiện trạng, thông số bảo trì và vị trí hoạt động
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

            {/* Row 1: Name & Category */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-2">
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Tên thiết bị / tài sản <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <Tag className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Nhóm tài sản <span className="text-rose-500">*</span>
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value as AssetCategory)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  {Object.entries(ASSET_CATEGORY_LABELS).map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Row 2: Status & Location */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Trạng thái hoạt động
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as AssetStatus)}
                  className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  <option value="IN_USE">Đang hoạt động (IN_USE)</option>
                  <option value="IN_REPAIR">Đang sửa chữa / Bảo hành (IN_REPAIR)</option>
                  <option value="STORED">Lưu kho / Tạm ngưng (STORED)</option>
                  <option value="DISPOSED">Đã thanh lý / Hủy (DISPOSED)</option>
                  <option value="TRANSFERRED">Đã điều chuyển (TRANSFERRED)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Vị trí lắp đặt tại quán
                </label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
                  <input
                    type="text"
                    value={locationInBranch}
                    onChange={(e) => setLocationInBranch(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>
            </div>

            {/* Row 3: Serial & Model */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                  Số Serial
                </label>
                <input
                  type="text"
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
                    value={acquisitionCostStr}
                    onChange={handleAcquisitionCostChange}
                    className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-medium text-emerald-600 dark:text-emerald-400"
                  />
                </div>
              </div>
            </div>

            {/* Row 5: Warranty */}
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

            {/* Maintenance note */}
            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                Ghi chú bảo trì & Tình trạng thiết bị
              </label>
              <textarea
                rows={2}
                value={maintenanceNote}
                onChange={(e) => setMaintenanceNote(e.target.value)}
                className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white resize-none"
              />
            </div>

            {/* Evidence attachment */}
            <div>
              <label className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
                Cập nhật chứng từ bảo hành / Hóa đơn mới (Nếu có)
              </label>
              <EvidenceDropzone
                branchId={branchId}
                onUploadSuccess={(file) => setEvidenceFileId(file.id)}
              />
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
                Lưu thay đổi
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
