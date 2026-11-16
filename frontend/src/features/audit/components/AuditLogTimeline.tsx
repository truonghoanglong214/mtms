import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  History,
  Search,
  Filter,
  RefreshCw,
  ChevronDown,
  ChevronUp,
  ShieldCheck,
  ShieldAlert,
  UserCheck,
  Users,
  Building2,
  Store,
  CreditCard,
  FileText,
  AlertCircle,
  Loader2,
  Copy,
  Check,
  MessageSquare,
  GitCommit,
  Eye,
  Code2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { auditApi } from '../auditApi'
import type { AuditPageResponse } from '../types'

export interface AuditLogTimelineProps {
  /** Chi nhánh mặc định cần xem kiểm toán (nếu có) */
  initialBranchId?: string
  /** Danh sách chi nhánh hiện có để hỗ trợ bộ lọc nhanh */
  branches?: Array<{ id: string; name: string; branchCode?: string }>
  /** Phân quyền: Người dùng có phải Quản trị viên hệ thống (System Admin) không */
  isSystemAdmin?: boolean
  className?: string
}

// Cấu hình nhãn, màu sắc và icon cho từng loại sự kiện kiểm toán
interface EventVisualConfig {
  label: string
  colorBadgeClass: string
  icon: React.ComponentType<{ className?: string }>
}

function getEventVisualConfig(eventName: string): EventVisualConfig {
  switch (eventName) {
    case 'BRANCH_ACTIVATED':
      return {
        label: 'Kích hoạt Chi nhánh',
        colorBadgeClass: 'border-emerald-500/40 text-emerald-300 bg-emerald-950/50',
        icon: Building2,
      }
    case 'ROLE_GRANTED':
      return {
        label: 'Cấp quyền truy cập',
        colorBadgeClass: 'border-purple-500/40 text-purple-300 bg-purple-950/50',
        icon: ShieldCheck,
      }
    case 'ROLE_REVOKED':
      return {
        label: 'Thu hồi quyền hạn',
        colorBadgeClass: 'border-rose-500/40 text-rose-300 bg-rose-950/50',
        icon: ShieldAlert,
      }
    case 'USER_STATUS_CHANGED':
      return {
        label: 'Đổi trạng thái tài khoản',
        colorBadgeClass: 'border-sky-500/40 text-sky-300 bg-sky-950/50',
        icon: UserCheck,
      }
    case 'BANK_ACCOUNT_ADDED':
      return {
        label: 'Thêm tài khoản NH',
        colorBadgeClass: 'border-teal-500/40 text-teal-300 bg-teal-950/50',
        icon: CreditCard,
      }
    case 'BANK_ACCOUNT_PRIMARY_SET':
      return {
        label: 'Đặt TK ngân hàng chính',
        colorBadgeClass: 'border-teal-500/40 text-teal-300 bg-teal-950/50',
        icon: CreditCard,
      }
    case 'BRANCH_CREATED':
      return {
        label: 'Tạo Chi nhánh mới',
        colorBadgeClass: 'border-emerald-600/40 text-emerald-400 bg-emerald-950/40',
        icon: Store,
      }
    case 'BUSINESS_ENTITY_CREATED':
      return {
        label: 'Tạo Pháp nhân',
        colorBadgeClass: 'border-indigo-500/40 text-indigo-300 bg-indigo-950/50',
        icon: Building2,
      }
    case 'USER_CREATED':
      return {
        label: 'Tạo Người dùng',
        colorBadgeClass: 'border-blue-500/40 text-blue-300 bg-blue-950/50',
        icon: Users,
      }
    case 'EVIDENCE_UPLOADED':
      return {
        label: 'Tải lên chứng từ',
        colorBadgeClass: 'border-amber-500/40 text-amber-300 bg-amber-950/50',
        icon: FileText,
      }
    default:
      return {
        label: eventName,
        colorBadgeClass: 'border-stone-700 text-stone-300 bg-stone-900/60',
        icon: History,
      }
  }
}

/**
 * Định dạng thời gian chi tiết theo chuẩn tiếng Việt: dd/MM/yyyy HH:mm:ss
 */
export function formatAuditDateTime(dateString: string): string {
  if (!dateString) return ''
  try {
    const d = new Date(dateString)
    if (isNaN(d.getTime())) return dateString
    const pad = (n: number) => n.toString().padStart(2, '0')
    const day = pad(d.getDate())
    const month = pad(d.getMonth() + 1)
    const year = d.getFullYear()
    const hours = pad(d.getHours())
    const minutes = pad(d.getMinutes())
    const seconds = pad(d.getSeconds())
    return `${day}/${month}/${year} ${hours}:${minutes}:${seconds}`
  } catch {
    return dateString
  }
}

/**
 * Phân tích dữ liệu Snapshot an toàn (hỗ trợ cả JSON string và Object)
 */
function parseSnapshot(snapshot?: Record<string, any> | string | null): Record<string, any> | null {
  if (!snapshot) return null
  if (typeof snapshot === 'object') return snapshot
  try {
    const parsed = JSON.parse(snapshot)
    if (typeof parsed === 'object' && parsed !== null) return parsed
    return { value: parsed }
  } catch {
    return { raw: snapshot }
  }
}

export const AuditLogTimeline: React.FC<AuditLogTimelineProps> = ({
  initialBranchId,
  branches = [],
  isSystemAdmin = true,
  className = '',
}) => {
  // Trạng thái chọn chi nhánh: 'ALL' hoặc UUID của chi nhánh
  const [selectedBranchId, setSelectedBranchId] = useState<string>(
    initialBranchId || (isSystemAdmin ? 'ALL' : branches[0]?.id || 'ALL')
  )

  // Trạng thái dữ liệu phân trang
  const [page, setPage] = useState<number>(0)
  const pageSize = 20
  const [auditData, setAuditData] = useState<AuditPageResponse | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  // Bộ lọc tìm kiếm & loại sự kiện
  const [searchQuery, setSearchQuery] = useState<string>('')
  const [selectedEventType, setSelectedEventType] = useState<string>('ALL')

  // Mở rộng/thu gọn Snapshot Before/After cho từng dòng
  const [expandedEvents, setExpandedEvents] = useState<Record<string, boolean>>({})
  // Chế độ xem Snapshot: 'diff' (so sánh từng trường) hoặc 'raw' (JSON code block)
  const [snapshotViewModes, setSnapshotViewModes] = useState<Record<string, 'diff' | 'raw'>>({})
  // Trạng thái nút copy clipboard
  const [copiedId, setCopiedId] = useState<string | null>(null)

  // Map tên chi nhánh để hiển thị nhãn thân thiện
  const branchMap = useMemo(() => {
    const map = new Map<string, string>()
    branches.forEach((b) => {
      map.set(b.id, b.branchCode ? `[${b.branchCode}] ${b.name}` : b.name)
    })
    return map
  }, [branches])

  // Hàm tải dữ liệu Audit Events
  const fetchAuditEvents = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      let response: AuditPageResponse
      if (selectedBranchId === 'ALL') {
        response = await auditApi.getGlobalAuditEvents(page, pageSize)
      } else {
        response = await auditApi.getBranchAuditEvents(selectedBranchId, page, pageSize)
      }
      setAuditData(response)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } }; message?: string }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          apiErr.message ||
          'Không thể tải danh sách nhật ký kiểm toán. Vui lòng kiểm tra quyền truy cập.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [selectedBranchId, page])

  useEffect(() => {
    fetchAuditEvents()
  }, [fetchAuditEvents])

  // Đồng bộ lại khi initialBranchId thay đổi từ ngoài
  useEffect(() => {
    if (initialBranchId) {
      setSelectedBranchId(initialBranchId)
      setPage(0)
    }
  }, [initialBranchId])

  // Đổi chi nhánh
  const handleBranchChange = (branchId: string) => {
    setSelectedBranchId(branchId)
    setPage(0)
  }

  // Bật/tắt mở rộng snapshot của sự kiện
  const toggleExpand = (eventId: string) => {
    setExpandedEvents((prev) => ({
      ...prev,
      [eventId]: !prev[eventId],
    }))
  }

  // Chuyển đổi giữa chế độ xem trực quan Diff và Raw JSON
  const toggleSnapshotMode = (eventId: string) => {
    setSnapshotViewModes((prev) => ({
      ...prev,
      [eventId]: prev[eventId] === 'raw' ? 'diff' : 'raw',
    }))
  }

  // Sao chép nội dung vào Clipboard
  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text)
    setCopiedId(id)
    setTimeout(() => {
      setCopiedId((curr) => (curr === id ? null : curr))
    }, 2000)
  }

  // Lọc sự kiện hiển thị theo từ khóa tìm kiếm và loại sự kiện
  const filteredEvents = useMemo(() => {
    if (!auditData?.content) return []
    return auditData.content.filter((event) => {
      // Lọc theo loại sự kiện
      if (selectedEventType !== 'ALL' && event.eventName !== selectedEventType) {
        return false
      }

      // Lọc theo từ khóa tìm kiếm
      if (searchQuery.trim() !== '') {
        const q = searchQuery.toLowerCase()
        const matchesEventName = event.eventName.toLowerCase().includes(q)
        const matchesTargetType = event.targetType.toLowerCase().includes(q)
        const matchesTargetId = event.targetId.toLowerCase().includes(q)
        const matchesActor = event.actorDisplayName?.toLowerCase().includes(q) || false
        const matchesActorId = event.actorUserId?.toLowerCase().includes(q) || false
        const matchesReason = event.actionReason?.toLowerCase().includes(q) || false
        const matchesCorrelation = event.correlationId?.toLowerCase().includes(q) || false

        if (
          !matchesEventName &&
          !matchesTargetType &&
          !matchesTargetId &&
          !matchesActor &&
          !matchesActorId &&
          !matchesReason &&
          !matchesCorrelation
        ) {
          return false
        }
      }

      return true
    })
  }, [auditData, searchQuery, selectedEventType])

  // Danh sách các loại sự kiện có trong dữ liệu hiện tại để đưa vào filter
  const eventTypesInList = useMemo(() => {
    const types = new Set<string>()
    auditData?.content?.forEach((item) => {
      if (item.eventName) types.add(item.eventName)
    })
    return Array.from(types)
  }, [auditData])

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Thanh điều khiển trên cùng: Tìm kiếm, Bộ lọc chi nhánh, Loại sự kiện, Nút làm mới */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3 rounded-xl border border-stone-800 bg-stone-900/60 p-3.5 backdrop-blur-sm">
        <div className="flex flex-1 flex-col sm:flex-row items-stretch sm:items-center gap-2.5">
          {/* Ô tìm kiếm nhanh */}
          <div className="relative flex-1 min-w-[220px]">
            <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-stone-500" />
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo sự kiện, đối tượng, người thực hiện, lý do..."
              className="pl-9 bg-stone-950 border-stone-800 text-xs text-stone-100 placeholder:text-stone-600 focus:border-emerald-500 h-9"
            />
          </div>

          {/* Lọc theo Chi nhánh */}
          <div className="flex items-center space-x-2">
            <span className="text-[11px] font-mono text-stone-400 shrink-0 flex items-center">
              <Store className="h-3 w-3 mr-1 text-emerald-400" />
              Chi nhánh:
            </span>
            <select
              value={selectedBranchId}
              onChange={(e) => handleBranchChange(e.target.value)}
              className="h-9 rounded-md border border-stone-800 bg-stone-950 px-2.5 text-xs text-stone-200 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            >
              {isSystemAdmin && <option value="ALL">🌐 Toàn hệ thống (Global Log)</option>}
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.branchCode ? `[${b.branchCode}] ${b.name}` : b.name}
                </option>
              ))}
            </select>
          </div>

          {/* Lọc theo Loại sự kiện */}
          <div className="flex items-center space-x-2">
            <span className="text-[11px] font-mono text-stone-400 shrink-0 flex items-center">
              <Filter className="h-3 w-3 mr-1 text-purple-400" />
              Sự kiện:
            </span>
            <select
              value={selectedEventType}
              onChange={(e) => setSelectedEventType(e.target.value)}
              className="h-9 rounded-md border border-stone-800 bg-stone-950 px-2.5 text-xs text-stone-200 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            >
              <option value="ALL">Tất cả sự kiện</option>
              <option value="BRANCH_ACTIVATED">BRANCH_ACTIVATED</option>
              <option value="ROLE_GRANTED">ROLE_GRANTED</option>
              <option value="ROLE_REVOKED">ROLE_REVOKED</option>
              <option value="USER_STATUS_CHANGED">USER_STATUS_CHANGED</option>
              <option value="BANK_ACCOUNT_ADDED">BANK_ACCOUNT_ADDED</option>
              <option value="BANK_ACCOUNT_PRIMARY_SET">BANK_ACCOUNT_PRIMARY_SET</option>
              <option value="BRANCH_CREATED">BRANCH_CREATED</option>
              <option value="BUSINESS_ENTITY_CREATED">BUSINESS_ENTITY_CREATED</option>
              <option value="USER_CREATED">USER_CREATED</option>
              <option value="EVIDENCE_UPLOADED">EVIDENCE_UPLOADED</option>
              {eventTypesInList
                .filter(
                  (t) =>
                    ![
                      'BRANCH_ACTIVATED',
                      'ROLE_GRANTED',
                      'ROLE_REVOKED',
                      'USER_STATUS_CHANGED',
                      'BANK_ACCOUNT_ADDED',
                      'BANK_ACCOUNT_PRIMARY_SET',
                      'BRANCH_CREATED',
                      'BUSINESS_ENTITY_CREATED',
                      'USER_CREATED',
                      'EVIDENCE_UPLOADED',
                    ].includes(t)
                )
                .map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
            </select>
          </div>
        </div>

        {/* Nút làm mới */}
        <div className="flex items-center space-x-2 self-end lg:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchAuditEvents}
            disabled={isLoading}
            className="h-9 border-stone-800 text-stone-300 hover:bg-stone-800 text-xs px-3"
          >
            <RefreshCw className={`h-3.5 w-3.5 mr-1.5 ${isLoading ? 'animate-spin text-emerald-400' : ''}`} />
            Làm mới
          </Button>
        </div>
      </div>

      {/* Thông báo lỗi nếu có */}
      {error && (
        <div className="flex items-center justify-between rounded-lg border border-red-500/30 bg-red-950/40 p-3.5 text-xs text-red-300">
          <div className="flex items-center space-x-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-red-400" />
            <span>{error}</span>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={fetchAuditEvents}
            className="h-7 text-xs text-red-300 hover:text-red-100 hover:bg-red-900/40"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* Danh sách Dòng thời gian Kiểm toán (Timeline View) */}
      <div className="rounded-xl border border-stone-800 bg-stone-900/40 overflow-hidden shadow-xl">
        {/* Header thông tin tổng quan */}
        <div className="flex flex-wrap items-center justify-between border-b border-stone-800 bg-stone-950/70 px-4 py-3 text-xs">
          <div className="flex items-center space-x-2">
            <span className="flex h-5 w-5 items-center justify-center rounded-md bg-stone-800 text-stone-300">
              <History className="h-3 w-3 text-emerald-400" />
            </span>
            <span className="font-semibold text-stone-200">Dòng thời gian sự kiện kiểm toán</span>
            <span className="rounded-full bg-stone-800 px-2 py-0.5 text-[10px] text-stone-400 font-mono">
              {filteredEvents.length} / {auditData?.totalElements || 0} bản ghi
            </span>
          </div>

          <div className="text-[11px] font-mono text-stone-400 flex items-center space-x-3">
            <span>
              Phạm vi:{' '}
              <strong className="text-emerald-400">
                {selectedBranchId === 'ALL'
                  ? 'Toàn hệ thống (Global)'
                  : branchMap.get(selectedBranchId) || selectedBranchId}
              </strong>
            </span>
          </div>
        </div>

        {/* Nội dung danh sách */}
        {isLoading && !auditData ? (
          <div className="p-12 flex flex-col items-center justify-center space-y-3 text-center">
            <Loader2 className="h-7 w-7 animate-spin text-emerald-400" />
            <p className="text-xs text-stone-400 font-mono">Đang tải nhật ký kiểm toán bất biến...</p>
          </div>
        ) : filteredEvents.length === 0 ? (
          <div className="p-12 flex flex-col items-center justify-center space-y-3 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-stone-800/80 border border-stone-700 text-stone-400">
              <ShieldAlert className="h-6 w-6 text-stone-500" />
            </div>
            <div className="space-y-1">
              <h4 className="text-sm font-semibold text-stone-200">Không tìm thấy sự kiện kiểm toán nào</h4>
              <p className="text-xs text-stone-400 max-w-sm">
                {searchQuery || selectedEventType !== 'ALL'
                  ? 'Không có sự kiện nào khớp với tiêu chí tìm kiếm hoặc bộ lọc hiện tại.'
                  : 'Hệ thống chưa ghi nhận sự kiện nào trong phạm vi chi nhánh này.'}
              </p>
            </div>
          </div>
        ) : (
          <div className="divide-y divide-stone-800/60">
            {filteredEvents.map((event, idx) => {
              const visual = getEventVisualConfig(event.eventName)
              const VisualIcon = visual.icon
              const isExpanded = !!expandedEvents[event.id]
              const viewMode = snapshotViewModes[event.id] || 'diff'
              const oldSnap = parseSnapshot(event.oldSnapshot)
              const newSnap = parseSnapshot(event.newSnapshot)
              const hasSnapshots = !!(oldSnap || newSnap)
              const branchLabel = event.branchId ? branchMap.get(event.branchId) || event.branchId : null

              return (
                <motion.div
                  key={event.id}
                  initial={{ opacity: 0, y: 4 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.15, delay: Math.min(idx * 0.02, 0.2) }}
                  className="p-4 hover:bg-stone-800/25 transition-colors"
                >
                  {/* Dòng chính: Badge sự kiện + Thông tin tác nhân + Thời gian */}
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5">
                    <div className="flex flex-wrap items-center gap-2">
                      {/* Badge Sự kiện */}
                      <Badge
                        variant="outline"
                        className={`inline-flex items-center space-x-1 px-2.5 py-1 text-xs font-semibold ${visual.colorBadgeClass}`}
                      >
                        <VisualIcon className="h-3.5 w-3.5 mr-1" />
                        <span>{event.eventName}</span>
                      </Badge>

                      {/* Tên thân thiện */}
                      <span className="text-xs font-medium text-stone-300">
                        {visual.label}
                      </span>

                      {/* Chi nhánh */}
                      {branchLabel ? (
                        <span className="inline-flex items-center rounded bg-stone-800/70 px-2 py-0.5 text-[11px] font-mono text-emerald-400 border border-stone-700/60">
                          <Store className="h-3 w-3 mr-1 text-emerald-500" />
                          {branchLabel}
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded bg-stone-800/40 px-2 py-0.5 text-[11px] font-mono text-stone-400 border border-stone-800">
                          Toàn hệ thống (Global)
                        </span>
                      )}
                    </div>

                    {/* Thời gian chi tiết tiếng Việt */}
                    <div className="flex items-center space-x-2 text-[11px] font-mono text-stone-400 self-start sm:self-auto">
                      <span>{formatAuditDateTime(event.occurredAt)}</span>
                    </div>
                  </div>

                  {/* Chi tiết phụ: Actor, Target, Correlation ID */}
                  <div className="mt-2.5 flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-stone-400">
                    {/* Người thực hiện (Actor) */}
                    <div className="flex items-center space-x-1.5">
                      <span className="text-stone-500">Người thực hiện:</span>
                      <span className="font-semibold text-stone-200 inline-flex items-center">
                        <Users className="h-3 w-3 mr-1 text-stone-400" />
                        {event.actorDisplayName || event.actorUserId || 'Hệ thống'}
                      </span>
                    </div>

                    {/* Đối tượng tác động (Target) */}
                    <div className="flex items-center space-x-1.5">
                      <span className="text-stone-500">Đối tượng:</span>
                      <span className="rounded bg-stone-950 px-1.5 py-0.5 font-mono text-[11px] text-stone-300 border border-stone-800">
                        {event.targetType}: {event.targetId.substring(0, 8)}...
                      </span>
                    </div>

                    {/* Mã tương quan (Correlation ID) nếu có */}
                    {event.correlationId && (
                      <div className="flex items-center space-x-1 text-[11px] text-stone-500 font-mono">
                        <GitCommit className="h-3 w-3 text-stone-600" />
                        <span>corr: {event.correlationId.substring(0, 8)}</span>
                      </div>
                    )}
                  </div>

                  {/* Lý do hành động (Action Reason) nếu có */}
                  {event.actionReason && (
                    <div className="mt-2.5 flex items-start space-x-2 rounded-lg border border-amber-500/20 bg-amber-950/20 px-3 py-2 text-xs text-amber-200">
                      <MessageSquare className="h-3.5 w-3.5 mt-0.5 shrink-0 text-amber-400" />
                      <div>
                        <span className="font-semibold text-amber-300 mr-1.5">Lý do thay đổi:</span>
                        <span className="italic">{event.actionReason}</span>
                      </div>
                    </div>
                  )}

                  {/* Nút xem Chi tiết Before/After Snapshot */}
                  {hasSnapshots && (
                    <div className="mt-3 flex items-center justify-between border-t border-stone-800/40 pt-2.5">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => toggleExpand(event.id)}
                        className="h-7 px-2.5 text-xs text-stone-300 hover:text-emerald-400 hover:bg-stone-800/60 inline-flex items-center space-x-1.5"
                      >
                        {isExpanded ? (
                          <>
                            <ChevronUp className="h-3.5 w-3.5 text-emerald-400" />
                            <span>Thu gọn Before/After Snapshot</span>
                          </>
                        ) : (
                          <>
                            <ChevronDown className="h-3.5 w-3.5 text-stone-400" />
                            <span>Xem chi tiết Before/After Snapshot</span>
                          </>
                        )}
                      </Button>

                      {isExpanded && (
                        <div className="flex items-center space-x-2">
                          {/* Chuyển đổi View Mode */}
                          <button
                            type="button"
                            onClick={() => toggleSnapshotMode(event.id)}
                            className="inline-flex items-center space-x-1 rounded px-2 py-0.5 text-[11px] font-mono text-stone-400 hover:text-stone-200 hover:bg-stone-800 border border-stone-800 transition-colors"
                            title="Chuyển chế độ so sánh"
                          >
                            {viewMode === 'diff' ? (
                              <>
                                <Code2 className="h-3 w-3 text-emerald-400" />
                                <span>Xem JSON Raw</span>
                              </>
                            ) : (
                              <>
                                <Eye className="h-3 w-3 text-purple-400" />
                                <span>Xem Diff dạng cột</span>
                              </>
                            )}
                          </button>

                          {/* Copy JSON */}
                          <button
                            type="button"
                            onClick={() =>
                              handleCopy(
                                JSON.stringify(
                                  {
                                    eventId: event.id,
                                    eventName: event.eventName,
                                    oldSnapshot: oldSnap,
                                    newSnapshot: newSnap,
                                  },
                                  null,
                                  2
                                ),
                                event.id
                              )
                            }
                            className="inline-flex items-center space-x-1 rounded px-2 py-0.5 text-[11px] font-mono text-stone-400 hover:text-stone-200 hover:bg-stone-800 border border-stone-800 transition-colors"
                          >
                            {copiedId === event.id ? (
                              <>
                                <Check className="h-3 w-3 text-emerald-400" />
                                <span className="text-emerald-400">Đã chép</span>
                              </>
                            ) : (
                              <>
                                <Copy className="h-3 w-3" />
                                <span>Copy JSON</span>
                              </>
                            )}
                          </button>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Vùng hiển thị mở rộng Snapshot Trước & Sau */}
                  <AnimatePresence>
                    {isExpanded && hasSnapshots && (
                      <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.2 }}
                        className="mt-3 overflow-hidden rounded-lg border border-stone-800 bg-stone-950 p-3"
                      >
                        {viewMode === 'diff' ? (
                          <SnapshotDiffView oldSnapshot={oldSnap} newSnapshot={newSnap} />
                        ) : (
                          <SnapshotRawJsonView oldSnapshot={oldSnap} newSnapshot={newSnap} />
                        )}
                      </motion.div>
                    )}
                  </AnimatePresence>
                </motion.div>
              )
            })}
          </div>
        )}

        {/* Thanh phân trang ở đáy bảng */}
        {auditData && auditData.totalPages > 1 && (
          <div className="flex flex-col sm:flex-row items-center justify-between border-t border-stone-800 bg-stone-950/80 px-4 py-3 text-xs gap-3">
            <div className="text-stone-400 font-mono text-[11px]">
              Trang <span className="text-emerald-400 font-bold">{auditData.number + 1}</span> /{' '}
              {auditData.totalPages} (Tổng cộng{' '}
              <span className="text-stone-200 font-semibold">{auditData.totalElements}</span> sự kiện)
            </div>

            <div className="flex items-center space-x-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(p - 1, 0))}
                disabled={auditData.number === 0 || isLoading}
                className="h-8 border-stone-800 text-stone-300 hover:bg-stone-800 text-xs px-3"
              >
                Trước
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.min(p + 1, auditData.totalPages - 1))}
                disabled={auditData.number >= auditData.totalPages - 1 || isLoading}
                className="h-8 border-stone-800 text-stone-300 hover:bg-stone-800 text-xs px-3"
              >
                Sau
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

/**
 * Component hiển thị so sánh 2 cột Before / After với highlight thay đổi
 */
interface SnapshotDiffViewProps {
  oldSnapshot: Record<string, any> | null
  newSnapshot: Record<string, any> | null
}

const SnapshotDiffView: React.FC<SnapshotDiffViewProps> = ({ oldSnapshot, newSnapshot }) => {
  // Tập hợp tất cả các keys xuất hiện ở 2 snapshot
  const allKeys = useMemo(() => {
    const keys = new Set<string>()
    if (oldSnapshot && typeof oldSnapshot === 'object') {
      Object.keys(oldSnapshot).forEach((k) => keys.add(k))
    }
    if (newSnapshot && typeof newSnapshot === 'object') {
      Object.keys(newSnapshot).forEach((k) => keys.add(k))
    }
    return Array.from(keys).sort()
  }, [oldSnapshot, newSnapshot])

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
      {/* Cột trái: Trạng thái trước (Before Snapshot) */}
      <div className="rounded-md border border-rose-950/60 bg-rose-950/15 p-3">
        <div className="mb-2 flex items-center justify-between border-b border-rose-900/40 pb-1.5 text-rose-300">
          <span className="font-semibold flex items-center">
            <span className="mr-1.5 inline-block h-2 w-2 rounded-full bg-rose-500" />
            Trạng thái trước (Before Snapshot)
          </span>
          <span className="text-[10px] text-rose-400/80">
            {oldSnapshot ? `${Object.keys(oldSnapshot).length} trường` : 'Chưa có'}
          </span>
        </div>

        {!oldSnapshot || Object.keys(oldSnapshot).length === 0 ? (
          <div className="py-4 text-center text-stone-500 italic text-[11px]">
            (Không có dữ liệu trước thay đổi - Khởi tạo mới)
          </div>
        ) : (
          <div className="space-y-1.5">
            {allKeys.map((key) => {
              const hasOld = oldSnapshot && key in oldSnapshot
              const hasNew = newSnapshot && key in newSnapshot
              const oldVal = hasOld ? oldSnapshot[key] : undefined
              const newVal = hasNew ? newSnapshot[key] : undefined
              const isModified =
                hasOld && hasNew && JSON.stringify(oldVal) !== JSON.stringify(newVal)
              const isRemoved = hasOld && !hasNew

              if (!hasOld) return null

              return (
                <div
                  key={key}
                  className={`flex flex-col sm:flex-row sm:items-baseline justify-between rounded px-2 py-1 text-[11px] ${
                    isRemoved
                      ? 'bg-rose-950/40 border border-rose-800/40 text-rose-200'
                      : isModified
                      ? 'bg-amber-950/30 border border-amber-800/30 text-amber-200'
                      : 'text-stone-300'
                  }`}
                >
                  <span className="font-semibold text-stone-400">{key}:</span>
                  <span className="font-mono break-all text-right">
                    {formatSnapshotValue(oldVal)}
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Cột phải: Trạng thái sau (After Snapshot) */}
      <div className="rounded-md border border-emerald-950/60 bg-emerald-950/15 p-3">
        <div className="mb-2 flex items-center justify-between border-b border-emerald-900/40 pb-1.5 text-emerald-300">
          <span className="font-semibold flex items-center">
            <span className="mr-1.5 inline-block h-2 w-2 rounded-full bg-emerald-500" />
            Trạng thái sau (After Snapshot)
          </span>
          <span className="text-[10px] text-emerald-400/80">
            {newSnapshot ? `${Object.keys(newSnapshot).length} trường` : 'Đã xoá'}
          </span>
        </div>

        {!newSnapshot || Object.keys(newSnapshot).length === 0 ? (
          <div className="py-4 text-center text-stone-500 italic text-[11px]">
            (Không có dữ liệu sau thay đổi - Đã xoá hoặc thu hồi)
          </div>
        ) : (
          <div className="space-y-1.5">
            {allKeys.map((key) => {
              const hasOld = oldSnapshot && key in oldSnapshot
              const hasNew = newSnapshot && key in newSnapshot
              const oldVal = hasOld ? oldSnapshot[key] : undefined
              const newVal = hasNew ? newSnapshot[key] : undefined
              const isModified =
                hasOld && hasNew && JSON.stringify(oldVal) !== JSON.stringify(newVal)
              const isAdded = !hasOld && hasNew

              if (!hasNew) return null

              return (
                <div
                  key={key}
                  className={`flex flex-col sm:flex-row sm:items-baseline justify-between rounded px-2 py-1 text-[11px] ${
                    isAdded
                      ? 'bg-emerald-950/40 border border-emerald-800/40 text-emerald-200'
                      : isModified
                      ? 'bg-amber-950/30 border border-amber-800/30 text-amber-200'
                      : 'text-stone-300'
                  }`}
                >
                  <span className="font-semibold text-stone-400">{key}:</span>
                  <span className="font-mono break-all text-right">
                    {formatSnapshotValue(newVal)}
                  </span>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

/**
 * Hiển thị JSON Raw có định dạng
 */
const SnapshotRawJsonView: React.FC<SnapshotDiffViewProps> = ({ oldSnapshot, newSnapshot }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs font-mono">
      <div>
        <div className="mb-1 text-[11px] font-semibold text-rose-400">BEFORE (JSON):</div>
        <pre className="max-h-60 overflow-y-auto rounded bg-stone-900/90 p-2 text-[11px] text-stone-300 border border-stone-800">
          {oldSnapshot ? JSON.stringify(oldSnapshot, null, 2) : 'null'}
        </pre>
      </div>
      <div>
        <div className="mb-1 text-[11px] font-semibold text-emerald-400">AFTER (JSON):</div>
        <pre className="max-h-60 overflow-y-auto rounded bg-stone-900/90 p-2 text-[11px] text-stone-300 border border-stone-800">
          {newSnapshot ? JSON.stringify(newSnapshot, null, 2) : 'null'}
        </pre>
      </div>
    </div>
  )
}

function formatSnapshotValue(val: any): string {
  if (val === null) return 'null'
  if (val === undefined) return 'undefined'
  if (typeof val === 'boolean') return val ? 'true' : 'false'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}
