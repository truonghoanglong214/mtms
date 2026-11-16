import type { AxiosRequestConfig } from 'axios'

export const IDEMPOTENCY_HEADER = 'Idempotency-Key'
export const IDEMPOTENT_REPLAYED_HEADER = 'idempotent-replayed'

export const ERROR_TYPE_VERSION_CONFLICT = 'urn:mtms:error:version-conflict'
export const ERROR_TYPE_IDEMPOTENCY_IN_PROGRESS = 'urn:mtms:error:idempotency-in-progress'
export const ERROR_TYPE_IDEMPOTENCY_PAYLOAD_MISMATCH = 'urn:mtms:error:idempotency-payload-mismatch'

/**
 * Generates an RFC 4122 v4 UUID for idempotency keys.
 * Uses crypto.randomUUID() when available in modern browsers / secure contexts,
 * with cryptographically secure fallback using crypto.getRandomValues().
 */
export function generateIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  // Fallback using crypto.getRandomValues
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    const bytes = new Uint8Array(16)
    crypto.getRandomValues(bytes)
    // Set version (4) and variant (RFC 4122)
    bytes[6] = (bytes[6] & 0x0f) | 0x40
    bytes[8] = (bytes[8] & 0x3f) | 0x80
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
  }

  // Pure Math.random fallback for non-crypto fallback environments
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/**
 * Helper to attach or override the Idempotency-Key header on an Axios request configuration.
 */
export function withIdempotencyKey(
  config: AxiosRequestConfig = {},
  key: string = generateIdempotencyKey()
): AxiosRequestConfig {
  const headers = config.headers ? { ...config.headers } : {}
  headers[IDEMPOTENCY_HEADER] = key
  return {
    ...config,
    headers,
  }
}

/**
 * Checks whether an HTTP method is a state mutation requiring deduplication.
 */
export function isMutationMethod(method?: string): boolean {
  if (!method) return false
  const upper = method.toUpperCase()
  return upper === 'POST' || upper === 'PUT' || upper === 'PATCH' || upper === 'DELETE'
}
