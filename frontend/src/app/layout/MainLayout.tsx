import { useState, Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import { Header } from './Header'
import { Sidebar } from './Sidebar'

function PageLoadingFallback() {
  return (
    <div className="h-64 flex items-center justify-center">
      <div className="flex flex-col items-center gap-3">
        <div className="h-6 w-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
        <span className="text-xs text-muted-foreground font-medium">Đang tải phân hệ...</span>
      </div>
    </div>
  )
}

export function MainLayout() {
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground antialiased selection:bg-primary/20">
      <Header onToggleMobileSidebar={() => setIsMobileSidebarOpen(true)} />
      <div className="flex flex-1">
        <Sidebar
          isMobileOpen={isMobileSidebarOpen}
          onCloseMobile={() => setIsMobileSidebarOpen(false)}
        />
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto max-w-7xl w-full mx-auto">
          <Suspense fallback={<PageLoadingFallback />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  )
}
