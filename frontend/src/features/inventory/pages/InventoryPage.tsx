import { Link } from 'react-router-dom'
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Package, ArrowRight, Calculator, Layers, Scale, Truck, Coins, Lock, CheckCircle2, Receipt, CreditCard, ShieldAlert, FileSpreadsheet, BarChart3, ShieldCheck } from 'lucide-react'

export function InventoryPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Quản lý Kho & Giá vốn WAC</h1>
          <p className="text-muted-foreground">
            Danh mục nguyên vật liệu, số dư đầu kỳ, phiếu nhập xuất kho thủ công và kiểm kê định kỳ.
          </p>
        </div>
      </div>

      {/* Quick Launch Cards Grid */}
      <div className="grid grid-cols-1 gap-4">
        {/* Card for P0-INV-06 & P0-INV-07 Inventory Reporting, Movements Ledger & COGS Reconciliation */}
        <div className="rounded-2xl border-2 border-emerald-400 bg-gradient-to-r from-emerald-100/70 via-white to-slate-50 p-6 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#304638] text-white">
                <BarChart3 className="h-4 w-4" />
              </span>
              <h3 className="text-base font-bold text-slate-900">
                Báo Cáo Kho, Sổ Chuyển Động & Đối Soát Giá Vốn
              </h3>
              <Badge variant="outline" className="border-emerald-600 text-emerald-800 bg-white text-[10px] font-mono">
                Mới
              </Badge>
            </div>
            <p className="text-xs text-slate-600 max-w-2xl leading-relaxed">
              Theo dõi 4 KPI tồn kho cốt lõi, top mặt hàng giá trị cao nhất, truy xuất sổ chi tiết xuất nhập tồn (Ledger)
              và kiểm chứng phương trình đối soát Giá vốn (COGS) không trùng hai lần chi phí.
            </p>
            <div className="flex flex-wrap items-center gap-3 pt-1 text-[11px] text-slate-500">
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <BarChart3 className="h-3.5 w-3.5 text-emerald-700" />
                KPI & Mặt hàng giá trị cao
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <FileSpreadsheet className="h-3.5 w-3.5 text-emerald-700" />
                Sổ biến động kho chi tiết
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <ShieldCheck className="h-3.5 w-3.5 text-emerald-700" />
                Đối soát COGS không trùng chi phí
              </span>
            </div>
          </div>

          <Link to="/inventory/reporting" className="shrink-0">
            <Button className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-sm">
              <span>Mở báo cáo & Sổ kho</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>
        {/* Card for P0-INV-03 Goods Receipt & Payables */}
        <div className="rounded-2xl border border-emerald-300 bg-gradient-to-r from-emerald-50/90 via-white to-slate-50 p-6 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#304638] text-white">
                <Receipt className="h-4 w-4" />
              </span>
              <h3 className="text-base font-bold text-slate-900">
                Nhập Kho Mua Hàng & Công Nợ NCC
              </h3>
            </div>
            <p className="text-xs text-slate-600 max-w-2xl leading-relaxed">
              Nhập hàng trực tiếp từ nhà cung cấp không cần PO, hỗ trợ quy đổi quy cách đóng gói thùng/bao,
              tự động cập nhật Moving WAC, theo dõi số lô & ghi nhận nợ NCC mà không trừ quỹ tiền mặt.
            </p>
            <div className="flex flex-wrap items-center gap-3 pt-1 text-[11px] text-slate-500">
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <CreditCard className="h-3.5 w-3.5 text-emerald-700" />
                Ghi nợ NCC
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <Calculator className="h-3.5 w-3.5 text-emerald-700" />
                Quy đổi đóng gói tự động
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-700" />
                Cập nhật Moving WAC tức thời
              </span>
            </div>
          </div>

          <Link to="/inventory/receipts" className="shrink-0">
            <Button className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-sm">
              <span>Mở sổ nhập kho & Nợ NCC</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>

        {/* Card for P0-INV-04 & P0-INV-05 Stock Issue & Moving WAC */}
        <div className="rounded-2xl border border-emerald-300 bg-gradient-to-r from-emerald-50/90 via-white to-slate-50 p-6 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#304638] text-white">
                <FileSpreadsheet className="h-4 w-4" />
              </span>
              <h3 className="text-base font-bold text-slate-900">
                Xuất Kho Thủ Công & Định Giá Vốn Moving WAC
              </h3>
            </div>
            <p className="text-xs text-slate-600 max-w-2xl leading-relaxed">
              Lập phiếu xuất quầy bar pha chế, hủy hỏng hao hụt hoặc điều chỉnh kiểm kê âm.
              Đơn giá xuất kho tự động chốt theo Giá vốn bình quân di động và khóa chặt không cho phép xuất âm.
            </p>
            <div className="flex flex-wrap items-center gap-3 pt-1 text-[11px] text-slate-500">
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <Coins className="h-3.5 w-3.5 text-emerald-700" />
                Snapshot Moving WAC
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-rose-800">
                <ShieldAlert className="h-3.5 w-3.5 text-rose-600" />
                Khóa chặt chống tồn âm
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-700" />
                An toàn tranh chấp đồng thời
              </span>
            </div>
          </div>

          <Link to="/inventory/issues" className="shrink-0">
            <Button className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-sm">
              <span>Mở sổ xuất kho pha chế</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>
        {/* Card for P0-INV-02 Opening Inventory & WAC */}
        <div className="rounded-2xl border border-emerald-300 bg-gradient-to-r from-emerald-50/90 via-white to-slate-50 p-6 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-800 text-white">
                <Coins className="h-4 w-4" />
              </span>
              <h3 className="text-base font-bold text-slate-900">
                Tồn kho Đầu kỳ & Khóa Sổ Chi Nhánh
              </h3>
            </div>
            <p className="text-xs text-slate-600 max-w-2xl leading-relaxed">
              Khởi tạo số dư ban đầu từ biên bản kiểm kê, nạp danh sách bằng file CSV/Excel mẫu,
              tự động tính giá vốn bình quân gia quyền di động (Moving WAC) và khóa sổ post-once theo chi nhánh.
            </p>
            <div className="flex flex-wrap items-center gap-3 pt-1 text-[11px] text-slate-500">
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <Lock className="h-3.5 w-3.5 text-emerald-700" />
                Khóa sổ 1 lần (Post-Once)
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-700" />
                Chống trùng lặp số dư
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-emerald-900">
                <Coins className="h-3.5 w-3.5 text-emerald-700" />
                Sổ số dư tồn & WAC chi nhánh
              </span>
            </div>
          </div>

          <Link to="/inventory/opening" className="shrink-0">
            <Button className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-2 shadow-sm">
              <span>Mở sổ đầu kỳ & WAC</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>

        {/* Card for P0-INV-01 Catalog */}
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-slate-800 text-white">
                <Package className="h-4 w-4" />
              </span>
              <h3 className="text-base font-bold text-slate-900">
                Danh mục Mặt hàng, Quy cách & Công cụ quy đổi
              </h3>
            </div>
            <p className="text-xs text-slate-600 max-w-2xl leading-relaxed">
              Khai báo danh mục nguyên vật liệu, đơn vị tính chuẩn (Base Units), danh sách nhà cung cấp
              và thiết lập tỷ lệ quy đổi đóng gói (Pack Conversions) có snapshot chứng từ.
            </p>
            <div className="flex flex-wrap items-center gap-3 pt-1 text-[11px] text-slate-500">
              <span className="flex items-center gap-1 font-medium text-slate-700">
                <Layers className="h-3.5 w-3.5 text-slate-500" />
                Quy cách đóng gói
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-slate-700">
                <Calculator className="h-3.5 w-3.5 text-slate-500" />
                Bộ tính quy đổi (1 Thùng 24 lon → 48 Lon)
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-slate-700">
                <Truck className="h-3.5 w-3.5 text-slate-500" />
                Nhà cung cấp
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 font-medium text-slate-700">
                <Scale className="h-3.5 w-3.5 text-slate-500" />
                Đơn vị đo lường chuẩn
              </span>
            </div>
          </div>

          <Link to="/inventory/catalog" className="shrink-0">
            <Button variant="outline" className="text-xs font-semibold gap-2 border-slate-300">
              <span>Mở danh mục & Quy cách</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Quy định nghiệp vụ</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground space-y-2">
          <p>• <strong>Không cho phép tồn kho âm:</strong> Không xác nhận phiếu xuất kho vượt quá số lượng sẵn có.</p>
          <p>• <strong>Giá vốn bình quân gia quyền di động (Moving WAC):</strong> Cập nhật tức thời theo từng chi nhánh.</p>
          <p>• Xuất kho nguyên vật liệu là phiếu xuất dùng thủ công hoặc kiểm kê hao hụt có duyệt.</p>
        </CardContent>
      </Card>
    </div>
  )
}
export default InventoryPage
