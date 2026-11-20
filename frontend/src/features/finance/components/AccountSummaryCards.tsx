import React from 'react'
import { motion } from 'motion/react'
import {
  Wallet,
  Landmark,
  Coins,
  RefreshCw,
  TrendingUp,
  CheckCircle2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { formatVnd } from '../api/financeApi'
import type { FinancialAccountDto } from '../types/finance.types'

interface AccountSummaryCardsProps {
  accounts: FinancialAccountDto[]
  isLoading?: boolean
  onRefresh?: () => void
  selectedAccountFilter?: string
  onSelectAccountFilter?: (accountId?: string) => void
  lastUpdated?: Date | null
}

export const AccountSummaryCards: React.FC<AccountSummaryCardsProps> = ({
  accounts,
  isLoading = false,
  onRefresh,
  selectedAccountFilter,
  onSelectAccountFilter,
  lastUpdated,
}) => {
  // Aggregate balances
  const cashAccounts = accounts.filter((a) => a.accountKind === 'CASH' && a.status === 'ACTIVE')
  const bankAccounts = accounts.filter((a) => a.accountKind === 'BANK' && a.status === 'ACTIVE')

  const totalCash = cashAccounts.reduce(
    (sum, a) => sum + (typeof a.currentBalance === 'string' ? parseFloat(a.currentBalance) || 0 : a.currentBalance || 0),
    0
  )

  const totalBank = bankAccounts.reduce(
    (sum, a) => sum + (typeof a.currentBalance === 'string' ? parseFloat(a.currentBalance) || 0 : a.currentBalance || 0),
    0
  )

  const totalLiquid = totalCash + totalBank

  const isCashActive = selectedAccountFilter && cashAccounts.some((a) => a.id === selectedAccountFilter)
  const isBankActive = selectedAccountFilter && bankAccounts.some((a) => a.id === selectedAccountFilter)
  const isAllActive = !selectedAccountFilter

  return (
    <div className="space-y-3">
      {/* Header bar with Refresh button & Last updated info */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-1">
        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
          <TrendingUp className="w-4 h-4 text-[#819B78]" />
          <span>Tổng quan số dư tiền khả dụng (Cập nhật tức thời theo sổ nhật ký)</span>
          {lastUpdated && (
            <span className="text-[11px] text-slate-400">
              · Lần cuối: {lastUpdated.toLocaleTimeString('vi-VN')}
            </span>
          )}
        </div>

        {onRefresh && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onRefresh}
            disabled={isLoading}
            className="h-7 text-xs text-slate-600 hover:text-slate-900 hover:bg-slate-100 flex items-center gap-1.5 cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-[#B96849]' : ''}`} />
            <span>{isLoading ? 'Đang cập nhật...' : 'Cập nhật số dư'}</span>
          </Button>
        )}
      </div>

      {/* 3 KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Card 1: Quỹ tiền mặt (Cash) */}
        <motion.div
          whileHover={{ y: -2 }}
          transition={{ duration: 0.15 }}
          onClick={() => {
            if (onSelectAccountFilter) {
              if (cashAccounts.length === 1) {
                onSelectAccountFilter(isCashActive ? undefined : cashAccounts[0].id)
              } else if (cashAccounts.length > 0) {
                onSelectAccountFilter(isCashActive ? undefined : cashAccounts[0].id)
              }
            }
          }}
          className={`relative p-5 rounded-2xl bg-white border transition-all cursor-pointer shadow-xs ${
            isCashActive
              ? 'border-emerald-500 ring-2 ring-emerald-500/20 bg-emerald-50/20 shadow-md'
              : 'border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Quỹ Tiền Mặt (Két)
                </span>
                <Badge
                  variant="outline"
                  className="bg-emerald-50 text-emerald-700 border-emerald-200 text-[10px] font-mono px-1.5 py-0"
                >
                  CASH
                </Badge>
              </div>
              <div className="text-2xl font-bold tracking-tight text-slate-900 font-mono">
                {isLoading ? (
                  <span className="text-slate-300 animate-pulse">Đang tải...</span>
                ) : (
                  formatVnd(totalCash)
                )}
              </div>
            </div>
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-100/70 text-emerald-700">
              <Wallet className="h-5 w-5" />
            </div>
          </div>

          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>
              {cashAccounts.length > 0
                ? `${cashAccounts.length} két tiền mặt tại quầy`
                : 'Chưa khởi tạo quỹ két'}
            </span>
            {isCashActive ? (
              <span className="text-emerald-600 font-medium flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" /> Đang lọc
              </span>
            ) : (
              <span className="text-slate-400 group-hover:text-slate-600">Bấm để lọc sổ quỹ</span>
            )}
          </div>
        </motion.div>

        {/* Card 2: Quỹ Ngân hàng (Bank) */}
        <motion.div
          whileHover={{ y: -2 }}
          transition={{ duration: 0.15 }}
          onClick={() => {
            if (onSelectAccountFilter) {
              if (bankAccounts.length === 1) {
                onSelectAccountFilter(isBankActive ? undefined : bankAccounts[0].id)
              } else if (bankAccounts.length > 0) {
                onSelectAccountFilter(isBankActive ? undefined : bankAccounts[0].id)
              }
            }
          }}
          className={`relative p-5 rounded-2xl bg-white border transition-all cursor-pointer shadow-xs ${
            isBankActive
              ? 'border-sky-500 ring-2 ring-sky-500/20 bg-sky-50/20 shadow-md'
              : 'border-slate-200 hover:border-slate-300'
          }`}
        >
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Tài Khoản Ngân Hàng
                </span>
                <Badge
                  variant="outline"
                  className="bg-sky-50 text-sky-700 border-sky-200 text-[10px] font-mono px-1.5 py-0"
                >
                  BANK
                </Badge>
              </div>
              <div className="text-2xl font-bold tracking-tight text-slate-900 font-mono">
                {isLoading ? (
                  <span className="text-slate-300 animate-pulse">Đang tải...</span>
                ) : (
                  formatVnd(totalBank)
                )}
              </div>
            </div>
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-sky-100/70 text-sky-700">
              <Landmark className="h-5 w-5" />
            </div>
          </div>

          <div className="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>
              {bankAccounts.length > 0
                ? `${bankAccounts.length} tài khoản liên kết`
                : 'Chưa liên kết tài khoản ngân hàng'}
            </span>
            {isBankActive ? (
              <span className="text-sky-600 font-medium flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" /> Đang lọc
              </span>
            ) : (
              <span className="text-slate-400 group-hover:text-slate-600">Bấm để lọc sổ quỹ</span>
            )}
          </div>
        </motion.div>

        {/* Card 3: Tổng tiền lưu động / Bảo toàn tài sản */}
        <motion.div
          whileHover={{ y: -2 }}
          transition={{ duration: 0.15 }}
          onClick={() => {
            if (onSelectAccountFilter) {
              onSelectAccountFilter(undefined)
            }
          }}
          className={`relative p-5 rounded-2xl bg-[#304638] text-white border transition-all cursor-pointer shadow-md ${
            isAllActive
              ? 'ring-2 ring-[#B96849] border-transparent'
              : 'border-[#3E5747] hover:border-[#819B78]'
          }`}
        >
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-[#C8CEC4]">
                  Tổng Tiền Lưu Động
                </span>
                <Badge
                  variant="outline"
                  className="bg-[#25372C] text-[#819B78] border-[#3E5747] text-[10px] font-mono px-1.5 py-0"
                >
                  TC-010
                </Badge>
              </div>
              <div className="text-2xl font-bold tracking-tight text-white font-mono">
                {isLoading ? (
                  <span className="text-slate-400 animate-pulse">Đang tải...</span>
                ) : (
                  formatVnd(totalLiquid)
                )}
              </div>
            </div>
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#25372C] text-[#819B78]">
              <Coins className="h-5 w-5" />
            </div>
          </div>

          <div className="mt-3 pt-3 border-t border-[#3E5747] flex items-center justify-between text-xs text-[#C8CEC4]">
            <span className="truncate">
              Bảo toàn tài sản · Tiền mặt + Ngân hàng
            </span>
            {isAllActive && (
              <span className="text-[#819B78] font-medium flex items-center gap-1 shrink-0">
                <CheckCircle2 className="w-3 h-3" /> Tất cả
              </span>
            )}
          </div>
        </motion.div>
      </div>

      {/* Account detail chips if multiple accounts exist */}
      {accounts.length > 2 && (
        <div className="flex items-center gap-2 flex-wrap pt-1 text-xs">
          <span className="text-slate-400 text-[11px]">Tài khoản chi nhánh:</span>
          {accounts.map((acc) => {
            const isSelected = selectedAccountFilter === acc.id
            return (
              <button
                key={acc.id}
                type="button"
                onClick={() =>
                  onSelectAccountFilter &&
                  onSelectAccountFilter(isSelected ? undefined : acc.id)
                }
                className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono transition-colors border ${
                  isSelected
                    ? 'bg-[#304638] text-white border-[#304638]'
                    : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                }`}
              >
                <span>{acc.accountName}</span>
                <span className="text-[11px] opacity-75">
                  ({formatVnd(acc.currentBalance)})
                </span>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
