import { useState, useEffect } from 'react'
import { Building2, ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { organizationApi } from '@/features/organization/organizationApi'
import { useAuthStore } from '@/features/auth/authStore'
import { OpeningInventoryTab } from '../components/OpeningInventoryTab'
import type { BusinessEntityDto } from '@/features/organization/types'

export function OpeningInventoryPage() {
  const currentUser = useAuthStore((state) => state.user)

  const [entities, setEntities] = useState<BusinessEntityDto[]>([])
  const [selectedEntityId, setSelectedEntityId] = useState<string>('')
  const [isEntitiesLoading, setIsEntitiesLoading] = useState(true)

  useEffect(() => {
    let isMounted = true
    setIsEntitiesLoading(true)
    organizationApi
      .getEntities()
      .then((data) => {
        if (!isMounted) return
        setEntities(data)
        if (data.length > 0) {
          const userEntityId = currentUser?.grants?.find((g) => g.businessEntityId)?.businessEntityId
          const matched = data.find((e) => e.id === userEntityId)
          setSelectedEntityId(matched ? matched.id : data[0].id)
        }
      })
      .catch((err) => {
        console.error('Failed to load business entities:', err)
      })
      .finally(() => {
        if (isMounted) setIsEntitiesLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [currentUser])

  return (
    <div className="space-y-6">
      {/* Top Header & Entity Selector */}
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <div className="flex items-center gap-2.5">
            <Link to="/inventory">
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0 rounded-lg text-slate-500 hover:text-slate-800">
                <ArrowLeft className="h-4 w-4" />
              </Button>
            </Link>
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Khởi Tạo Tồn Kho Đầu Kỳ & Giá Vốn WAC
            </h1>
            <Badge variant="secondary" className="font-mono text-xs">
              P0-INV-02 / BR-INV-08
            </Badge>
            <Badge variant="outline" className="border-emerald-600 text-emerald-800 bg-emerald-50 text-xs font-mono">
              TC-024
            </Badge>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Ghi nhận kiểm kê thực tế mở sổ, thiết lập giá vốn bình quân gia quyền di động (Moving WAC) và khóa sổ chi nhánh.
          </p>
        </div>

        {/* Entity Switcher */}
        <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-slate-200 shadow-xs shrink-0">
          <Building2 className="h-4 w-4 text-emerald-800" />
          <span className="text-xs font-medium text-slate-500">Pháp nhân:</span>
          {isEntitiesLoading ? (
            <span className="text-xs text-slate-400">Đang tải pháp nhân...</span>
          ) : entities.length === 0 ? (
            <span className="text-xs text-red-500">Chưa có pháp nhân</span>
          ) : (
            <select
              value={selectedEntityId}
              onChange={(e) => setSelectedEntityId(e.target.value)}
              className="text-xs font-semibold text-slate-800 bg-transparent focus:outline-none cursor-pointer"
            >
              {entities.map((e) => (
                <option key={e.id} value={e.id}>
                  [{e.entityCode}] {e.registeredName}
                </option>
              ))}
            </select>
          )}
        </div>
      </div>

      {/* Main Tab Component */}
      <OpeningInventoryTab entityId={selectedEntityId} />
    </div>
  )
}
export default OpeningInventoryPage
