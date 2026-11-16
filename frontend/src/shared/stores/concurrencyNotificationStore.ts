import { create } from 'zustand'

export type ConcurrencyNotificationType =
  | 'version-conflict'
  | 'idempotency-in-progress'
  | 'replayed'
  | 'warning'
  | 'error'

export interface ConcurrencyNotificationItem {
  id: string
  type: ConcurrencyNotificationType
  title: string
  message: string
  detail?: string
  canReload?: boolean
  timestamp: number
  autoClose?: boolean
  durationMs?: number
}

interface ConcurrencyNotificationState {
  notifications: ConcurrencyNotificationItem[]
  showNotification: (
    item: Omit<ConcurrencyNotificationItem, 'id' | 'timestamp'> & {
      id?: string
    }
  ) => string
  dismissNotification: (id: string) => void
  clearAll: () => void
}

export const useConcurrencyNotificationStore = create<ConcurrencyNotificationState>((set) => ({
  notifications: [],

  showNotification: (item) => {
    const id = item.id || `cn-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    const timestamp = Date.now()
    const autoClose = item.autoClose ?? (item.type !== 'version-conflict')
    const durationMs = item.durationMs ?? (item.type === 'replayed' ? 4000 : 6000)

    const newItem: ConcurrencyNotificationItem = {
      ...item,
      id,
      timestamp,
      autoClose,
      durationMs,
    }

    set((state) => {
      // Deduplicate if already present with same type and message within last 3 seconds
      const exists = state.notifications.some(
        (n) => n.type === item.type && n.message === item.message && timestamp - n.timestamp < 3000
      )
      if (exists) {
        return state
      }
      return {
        notifications: [...state.notifications, newItem],
      }
    })

    if (autoClose) {
      setTimeout(() => {
        set((state) => ({
          notifications: state.notifications.filter((n) => n.id !== id),
        }))
      }, durationMs)
    }

    return id
  },

  dismissNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    })),

  clearAll: () => set({ notifications: [] }),
}))
