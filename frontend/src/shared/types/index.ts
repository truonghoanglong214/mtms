export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string
  timestamp: string
}

export interface BranchContext {
  branchId: string
  branchName: string
  entityId: string
  entityName: string
}
