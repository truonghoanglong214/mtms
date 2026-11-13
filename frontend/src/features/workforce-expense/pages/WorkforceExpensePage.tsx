import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'

export function WorkforceExpensePage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Master Nhân sự & Chi phí lương</h1>
          <p className="text-muted-foreground">
            Danh bạ nhân viên, phân bổ chi nhánh chính và ghi nhận chi phí lương tháng.
          </p>
        </div>
        <Badge variant="secondary">BR-HR-01 / ADR-014</Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Quy định nghiệp vụ</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground space-y-2">
          <p>• Nhân viên là Master Data, không cấp tài khoản đăng nhập tự phục vụ.</p>
          <p>• Không tính lương tự động qua chấm công GPS (GPS chỉ là chứng từ tham chiếu).</p>
          <p>• Chi phí lương gán chính xác theo chi nhánh được Chủ chuỗi chỉ định.</p>
        </CardContent>
      </Card>
    </div>
  )
}
