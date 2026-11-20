import { apiClient } from '@/shared/api/apiClient'
import {
  ClosePeriodCommand,
  CloseStatusDto,
  ClosingPrerequisitesDto,
  ManagerialPnLReportDto,
  MonthlyCloseDto,
} from '../types/closing.types'

function extractData<T>(res: { data: unknown }): T {
  if (
    res.data &&
    typeof res.data === 'object' &&
    'data' in res.data &&
    (res.data as { data: unknown }).data !== undefined
  ) {
    return (res.data as { data: T }).data
  }
  return res.data as T
}

export const closingApi = {
  getPrerequisites: async (branchId: string, month: string): Promise<ClosingPrerequisitesDto> => {
    const res = await apiClient.get(`/branches/${branchId}/closing/prerequisites`, {
      params: { month },
    })
    return extractData<ClosingPrerequisitesDto>(res)
  },

  getManagerialPnL: async (branchId: string, month: string): Promise<ManagerialPnLReportDto> => {
    const res = await apiClient.get(`/branches/${branchId}/closing/pnl`, {
      params: { month },
    })
    return extractData<ManagerialPnLReportDto>(res)
  },

  closePeriod: async (branchId: string, data: ClosePeriodCommand): Promise<MonthlyCloseDto> => {
    const res = await apiClient.post(`/branches/${branchId}/closing/close`, data)
    return extractData<MonthlyCloseDto>(res)
  },

  getCloseStatus: async (branchId: string, month: string): Promise<CloseStatusDto> => {
    const res = await apiClient.get(`/branches/${branchId}/closing/status`, {
      params: { month },
    })
    return extractData<CloseStatusDto>(res)
  },

  getCloseHistory: async (branchId: string): Promise<MonthlyCloseDto[]> => {
    const res = await apiClient.get(`/branches/${branchId}/closing/history`)
    return extractData<MonthlyCloseDto[]>(res)
  },
}
