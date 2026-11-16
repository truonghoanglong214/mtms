import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/features/auth/authStore'
import type { ApiResponse } from '@/shared/types'
import type { TokenRefreshResponse, ApiProblemDetail } from '@/features/auth/types'
import {
  generateIdempotencyKey,
  isMutationMethod,
  IDEMPOTENCY_HEADER,
  IDEMPOTENT_REPLAYED_HEADER,
  ERROR_TYPE_VERSION_CONFLICT,
  ERROR_TYPE_IDEMPOTENCY_IN_PROGRESS,
  ERROR_TYPE_IDEMPOTENCY_PAYLOAD_MISMATCH,
} from '@/shared/utils/idempotency'
import { useConcurrencyNotificationStore } from '@/shared/stores/concurrencyNotificationStore'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

let isRefreshing = false
let failedQueue: Array<{
  resolve: (value?: unknown) => void
  reject: (reason?: unknown) => void
}> = []

const processQueue = (error: Error | null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve()
    }
  })
  failedQueue = []
}

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // Auto-attach Idempotency-Key for mutation requests (POST, PUT, PATCH, DELETE)
    if (isMutationMethod(config.method) && config.headers) {
      const hasIdempotencyKey =
        Boolean(config.headers[IDEMPOTENCY_HEADER]) ||
        Boolean(config.headers[IDEMPOTENCY_HEADER.toLowerCase()]) ||
        (typeof config.headers.get === 'function' && Boolean(config.headers.get(IDEMPOTENCY_HEADER)))

      if (!hasIdempotencyKey) {
        config.headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
      }
    }

    return config
  },
  (error) => Promise.reject(error)
)

apiClient.interceptors.response.use(
  (response) => {
    // Check if request was replayed from idempotent cache
    const replayedHeader =
      response.headers?.[IDEMPOTENT_REPLAYED_HEADER] ??
      (typeof response.headers?.get === 'function' ? response.headers.get(IDEMPOTENT_REPLAYED_HEADER) : undefined)

    const isReplayed = replayedHeader === 'true' || replayedHeader === true

    if (isReplayed) {
      console.debug('[Idempotency] Request replayed from idempotent cache:', {
        url: response.config.url,
        method: response.config.method,
      })
      useConcurrencyNotificationStore.getState().showNotification({
        type: 'replayed',
        title: 'Thông báo giao dịch',
        message: 'Giao dịch đã được ghi nhận trước đó (Idempotent replay)',
      })
    }

    return response
  },
  async (error: AxiosError<ApiProblemDetail | ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // Handle 401 Unauthorized token refresh
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      // Do not attempt refresh on auth endpoints themselves (login, refresh, logout)
      if (
        originalRequest.url?.includes('/auth/login') ||
        originalRequest.url?.includes('/auth/refresh')
      ) {
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then(() => apiClient(originalRequest))
          .catch((err) => Promise.reject(err))
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = useAuthStore.getState().refreshToken

      if (!refreshToken) {
        useAuthStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const response = await axios.post<ApiResponse<TokenRefreshResponse>>(
          '/api/v1/auth/refresh',
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } }
        )

        const newAccessToken = response.data.data.accessToken
        useAuthStore.getState().setAccessToken(newAccessToken)

        processQueue(null)
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        }
        return apiClient(originalRequest)
      } catch (refreshErr) {
        processQueue(refreshErr as Error)
        useAuthStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }

    // Handle 409 Conflict & Concurrency Errors
    if (error.response?.status === 409) {
      const problem = error.response.data as ApiProblemDetail | undefined
      const errorType = problem?.type

      if (errorType === ERROR_TYPE_VERSION_CONFLICT) {
        useConcurrencyNotificationStore.getState().showNotification({
          type: 'version-conflict',
          title: 'Xung đột phiên bản dữ liệu',
          message: 'Dữ liệu vừa được cập nhật bởi một người dùng khác. Vui lòng làm mới trang.',
          detail: problem?.detail,
          canReload: true,
        })
      } else if (errorType === ERROR_TYPE_IDEMPOTENCY_IN_PROGRESS) {
        useConcurrencyNotificationStore.getState().showNotification({
          type: 'idempotency-in-progress',
          title: 'Yêu cầu đang được xử lý',
          message: 'Yêu cầu tương tự đang được xử lý. Vui lòng không nhấn liên tục.',
          detail: problem?.detail,
        })
      } else {
        useConcurrencyNotificationStore.getState().showNotification({
          type: 'warning',
          title: 'Xung đột dữ liệu',
          message: problem?.detail || 'Có xung đột dữ liệu trong quá trình xử lý yêu cầu.',
        })
      }
    }

    // Handle 422 Unprocessable Entity (e.g. Idempotency Payload Mismatch)
    if (error.response?.status === 422) {
      const problem = error.response.data as ApiProblemDetail | undefined
      if (problem?.type === ERROR_TYPE_IDEMPOTENCY_PAYLOAD_MISMATCH) {
        useConcurrencyNotificationStore.getState().showNotification({
          type: 'error',
          title: 'Trùng lặp khoá giao dịch',
          message: 'Khoá Idempotency bị trùng lặp với nội dung khác. Vui lòng thử lại.',
          detail: problem?.detail,
        })
      }
    }

    return Promise.reject(error)
  }
)
