import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'

export function MonthlyRevenuePage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Doanh thu tổng hợp tháng</h1>
          <p className="text-muted-foreground">
            Quản lý doanh thu tổng hợp NET theo chi nhánh và tháng kinh doanh.
          </p>
        </div>
        <Badge variant="secondary">BR-REV-01 / ADR-005</Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Quy định nghiệp vụ (Invariants)</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground space-y-2">
          <p>• <strong>NO POS:</strong> Không ghi nhận đơn hàng chi tiết từng ly trà sữa.</p>
          <p>• <strong>Doanh thu kênh giao hàng:</strong> Không tự động cộng vào tài khoản ngân hàng khi chưa có chứng từ đối soát.</p>
          <p>• Mỗi sửa đổi đều lưu lại số hiệu phiên bản revision bất biến phục vụ audit.</p>
        </CardContent>
      </Card>
    </div>
  )
}
