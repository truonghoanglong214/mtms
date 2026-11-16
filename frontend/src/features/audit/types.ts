export interface AuditEventDto {
  id: string
  branchId?: string
  actorUserId?: string
  actorDisplayName?: string
  correlationId?: string
  targetType: string
  targetId: string
  eventName: string
  actionReason?: string
  oldSnapshot?: Record<string, any> | string
  newSnapshot?: Record<string, any> | string
  occurredAt: string
}

export interface AuditPageResponse {
  content: AuditEventDto[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface AuditFilterParams {
  page?: number
  size?: number
  branchId?: string
  targetType?: string
  eventName?: string
  search?: string
}
