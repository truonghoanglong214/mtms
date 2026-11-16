import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import type {
  ImportBatch,
  ImportRow,
  ImportKind,
  ImportRowStatus,
} from './types'

/**
 * Unwrap response whether backend returns ApiResponse<T> wrapper or direct object T
 */
function unwrapResponse<T>(resData: ApiResponse<T> | T | unknown): T {
  if (
    resData &&
    typeof resData === 'object' &&
    'data' in resData &&
    'success' in resData
  ) {
    return (resData as ApiResponse<T>).data
  }
  return resData as T
}

export const stageImportFile = async (
  branchId: string,
  importKind: ImportKind,
  file: File,
  onProgress?: (percent: number) => void
): Promise<ImportBatch> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('importKind', importKind)

  const res = await apiClient.post<ApiResponse<ImportBatch> | ImportBatch>(
    `/branches/${branchId}/imports/stage`,
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
  return unwrapResponse<ImportBatch>(res.data)
}

export const getImportBatch = async (
  branchId: string,
  batchId: string
): Promise<ImportBatch> => {
  const res = await apiClient.get<ApiResponse<ImportBatch> | ImportBatch>(
    `/branches/${branchId}/imports/${batchId}`
  )
  return unwrapResponse<ImportBatch>(res.data)
}

export const getImportRows = async (
  branchId: string,
  batchId: string,
  status?: ImportRowStatus
): Promise<ImportRow[]> => {
  const params = status ? { status } : {}
  const res = await apiClient.get<ApiResponse<ImportRow[]> | ImportRow[]>(
    `/branches/${branchId}/imports/${batchId}/rows`,
    { params }
  )
  return unwrapResponse<ImportRow[]>(res.data)
}

export const commitImportBatch = async (
  branchId: string,
  batchId: string
): Promise<ImportBatch> => {
  const res = await apiClient.post<ApiResponse<ImportBatch> | ImportBatch>(
    `/branches/${branchId}/imports/${batchId}/commit`
  )
  return unwrapResponse<ImportBatch>(res.data)
}

export const importApi = {
  stageImportFile,
  getImportBatch,
  getImportRows,
  commitImportBatch,
}
