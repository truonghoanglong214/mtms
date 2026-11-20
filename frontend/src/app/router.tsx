import { lazy } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { MainLayout } from './layout/MainLayout'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { PublicOnlyRoute } from './routes/PublicOnlyRoute'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage'

// Lazy load non-dashboard feature pages for optimal bundle splitting (Vercel React best practice)
const DailyRevenuePage = lazy(() =>
  import('@/features/revenue/pages/DailyRevenuePage').then((m) => ({
    default: m.DailyRevenuePage,
  }))
)
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
const InventoryCatalogPage = lazy(() =>
  import('@/features/inventory/pages/InventoryCatalogPage').then((m) => ({
    default: m.InventoryCatalogPage,
  }))
)
const OpeningInventoryPage = lazy(() =>
  import('@/features/inventory/pages/OpeningInventoryPage').then((m) => ({
    default: m.OpeningInventoryPage,
  }))
)
const GoodsReceiptPage = lazy(() =>
  import('@/features/inventory/pages/GoodsReceiptPage').then((m) => ({
    default: m.GoodsReceiptPage,
  }))
)
const StockIssuePage = lazy(() =>
  import('@/features/inventory/pages/StockIssuePage').then((m) => ({
    default: m.StockIssuePage,
  }))
)
const InventoryReportingPage = lazy(() =>
  import('@/features/inventory/pages/InventoryReportingPage').then((m) => ({
    default: m.InventoryReportingPage,
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
const OrganizationAdminPage = lazy(() =>
  import('@/features/organization/pages/OrganizationAdminPage').then((m) => ({
    default: m.OrganizationAdminPage,
  }))
)
const EmployeeManagementPage = lazy(() =>
  import('@/features/hr/pages/EmployeeManagementPage').then((m) => ({
    default: m.EmployeeManagementPage,
  }))
)
const TimesheetManagementPage = lazy(() =>
  import('@/features/hr/pages/TimesheetManagementPage').then((m) => ({
    default: m.TimesheetManagementPage,
  }))
)
const PayrollManagementPage = lazy(() =>
  import('@/features/hr/pages/PayrollManagementPage').then((m) => ({
    default: m.PayrollManagementPage,
  }))
)
const AssetManagementPage = lazy(() =>
  import('@/features/assets/pages/AssetManagementPage').then((m) => ({
    default: m.AssetManagementPage,
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
            path: 'revenue/daily',
            element: <DailyRevenuePage />,
          },
          {
            path: 'revenue',
            element: <DailyRevenuePage />,
          },
          {
            path: 'revenue/monthly',
            element: <MonthlyRevenuePage />,
          },
          {
            path: 'workforce',
            element: <WorkforceExpensePage />,
          },
          {
            path: 'hr/employees',
            element: <EmployeeManagementPage />,
          },
          {
            path: 'hr/timesheets',
            element: <TimesheetManagementPage />,
          },
          {
            path: 'hr/payroll',
            element: <PayrollManagementPage />,
          },
          {
            path: 'hr',
            element: <Navigate to="/hr/employees" replace />,
          },
          {
            path: 'assets',
            element: <AssetManagementPage />,
          },
          {
            path: 'inventory',
            element: <InventoryPage />,
          },
          {
            path: 'inventory/catalog',
            element: <InventoryCatalogPage />,
          },
          {
            path: 'inventory/opening',
            element: <OpeningInventoryPage />,
          },
          {
            path: 'inventory/receipts',
            element: <GoodsReceiptPage />,
          },
          {
            path: 'inventory/issues',
            element: <StockIssuePage />,
          },
          {
            path: 'inventory/reporting',
            element: <InventoryReportingPage />,
          },
          {
            path: 'inventory/dashboard',
            element: <InventoryReportingPage />,
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
            path: 'admin/organization',
            element: <OrganizationAdminPage />,
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
