import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { ShieldCheck, Database, Layers, CheckCircle } from 'lucide-react'

export function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Hệ thống MTMS</h1>
        <p className="text-muted-foreground mt-1">
          Bảng điều khiển vận hành và quản trị tài chính chuỗi trà sữa.
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Kiến trúc Bounded Context</CardTitle>
            <Layers className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">11 Modules</div>
            <p className="text-xs text-muted-foreground mt-1">
              Phân định ranh giới nghiêm ngặt bằng ArchUnit
            </p>
            <div className="mt-3">
              <Badge variant="default" className="text-xs">Modular Monolith</Badge>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Bảo vệ Tài chính</CardTitle>
            <ShieldCheck className="h-4 w-4 text-emerald-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">Strict Safety</div>
            <p className="text-xs text-muted-foreground mt-1">
              NO POS · Tách biệt doanh thu NET và dòng tiền
            </p>
            <div className="mt-3">
              <Badge variant="provisional" className="text-xs">TẠM TÍNH</Badge>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Trạng thái Cơ sở dữ liệu</CardTitle>
            <Database className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">PostgreSQL 16</div>
            <p className="text-xs text-muted-foreground mt-1">
              Flyway migration chuẩn bị cho Task P0-ORG-02
            </p>
            <div className="mt-3">
              <Badge variant="secondary" className="text-xs">Clean Schema</Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Trạng thái Skeleton Task P0-ORG-01</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center space-x-2 text-sm">
            <CheckCircle className="h-4 w-4 text-emerald-600" />
            <span>Backend: Java 21 LTS + Spring Boot 3.3.5 Modular Monolith skeleton (6/6 tests PASS)</span>
          </div>
          <div className="flex items-center space-x-2 text-sm">
            <CheckCircle className="h-4 w-4 text-emerald-600" />
            <span>Frontend: React 18 + TypeScript + Vite + Tailwind CSS + shadcn/ui components</span>
          </div>
          <div className="flex items-center space-x-2 text-sm">
            <CheckCircle className="h-4 w-4 text-emerald-600" />
            <span>Docker: Multi-stage Dockerfiles & Docker Compose baseline sẵn sàng triển khai</span>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
