import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'

export function InventoryPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Quản lý Kho & Giá vốn WAC</h1>
          <p className="text-muted-foreground">
            Danh mục nguyên vật liệu, phiếu nhập xuất kho thủ công và kiểm kê định kỳ.
          </p>
        </div>
        <Badge variant="secondary">BR-INV-01 / ADR-008</Badge>
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
