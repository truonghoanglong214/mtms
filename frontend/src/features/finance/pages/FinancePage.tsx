import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'

export function FinancePage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Sổ quỹ & Quản lý Chi phí</h1>
          <p className="text-muted-foreground">
            Theo dõi dòng tiền mặt, tài khoản ngân hàng chi nhánh và công nợ nhà cung cấp.
          </p>
        </div>
        <Badge variant="secondary">BR-FIN-01 / ADR-007</Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Quy định nghiệp vụ</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground space-y-2">
          <p>• Tách biệt tuyệt đối giữa dòng tiền thực tế và doanh thu ghi nhận kế toán.</p>
          <p>• Chi phí vận hành gắn kèm chứng từ hóa đơn và phân loại đúng chi nhánh phát sinh.</p>
        </CardContent>
      </Card>
    </div>
  )
}
