import { motion, AnimatePresence } from 'motion/react'
import {
  AlertTriangle,
  Clock,
  History,
  AlertCircle,
  RefreshCw,
  X,
} from 'lucide-react'
import {
  useConcurrencyNotificationStore,
  type ConcurrencyNotificationItem,
} from '@/shared/stores/concurrencyNotificationStore'
import { cn } from '@/shared/lib/utils'

export function ConcurrencyNotification() {
  const { notifications, dismissNotification } = useConcurrencyNotificationStore()

  if (notifications.length === 0) {
    return null
  }

  return (
    <aside
      aria-label="Thông báo đồng thời và khoá lạc quan"
      className="fixed top-4 right-4 z-50 flex flex-col gap-2.5 max-w-[calc(100vw-2rem)] sm:w-[420px] pointer-events-none"
    >
      <AnimatePresence mode="popLayout">
        {notifications.map((notification) => (
          <NotificationCard
            key={notification.id}
            notification={notification}
            onDismiss={() => dismissNotification(notification.id)}
          />
        ))}
      </AnimatePresence>
    </aside>
  )
}

interface NotificationCardProps {
  notification: ConcurrencyNotificationItem
  onDismiss: () => void
}

function NotificationCard({ notification, onDismiss }: NotificationCardProps) {
  const isConflict = notification.type === 'version-conflict'
  const isInProgress = notification.type === 'idempotency-in-progress'
  const isReplayed = notification.type === 'replayed'

  const handleReload = () => {
    onDismiss()
    window.location.reload()
  }

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: -16, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -12, scale: 0.94 }}
      transition={{ type: 'spring', stiffness: 400, damping: 30 }}
      role="alert"
      aria-live={isConflict ? 'assertive' : 'polite'}
      className={cn(
        'pointer-events-auto rounded-xl p-4 shadow-xl border backdrop-blur-md transition-colors',
        'bg-card/95 text-card-foreground',
        isConflict && 'border-destructive/60 ring-1 ring-destructive/30 bg-destructive/5 dark:bg-destructive/10',
        isInProgress && 'border-warning/50 ring-1 ring-warning/20 bg-warning/5 dark:bg-warning/10',
        isReplayed && 'border-secondary/50 ring-1 ring-secondary/20 bg-secondary/5 dark:bg-secondary/10',
        !isConflict && !isInProgress && !isReplayed && 'border-border'
      )}
    >
      <div className="flex items-start gap-3">
        <div className="shrink-0 mt-0.5">
          {isConflict && <AlertTriangle className="w-5 h-5 text-destructive animate-pulse" />}
          {isInProgress && <Clock className="w-5 h-5 text-warning" />}
          {isReplayed && <History className="w-5 h-5 text-secondary" />}
          {!isConflict && !isInProgress && !isReplayed && (
            <AlertCircle className="w-5 h-5 text-muted-foreground" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-2">
            <h4 className="text-sm font-semibold tracking-tight text-foreground">
              {notification.title}
            </h4>
            <button
              type="button"
              onClick={onDismiss}
              aria-label="Đóng thông báo"
              className="text-muted-foreground hover:text-foreground rounded p-0.5 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <p className="mt-1 text-xs text-muted-foreground leading-relaxed break-words">
            {notification.message}
          </p>

          {notification.detail && notification.detail !== notification.message && (
            <p className="mt-1 text-[11px] font-mono text-muted-foreground/80 break-words bg-background/50 rounded px-1.5 py-0.5 border border-border/50">
              {notification.detail}
            </p>
          )}

          {notification.canReload && (
            <div className="mt-3 flex items-center gap-2">
              <button
                type="button"
                onClick={handleReload}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg bg-primary text-primary-foreground hover:bg-primary/90 active:scale-95 transition-all shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                Làm mới trang
              </button>
              <button
                type="button"
                onClick={onDismiss}
                className="px-2.5 py-1.5 text-xs font-medium rounded-lg border border-border bg-background/60 hover:bg-accent hover:text-accent-foreground transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                Bỏ qua
              </button>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  )
}
