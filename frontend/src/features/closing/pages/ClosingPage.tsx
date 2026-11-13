import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'

export function ClosingPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Chốt sổ kỳ kinh doanh</h1>
          <p className="text-muted-foreground">
            Kiểm tra checklist điều kiện chốt sổ và tạo snapshot số liệu bất biến.
          </p>
        </div>
        <Badge variant="provisional">TẠM TÍNH / PROVISIONAL</Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Quy định chốt sổ</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground space-y-2">
          <p>• Xác nhận toàn bộ doanh thu tháng và chi phí lương của tất cả nhân viên.</p>
          <p>• Snapshot lưu vết chính xác mã phiên bản nguồn tại thời điểm chốt.</p>
          <p>• Sau khi chốt sổ, dữ liệu nguồn bị khóa; chỉ điều chỉnh qua quy trình Reopen có phê duyệt.</p>
        </CardContent>
      </Card>
    </div>
  )
}
