import { lazy } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { MainLayout } from './layout/MainLayout'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { PublicOnlyRoute } from './routes/PublicOnlyRoute'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'

// Lazy load non-dashboard feature pages for optimal bundle splitting (Vercel React best practice)
const MonthlyRevenuePage = lazy(() =>
  import('@/features/revenue-monthly/pages/MonthlyRevenuePage').then((m) => ({
    default: m.MonthlyRevenuePage,
  }))
)
const WorkforceExpensePage = lazy(() =>
  import('@/features/workforce-expense/pages/WorkforceExpensePage').then((m) => ({
    default: m.WorkforceExpensePage,
  }))
)
const InventoryPage = lazy(() =>
  import('@/features/inventory/pages/InventoryPage').then((m) => ({
    default: m.InventoryPage,
  }))
)
const FinancePage = lazy(() =>
  import('@/features/finance/pages/FinancePage').then((m) => ({
    default: m.FinancePage,
  }))
)
const ClosingPage = lazy(() =>
  import('@/features/closing/pages/ClosingPage').then((m) => ({
    default: m.ClosingPage,
  }))
)
const ReportingPage = lazy(() =>
  import('@/features/reporting/pages/ReportingPage').then((m) => ({
    default: m.ReportingPage,
  }))
)

export const router = createBrowserRouter([
  // Public-only route (Login)
  {
    element: <PublicOnlyRoute />,
    children: [
      {
        path: '/login',
        element: <LoginPage />,
      },
    ],
  },

  // Protected application routes
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/',
        element: <MainLayout />,
        children: [
          {
            index: true,
            element: <DashboardPage />,
          },
          {
            path: 'revenue',
            element: <MonthlyRevenuePage />,
          },
          {
            path: 'workforce',
            element: <WorkforceExpensePage />,
          },
          {
            path: 'inventory',
            element: <InventoryPage />,
          },
          {
            path: 'finance',
            element: <FinancePage />,
          },
          {
            path: 'closing',
            element: <ClosingPage />,
          },
          {
            path: 'reporting',
            element: <ReportingPage />,
          },
          {
            path: '*',
            element: <Navigate to="/" replace />,
          },
        ],
      },
    ],
  },
])
