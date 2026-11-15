export type VisibilityClass = 'BRANCH_PRIVATE' | 'ENTITY_PRIVATE' | 'GLOBAL_ADMIN_ONLY'

export interface EvidenceFileDto {
  id: string
  branchId: string
  businessEntityId?: string
  originalFilename: string
  contentType: string
  byteSize: number
  checksumSha256: string
  visibilityClass: VisibilityClass
  uploadedBy: string
  createdAt: string
}

export interface DownloadTokenResponse {
  fileId: string
  token: string
  expiresAt: string
  downloadUrl: string
}

export interface EvidenceUploadResponse {
  file: EvidenceFileDto
  message?: string
}
