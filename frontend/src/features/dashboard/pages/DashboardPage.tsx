import { useState } from 'react'
import {
  DollarSign,
  TrendingUp,
  Package,
  Layers,
  AlertTriangle,
  Clock,
  ArrowUpRight,
  ShieldAlert,
  Building2,
  PlusCircle,
  FileSpreadsheet
} from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts'

// Dữ liệu mẫu minh họa xu hướng doanh thu theo ngày (Mock Prototype Data)
const revenueTrendData = [
  { day: '08/11', netRevenue: 42500000, cash: 13500000, bank: 16000000, appNet: 13000000 },
  { day: '09/11', netRevenue: 48200000, cash: 15100000, bank: 18400000, appNet: 14700000 },
  { day: '10/11', netRevenue: 46100000, cash: 14200000, bank: 17800000, appNet: 14100000 },
  { day: '11/11', netRevenue: 51800000, cash: 16500000, bank: 20100000, appNet: 15200000 },
  { day: '12/11', netRevenue: 49400000, cash: 15400000, bank: 19000000, appNet: 15000000 },
  { day: '13/11', netRevenue: 47900000, cash: 14900000, bank: 18200000, appNet: 14800000 },
  { day: '14/11', netRevenue: 48520000, cash: 15200000, bank: 18520000, appNet: 14800000 },
]

// Dữ liệu cơ cấu kênh thanh toán (Tách biệt tiền mặt, ngân hàng và NET ứng dụng)
const channelShareData = [
  { name: 'Chuyển khoản NH', value: 18520000, color: '#304638' },
  { name: 'Tiền mặt tại két', value: 15200000, color: '#819B78' },
  { name: 'ShopeeFood NET', value: 8700000, color: '#B96849' },
  { name: 'GrabFood NET', value: 4600000, color: '#D18A6A' },
  { name: 'BeFood NET', value: 1500000, color: '#A96A17' },
]

export function DashboardPage() {
  const [timeRange, setTimeRange] = useState<'today' | 'week' | 'month'>('week')

  return (
    <div className="space-y-6">
      {/* ======================================================== */}
      {/* 1. PAGE HEADER: TIÊU ĐỀ & BỘ LỌC NGỮ CẢNH               */}
      {/* ======================================================== */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-[#E5DED4]">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-[#352D26]">
            Tổng quan điều hành
          </h1>
          <p className="text-xs sm:text-sm text-[#786C60] mt-1">
            Hệ thống kế toán quản trị và kiểm soát dòng tiền chuỗi Lục Trà Tinh Hoa.
          </p>
        </div>

        {/* Bộ lọc thời gian */}
        <div className="flex items-center gap-1.5 p-1 rounded-xl bg-white border border-[#E5DED4] shadow-2xs self-start sm:self-auto">
          <button
            type="button"
            onClick={() => setTimeRange('today')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              timeRange === 'today'
                ? 'bg-[#B96849] text-white shadow-xs'
                : 'text-[#786C60] hover:text-[#352D26]'
            }`}
          >
            Hôm nay
          </button>
          <button
            type="button"
            onClick={() => setTimeRange('week')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              timeRange === 'week'
                ? 'bg-[#B96849] text-white shadow-xs'
                : 'text-[#786C60] hover:text-[#352D26]'
            }`}
          >
            Tuần này
          </button>
          <button
            type="button"
            onClick={() => setTimeRange('month')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              timeRange === 'month'
                ? 'bg-[#B96849] text-white shadow-xs'
                : 'text-[#786C60] hover:text-[#352D26]'
            }`}
          >
            Tháng này
          </button>
        </div>
      </div>

      {/* ======================================================== */}
      {/* 2. DẢI 4 THẺ CHỈ SỐ KPI TÀI CHÍNH CỐT LÕI (HIGH-DENSITY)  */}
      {/* ======================================================== */}
      <div className="grid gap-4 sm:gap-5 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
        {/* KPI 1: Doanh thu tổng hợp kỳ */}
        <Card className="rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs">
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-[#786C60] uppercase tracking-wider">
              Doanh thu tổng hợp
            </CardTitle>
            <div className="p-2 rounded-xl bg-[#304638]/10 text-[#304638]">
              <DollarSign className="w-4 h-4" />
            </div>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="text-2xl sm:text-3xl font-bold font-mono tabular-nums text-[#352D26]">
              48.520.000 ₫
            </div>
            <div className="pt-1 border-t border-[#E5DED4]/60 text-[11px] text-[#786C60] space-y-0.5">
              <div className="flex justify-between">
                <span>Tiền mặt tại két:</span>
                <span className="font-mono text-[#352D26]">15.200.000 ₫</span>
              </div>
              <div className="flex justify-between">
                <span>Chuyển khoản NH:</span>
                <span className="font-mono text-[#352D26]">18.520.000 ₫</span>
              </div>
              <div className="flex justify-between">
                <span>App NET (Shopee/Grab):</span>
                <span className="font-mono font-medium text-[#B96849]">14.800.000 ₫</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* KPI 2: Chi phí hoạt động kỳ */}
        <Card className="rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs">
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-[#786C60] uppercase tracking-wider">
              Chi phí hoạt động
            </CardTitle>
            <div className="p-2 rounded-xl bg-[#B7423E]/10 text-[#B7423E]">
              <Layers className="w-4 h-4" />
            </div>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="text-2xl sm:text-3xl font-bold font-mono tabular-nums text-[#352D26]">
              18.240.000 ₫
            </div>
            <div className="pt-1 border-t border-[#E5DED4]/60 text-[11px] text-[#786C60] space-y-0.5">
              <div className="flex justify-between">
                <span>Giá vốn NVL xuất kho:</span>
                <span className="font-mono text-[#352D26]">12.100.000 ₫</span>
              </div>
              <div className="flex justify-between">
                <span>Lương tạm tính (Service):</span>
                <span className="font-mono text-[#352D26]">4.540.000 ₫</span>
              </div>
              <div className="flex justify-between">
                <span>Chi phí quản lý & khác:</span>
                <span className="font-mono text-[#352D26]">1.600.000 ₫</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* KPI 3: Lợi nhuận Quản trị (BẮT BUỘC NHÃN TẠM TÍNH) */}
        <Card className="rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs relative overflow-hidden">
          <div className="absolute top-0 right-0 left-0 h-1 bg-[#A96A17]" />
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-[#786C60] uppercase tracking-wider">
              Lợi nhuận Quản trị
            </CardTitle>
            <Badge
              variant="provisional"
              className="py-0.5 px-2 text-[10px] font-bold"
            >
              TẠM TÍNH
            </Badge>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="text-2xl sm:text-3xl font-bold font-mono tabular-nums text-[#287548]">
              +30.280.000 ₫
            </div>
            <p className="text-[11px] text-[#786C60] leading-snug pt-1 border-t border-[#E5DED4]/60">
              Chưa bao gồm nghĩa vụ thuế cuối kỳ & không dùng để phân phối cổ đông trước cổng G5.
            </p>
          </CardContent>
        </Card>

        {/* KPI 4: Giá trị Tồn kho Moving WAC */}
        <Card className="rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs">
          <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
            <CardTitle className="text-xs font-semibold text-[#786C60] uppercase tracking-wider">
              Giá trị Tồn kho WAC
            </CardTitle>
            <div className="p-2 rounded-xl bg-[#819B78]/15 text-[#304638]">
              <Package className="w-4 h-4" />
            </div>
          </CardHeader>
          <CardContent className="space-y-2">
            <div className="text-2xl sm:text-3xl font-bold font-mono tabular-nums text-[#352D26]">
              64.150.000 ₫
            </div>
            <div className="pt-1 border-t border-[#E5DED4]/60 text-[11px] text-[#786C60] space-y-0.5">
              <div className="flex justify-between">
                <span>Mặt hàng theo dõi:</span>
                <span className="font-mono text-[#352D26]">42 danh mục</span>
              </div>
              <div className="flex justify-between text-[#B7423E]">
                <span>Lô cận hạn (≤ 5 ngày):</span>
                <span className="font-mono font-bold">2 lô cần dùng</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* ======================================================== */}
      {/* 3. BIỂU ĐỒ DOANH THU & PHÂN TÍCH CƠ CẤU KÊNH (60% / 40%) */}
      {/* ======================================================== */}
      <div className="grid gap-6 grid-cols-1 lg:grid-cols-12">
        {/* Cột trái (60% - 7/12 cols): Biểu đồ xu hướng doanh thu */}
        <Card className="lg:col-span-7 rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs">
          <CardHeader className="flex flex-row items-center justify-between pb-4">
            <div>
              <CardTitle className="text-base font-bold text-[#352D26]">
                Xu hướng Doanh thu tổng hợp theo ngày
              </CardTitle>
              <p className="text-xs text-[#786C60] mt-0.5">
                Đường nét đứt biểu thị Doanh thu NET toàn chuỗi Lục Trà Tinh Hoa
              </p>
            </div>
            <div className="flex items-center gap-2 text-xs font-semibold text-[#287548] bg-green-50 px-2.5 py-1 rounded-lg border border-green-200">
              <TrendingUp className="w-3.5 h-3.5" />
              <span>+6.4% so kỳ trước</span>
            </div>
          </CardHeader>
          <CardContent>
            <div className="h-[280px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={revenueTrendData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorNet" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#B96849" stopOpacity={0.25} />
                      <stop offset="95%" stopColor="#B96849" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5DED4" />
                  <XAxis
                    dataKey="day"
                    stroke="#786C60"
                    fontSize={11}
                    tickLine={false}
                    axisLine={{ stroke: '#E5DED4' }}
                  />
                  <YAxis
                    stroke="#786C60"
                    fontSize={11}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) => `${value / 1000000}M`}
                  />
                  <Tooltip
                    formatter={(value: any) => [`${Number(value).toLocaleString('vi-VN')} ₫`, 'Doanh thu NET']}
                    contentStyle={{
                      backgroundColor: '#FFFCF8',
                      borderColor: '#E5DED4',
                      borderRadius: '0.75rem',
                      fontSize: '12px',
                      color: '#352D26'
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="netRevenue"
                    stroke="#B96849"
                    strokeWidth={2.5}
                    fillOpacity={1}
                    fill="url(#colorNet)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        {/* Cột phải (40% - 5/12 cols): Donut cơ cấu kênh doanh thu */}
        <Card className="lg:col-span-5 rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs">
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-bold text-[#352D26]">
              Cơ cấu Doanh thu theo Kênh
            </CardTitle>
            <p className="text-xs text-[#786C60]">
              Tách bạch tiền ngân hàng, tiền mặt tại két và NET ứng dụng
            </p>
          </CardHeader>
          <CardContent className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="h-[200px] w-[200px] shrink-0">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={channelShareData}
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={80}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {channelShareData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value: any) => [`${Number(value).toLocaleString('vi-VN')} ₫`, '']}
                    contentStyle={{
                      backgroundColor: '#FFFCF8',
                      borderColor: '#E5DED4',
                      borderRadius: '0.75rem',
                      fontSize: '12px'
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Chú giải kênh */}
            <div className="w-full space-y-2 text-xs">
              {channelShareData.map((item) => (
                <div key={item.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: item.color }} />
                    <span className="text-[#786C60] font-medium">{item.name}</span>
                  </div>
                  <span className="font-mono text-[#352D26] font-semibold">
                    {Math.round((item.value / 48520000) * 100)}%
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* ======================================================== */}
      {/* 4. CẢNH BÁO VẬN HÀNH & THAO TÁC NHANH                   */}
      {/* ======================================================== */}
      <div className="grid gap-6 grid-cols-1 lg:grid-cols-12">
        {/* Cảnh báo vận hành (7 cols) */}
        <Card className="lg:col-span-7 rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-bold text-[#352D26] flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-[#A96A17]" />
              Cảnh báo Vận hành & Hạch toán cần xử lý
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-start gap-3 p-3 rounded-xl bg-amber-50/80 border border-amber-200/80 text-xs text-amber-900">
              <Clock className="w-4 h-4 text-[#A96A17] shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">Chi nhánh 768 Nguyễn Văn Quá (Q12):</span> Ca Tối ngày 13/11 chưa hoàn tất chốt doanh thu tổng hợp.
              </div>
            </div>

            <div className="flex items-start gap-3 p-3 rounded-xl bg-red-50/80 border border-red-200/80 text-xs text-red-900">
              <ShieldAlert className="w-4 h-4 text-[#B7423E] shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">Cảnh báo Hạn dùng Kho:</span> 2 Lô Trà Ô long Mộc Sương (Lô #OL-2605) còn 4 ngày sử dụng. Cần ưu tiên xuất trước (FEFO).
              </div>
            </div>

            <div className="flex items-start gap-3 p-3 rounded-xl bg-green-50/80 border border-green-200/80 text-xs text-green-900">
              <Building2 className="w-4 h-4 text-[#287548] shrink-0 mt-0.5" />
              <div>
                <span className="font-bold">Đối soát Ngân hàng:</span> Toàn bộ 4 chi nhánh đã khớp số dư với sổ quỹ; chênh lệch hạch toán: <span className="font-mono font-bold">0 ₫</span>.
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Thao tác nhanh theo thẩm quyền (5 cols) */}
        <Card className="lg:col-span-5 rounded-2xl border-[#E5DED4] bg-[#FFFCF8] shadow-xs">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-bold text-[#352D26]">
              Thao tác Nhanh theo Thẩm quyền
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2.5">
            <Button
              variant="outline"
              className="w-full justify-between h-10 text-xs font-semibold border-[#E5DED4] hover:bg-[#F7F3EC] text-[#352D26] rounded-xl"
            >
              <span className="flex items-center gap-2">
                <PlusCircle className="w-4 h-4 text-[#B96849]" />
                Nhập báo cáo doanh thu ca mới
              </span>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#786C60]" />
            </Button>

            <Button
              variant="outline"
              className="w-full justify-between h-10 text-xs font-semibold border-[#E5DED4] hover:bg-[#F7F3EC] text-[#352D26] rounded-xl"
            >
              <span className="flex items-center gap-2">
                <PlusCircle className="w-4 h-4 text-[#304638]" />
                Lập phiếu chi tiền mặt / ngân hàng
              </span>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#786C60]" />
            </Button>

            <Button
              variant="outline"
              className="w-full justify-between h-10 text-xs font-semibold border-[#E5DED4] hover:bg-[#F7F3EC] text-[#352D26] rounded-xl"
            >
              <span className="flex items-center gap-2">
                <FileSpreadsheet className="w-4 h-4 text-[#819B78]" />
                Nạp file Excel kiểm kho / giờ công
              </span>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#786C60]" />
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
