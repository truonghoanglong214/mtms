import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  X,
  Package,
  MapPin,
  ShieldCheck,
  ShieldAlert,
  ShieldX,
  Edit,
  Building2,
  Wrench,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import {
  ASSET_CATEGORY_LABELS,
  ASSET_STATUS_LABELS,
  EquipmentAsset,
} from '../types/asset.types';

interface AssetDetailModalProps {
  isOpen: boolean;
  asset: EquipmentAsset | null;
  onClose: () => void;
  onEdit: (asset: EquipmentAsset) => void;
}

export const AssetDetailModal: React.FC<AssetDetailModalProps> = ({
  isOpen,
  asset,
  onClose,
  onEdit,
}) => {
  if (!isOpen || !asset) return null;

  const statusMeta = ASSET_STATUS_LABELS[asset.status] || {
    label: asset.status,
    color: 'text-slate-700',
    bg: 'bg-slate-100',
  };

  const getWarrantyInfo = () => {
    if (!asset.warrantyUntil) {
      return { status: 'NONE', text: 'Chưa cập nhật bảo hành', color: 'text-slate-500', icon: ShieldCheck };
    }
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const warrantyDate = new Date(asset.warrantyUntil);
    warrantyDate.setHours(0, 0, 0, 0);

    const diffDays = Math.ceil((warrantyDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
      return {
        status: 'EXPIRED',
        text: `Đã hết hạn (${Math.abs(diffDays)} ngày trước)`,
        color: 'text-rose-600 dark:text-rose-400',
        bg: 'bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800',
        icon: ShieldX,
      };
    } else if (diffDays <= 30) {
      return {
        status: 'EXPIRING',
        text: `Sắp hết hạn (còn ${diffDays} ngày)`,
        color: 'text-amber-600 dark:text-amber-400',
        bg: 'bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800',
        icon: ShieldAlert,
      };
    } else {
      return {
        status: 'ACTIVE',
        text: `Còn bảo hành (${diffDays} ngày)`,
        color: 'text-emerald-600 dark:text-emerald-400',
        bg: 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800',
        icon: ShieldCheck,
      };
    }
  };

  const warranty = getWarrantyInfo();
  const WarrantyIcon = warranty.icon;

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
                <Package className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                    {asset.name}
                  </h3>
                  <span className="px-2 py-0.5 rounded text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700">
                    {asset.assetCode}
                  </span>
                </div>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  {ASSET_CATEGORY_LABELS[asset.category] || asset.category} • {asset.branchName}
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-6 space-y-6 max-h-[80vh] overflow-y-auto">
            {/* Top Status & Valuation Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="p-3.5 rounded-xl border bg-slate-50/50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-800">
                <span className="text-xs font-medium text-slate-500 dark:text-slate-400 block mb-1">
                  Trạng thái
                </span>
                <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold border ${statusMeta.bg} ${statusMeta.color}`}>
                  {statusMeta.label}
                </span>
              </div>

              <div className="p-3.5 rounded-xl border bg-slate-50/50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-800">
                <span className="text-xs font-medium text-slate-500 dark:text-slate-400 block mb-1">
                  Nguyên giá ban đầu
                </span>
                <span className="text-base font-bold text-emerald-600 dark:text-emerald-400">
                  {Number(asset.acquisitionCost || 0).toLocaleString('vi-VN')} đ
                </span>
              </div>

              <div className={`p-3.5 rounded-xl border ${warranty.bg || 'bg-slate-50/50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-800'}`}>
                <span className="text-xs font-medium text-slate-500 dark:text-slate-400 block mb-1">
                  Tình trạng bảo hành
                </span>
                <div className="flex items-center gap-1.5">
                  <WarrantyIcon className={`w-4 h-4 ${warranty.color}`} />
                  <span className={`text-xs font-semibold ${warranty.color}`}>
                    {warranty.text}
                  </span>
                </div>
              </div>
            </div>

            {/* Technical Specs */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                Thông số kỹ thuật & Định danh
              </h4>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-4 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/30 dark:bg-slate-900/40 text-xs">
                <div>
                  <span className="text-slate-400 block mb-0.5">Số Serial:</span>
                  <span className="font-mono font-medium text-slate-800 dark:text-slate-200">
                    {asset.serialNumber || '—'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block mb-0.5">Model / Đời máy:</span>
                  <span className="font-medium text-slate-800 dark:text-slate-200">
                    {asset.model || '—'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block mb-0.5">Ngày mua:</span>
                  <span className="font-medium text-slate-800 dark:text-slate-200">
                    {asset.purchasedOn || '—'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block mb-0.5">Hạn bảo hành:</span>
                  <span className="font-medium text-slate-800 dark:text-slate-200">
                    {asset.warrantyUntil || '—'}
                  </span>
                </div>
              </div>
            </div>

            {/* Operational Location & Supplier */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
                Vị trí & Nhà cung cấp
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div className="flex items-start gap-3 p-3.5 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/30 dark:bg-slate-900/40">
                  <MapPin className="w-4 h-4 text-primary mt-0.5 shrink-0" />
                  <div>
                    <span className="text-xs text-slate-400 block">Vị trí lắp đặt:</span>
                    <span className="text-sm font-medium text-slate-800 dark:text-slate-200">
                      {asset.locationInBranch || 'Chưa định vị'}
                    </span>
                  </div>
                </div>

                <div className="flex items-start gap-3 p-3.5 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/30 dark:bg-slate-900/40">
                  <Building2 className="w-4 h-4 text-primary mt-0.5 shrink-0" />
                  <div>
                    <span className="text-xs text-slate-400 block">Nhà cung cấp:</span>
                    <span className="text-sm font-medium text-slate-800 dark:text-slate-200">
                      {asset.supplierName || 'Mua lẻ / Chưa gắn NCC'}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Maintenance Notes */}
            {asset.maintenanceNote && (
              <div className="space-y-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500 flex items-center gap-1.5">
                  <Wrench className="w-3.5 h-3.5" /> Ghi chú bảo trì & Tình trạng kỹ thuật
                </h4>
                <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 text-xs text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-wrap">
                  {asset.maintenanceNote}
                </div>
              </div>
            )}

            {/* Boundary Notice */}
            <div className="p-3.5 rounded-xl bg-amber-50/60 dark:bg-amber-950/30 border border-amber-200/60 dark:border-amber-800/40 text-xs text-amber-800 dark:text-amber-300">
              <span className="font-semibold">Quy định hạch toán:</span> Thiết bị được ghi nhận với nguyên giá phục vụ quản lý hiện vật điểm bán, <strong>không trích khấu hao vào Báo cáo P&L tạm tính</strong>.
            </div>

            {/* Footer Buttons */}
            <div className="flex items-center justify-between pt-4 border-t border-slate-100 dark:border-slate-800">
              <span className="text-xs text-slate-400">
                Tạo ngày {new Date(asset.createdAt).toLocaleDateString('vi-VN')}
              </span>
              <div className="flex items-center gap-2">
                <Button variant="outline" onClick={onClose}>
                  Đóng
                </Button>
                <Button
                  onClick={() => {
                    onClose();
                    onEdit(asset);
                  }}
                  className="gap-2"
                >
                  <Edit className="w-4 h-4" />
                  Chỉnh sửa
                </Button>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};
