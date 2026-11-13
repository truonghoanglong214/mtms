import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'

export function ReportingPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Báo cáo Quản trị (P&L)</h1>
          <p className="text-muted-foreground">
            Hiển thị kết quả kinh doanh theo chi nhánh và toàn chuỗi.
          </p>
        </div>
        <Badge variant="provisional">TẠM TÍNH / PROVISIONAL</Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Lưu ý quản trị rủi ro</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground space-y-2">
          <p>• Báo cáo P&L giai đoạn P0 luôn mang nhãn <strong>TẠM TÍNH</strong>.</p>
          <p>• Tuyệt đối không tự động kích hoạt chi trả lợi nhuận cho nhà đầu tư trước Gate G5.</p>
        </CardContent>
      </Card>
    </div>
  )
}
