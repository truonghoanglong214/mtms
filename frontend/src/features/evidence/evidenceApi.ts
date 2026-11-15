import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import type {
  EvidenceFileDto,
  DownloadTokenResponse,
  VisibilityClass,
} from './types'

export const uploadEvidence = async (
  branchId: string,
  file: File,
  visibilityClass: VisibilityClass = 'BRANCH_PRIVATE',
  onProgress?: (percent: number) => void
): Promise<EvidenceFileDto> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('visibilityClass', visibilityClass)

  const res = await apiClient.post<ApiResponse<EvidenceFileDto>>(
    `/branches/${branchId}/evidence`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      },
    }
  )
  return res.data.data
}

export const listBranchEvidence = async (
  branchId: string
): Promise<EvidenceFileDto[]> => {
  const res = await apiClient.get<ApiResponse<EvidenceFileDto[]>>(
    `/branches/${branchId}/evidence`
  )
  return res.data.data
}

export const getEvidenceDownloadToken = async (
  fileId: string
): Promise<DownloadTokenResponse> => {
  const res = await apiClient.post<ApiResponse<DownloadTokenResponse>>(
    `/evidence/${fileId}/token`
  )
  return res.data.data
}

export const getEvidenceContentUrl = (fileId: string, token: string): string => {
  const rawBaseUrl = apiClient.defaults.baseURL || '/api/v1'
  const baseUrl = rawBaseUrl.endsWith('/') ? rawBaseUrl.slice(0, -1) : rawBaseUrl
  return `${baseUrl}/evidence/${fileId}/content?token=${encodeURIComponent(token)}`
}

export const evidenceApi = {
  uploadEvidence,
  listBranchEvidence,
  getEvidenceDownloadToken,
  getEvidenceContentUrl,
}
