import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  Building2,
  Store,
  Users,
  ShieldCheck,
  Plus,
  Search,
  AlertCircle,
  Loader2,
  Trash2,
  Lock,
  Unlock,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  RefreshCw,
  CreditCard,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { organizationApi } from '../organizationApi'
import { CreateEntityModal } from '../components/CreateEntityModal'
import { CreateBranchModal } from '../components/CreateBranchModal'
import { CreateUserModal } from '../components/CreateUserModal'
import { GrantRoleModal } from '../components/GrantRoleModal'
import { BranchBankModal } from '../components/BranchBankModal'
import type { BusinessEntityDto, BranchDto, UserDto } from '../types'

export const OrganizationAdminPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'org' | 'users'>('org')

  // Data states
  const [entities, setEntities] = useState<BusinessEntityDto[]>([])
  const [branchesByEntity, setBranchesByEntity] = useState<Record<string, BranchDto[]>>({})
  const [users, setUsers] = useState<UserDto[]>([])
  const [expandedEntities, setExpandedEntities] = useState<Record<string, boolean>>({})

  // Loading & error
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Search
  const [orgSearch, setOrgSearch] = useState('')
  const [userSearch, setUserSearch] = useState('')

  // Modals state
  const [isEntityModalOpen, setIsEntityModalOpen] = useState(false)
  const [isBranchModalOpen, setIsBranchModalOpen] = useState(false)
  const [selectedEntityForBranch, setSelectedEntityForBranch] = useState<string | undefined>()
  const [isUserModalOpen, setIsUserModalOpen] = useState(false)
  const [isGrantModalOpen, setIsGrantModalOpen] = useState(false)
  const [selectedUserForGrant, setSelectedUserForGrant] = useState<string | undefined>()
  const [isBankModalOpen, setIsBankModalOpen] = useState(false)
  const [selectedBranchForBank, setSelectedBranchForBank] = useState<BranchDto | null>(null)

  // Fetch all initial data
  const loadData = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const [fetchedEntities, fetchedUsers] = await Promise.all([
        organizationApi.getEntities(),
        organizationApi.getUsers(),
      ])

      setEntities(fetchedEntities)
      setUsers(fetchedUsers)

      // Automatically expand all entities
      const expanded: Record<string, boolean> = {}
      fetchedEntities.forEach((ent) => {
        expanded[ent.id] = true
      })
      setExpandedEntities(expanded)

      // Fetch branches for each entity in parallel
      const branchPromises = fetchedEntities.map(async (ent) => {
        try {
          const branches = await organizationApi.getBranches(ent.id)
          return { entityId: ent.id, branches }
        } catch {
          return { entityId: ent.id, branches: [] }
        }
      })

      const branchResults = await Promise.all(branchPromises)
      const branchMap: Record<string, BranchDto[]> = {}
      branchResults.forEach((res) => {
        branchMap[res.entityId] = res.branches
      })
      setBranchesByEntity(branchMap)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải dữ liệu tổ chức và người dùng.'
      )
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const showToast = (message: string) => {
    setSuccessToast(message)
    setTimeout(() => setSuccessToast(null), 4000)
  }

  // All branches flattened for grant modal
  const allBranches = useMemo(() => {
    return Object.values(branchesByEntity).flat()
  }, [branchesByEntity])

  // Filtered entities & branches
  const filteredEntities = useMemo(() => {
    if (!orgSearch.trim()) return entities
    const q = orgSearch.trim().toLowerCase()
    return entities.filter(
      (e) =>
        e.entityCode.toLowerCase().includes(q) ||
        e.registeredName.toLowerCase().includes(q) ||
        e.taxCode.toLowerCase().includes(q)
    )
  }, [entities, orgSearch])

  // Filtered users
  const filteredUsers = useMemo(() => {
    if (!userSearch.trim()) return users
    const q = userSearch.trim().toLowerCase()
    return users.filter(
      (u) =>
        u.loginIdentifier.toLowerCase().includes(q) ||
        u.displayName.toLowerCase().includes(q)
    )
  }, [users, userSearch])

  // Toggle user active/suspended status
  const handleToggleUserStatus = async (user: UserDto) => {
    const nextStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    try {
      const updated = await organizationApi.updateUserStatus(user.id, { status: nextStatus })
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)))
      showToast(
        `Đã chuyển trạng thái tài khoản @${user.loginIdentifier} sang ${nextStatus}`
      )
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể cập nhật trạng thái người dùng.'
      )
    }
  }

  // Revoke grant
  const handleRevokeGrant = async (grantId: string, username: string) => {
    if (!confirm(`Bạn có chắc chắn muốn thu hồi quyền này của người dùng @${username}?`)) {
      return
    }
    try {
      await organizationApi.revokeGrant(grantId)
      // Reload users to refresh active grants
      const updatedUsers = await organizationApi.getUsers()
      setUsers(updatedUsers)
      showToast('Thu hồi quyền thành công')
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể thu hồi quyền.'
      )
    }
  }

  return (
    <div className="space-y-6">
      {/* Toast alert */}
      <AnimatePresence>
        {successToast && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="fixed top-4 right-4 z-50 flex items-center space-x-2 rounded-lg border border-emerald-500/30 bg-emerald-950/90 px-4 py-3 text-xs text-emerald-200 shadow-xl backdrop-blur-md"
          >
            <CheckCircle2 className="h-4 w-4 text-emerald-400" />
            <span>{successToast}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-stone-800">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-stone-100 flex items-center space-x-2.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-950/80 border border-emerald-500/20 text-emerald-400">
              <Building2 className="h-4 w-4" />
            </span>
            <span>Cơ cấu tổ chức & Người dùng</span>
          </h1>
          <p className="mt-1 text-xs text-stone-400">
            Quản trị mạng lưới pháp nhân, chi nhánh, tài khoản người dùng và phân quyền hạn theo chi nhánh (Scoped Role Grants).
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            size="sm"
            onClick={loadData}
            disabled={isLoading}
            className="border-stone-800 text-stone-300 hover:bg-stone-800 text-xs"
          >
            <RefreshCw className={`h-3.5 w-3.5 mr-1.5 ${isLoading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>

          {activeTab === 'org' ? (
            <>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSelectedEntityForBranch(undefined)
                  setIsBranchModalOpen(true)
                }}
                disabled={entities.length === 0}
                className="border-stone-800 text-stone-300 hover:bg-stone-800 text-xs"
              >
                <Store className="h-3.5 w-3.5 mr-1.5 text-emerald-400" />
                Thêm Chi nhánh
              </Button>
              <Button
                size="sm"
                onClick={() => setIsEntityModalOpen(true)}
                className="bg-emerald-600 text-white hover:bg-emerald-500 text-xs"
              >
                <Plus className="h-3.5 w-3.5 mr-1.5" />
                Thêm Pháp nhân
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSelectedUserForGrant(undefined)
                  setIsGrantModalOpen(true)
                }}
                disabled={users.length === 0}
                className="border-stone-800 text-stone-300 hover:bg-stone-800 text-xs"
              >
                <ShieldCheck className="h-3.5 w-3.5 mr-1.5 text-emerald-400" />
                Cấp quyền Chi nhánh
              </Button>
              <Button
                size="sm"
                onClick={() => setIsUserModalOpen(true)}
                className="bg-emerald-600 text-white hover:bg-emerald-500 text-xs"
              >
                <Plus className="h-3.5 w-3.5 mr-1.5" />
                Thêm Người dùng
              </Button>
            </>
          )}
        </div>
      </div>

      {error && (
        <div className="flex items-center space-x-2 rounded-lg border border-red-500/30 bg-red-950/40 p-3 text-xs text-red-300">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-400" />
          <span>{error}</span>
        </div>
      )}

      {/* Tabs navigation */}
      <div className="flex space-x-2 border-b border-stone-800 pb-px">
        <button
          onClick={() => setActiveTab('org')}
          className={`flex items-center space-x-2 px-4 py-2 text-xs font-semibold rounded-t-lg transition-colors border-b-2 ${
            activeTab === 'org'
              ? 'border-emerald-500 text-emerald-400 bg-emerald-950/20'
              : 'border-transparent text-stone-400 hover:text-stone-200 hover:bg-stone-900/40'
          }`}
        >
          <Building2 className="h-4 w-4" />
          <span>Pháp nhân & Chi nhánh</span>
          <span className="ml-1 rounded-full bg-stone-800 px-2 py-0.5 text-[10px] text-stone-300 font-mono">
            {entities.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('users')}
          className={`flex items-center space-x-2 px-4 py-2 text-xs font-semibold rounded-t-lg transition-colors border-b-2 ${
            activeTab === 'users'
              ? 'border-emerald-500 text-emerald-400 bg-emerald-950/20'
              : 'border-transparent text-stone-400 hover:text-stone-200 hover:bg-stone-900/40'
          }`}
        >
          <Users className="h-4 w-4" />
          <span>Tài khoản & Phân quyền</span>
          <span className="ml-1 rounded-full bg-stone-800 px-2 py-0.5 text-[10px] text-stone-300 font-mono">
            {users.length}
          </span>
        </button>
      </div>

      {/* Tab 1: Organization (Entities & Branches) */}
      {activeTab === 'org' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-4">
            <div className="relative w-full max-w-sm">
              <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-stone-500" />
              <Input
                value={orgSearch}
                onChange={(e) => setOrgSearch(e.target.value)}
                placeholder="Tìm theo mã pháp nhân, tên công ty, MST..."
                className="pl-9 bg-stone-950 border-stone-800 text-xs text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
              />
            </div>
            <div className="text-xs text-stone-400 font-mono">
              Tổng số chi nhánh: <span className="text-emerald-400 font-bold">{allBranches.length}</span>
            </div>
          </div>

          {isLoading ? (
            <div className="flex h-48 items-center justify-center rounded-xl border border-stone-800 bg-stone-900/30">
              <Loader2 className="h-6 w-6 animate-spin text-emerald-500" />
            </div>
          ) : filteredEntities.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center rounded-xl border border-dashed border-stone-800 bg-stone-900/20">
              <Building2 className="h-8 w-8 text-stone-600 mb-2" />
              <p className="text-xs text-stone-400">Chưa có pháp nhân nào phù hợp với tìm kiếm.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredEntities.map((entity) => {
                const entityBranches = branchesByEntity[entity.id] || []
                const isExpanded = expandedEntities[entity.id] ?? true

                return (
                  <div
                    key={entity.id}
                    className="rounded-xl border border-stone-800 bg-stone-900/70 overflow-hidden shadow-sm"
                  >
                    {/* Entity Banner */}
                    <div className="flex items-center justify-between p-4 bg-stone-900/90 border-b border-stone-800/80">
                      <div className="flex items-center space-x-3">
                        <button
                          onClick={() =>
                            setExpandedEntities((prev) => ({
                              ...prev,
                              [entity.id]: !isExpanded,
                            }))
                          }
                          className="rounded p-1 text-stone-400 hover:bg-stone-800 transition-colors"
                        >
                          {isExpanded ? (
                            <ChevronDown className="h-4 w-4" />
                          ) : (
                            <ChevronRight className="h-4 w-4" />
                          )}
                        </button>
                        <div>
                          <div className="flex items-center space-x-2">
                            <span className="font-mono text-xs font-bold text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-500/20">
                              {entity.entityCode}
                            </span>
                            <h3 className="text-sm font-semibold text-stone-100">
                              {entity.registeredName}
                            </h3>
                            <Badge
                              variant={entity.status === 'ACTIVE' ? 'default' : 'secondary'}
                              className={
                                entity.status === 'ACTIVE'
                                  ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/30 text-[10px]'
                                  : 'bg-stone-800 text-stone-400 text-[10px]'
                              }
                            >
                              {entity.status}
                            </Badge>
                          </div>
                          <div className="mt-1 flex items-center space-x-4 text-[11px] text-stone-400 font-mono">
                            <span>MST: {entity.taxCode || 'Chưa cập nhật'}</span>
                            <span>•</span>
                            <span>{entityBranches.length} chi nhánh</span>
                          </div>
                        </div>
                      </div>

                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          setSelectedEntityForBranch(entity.id)
                          setIsBranchModalOpen(true)
                        }}
                        className="text-xs text-emerald-400 hover:text-emerald-300 hover:bg-emerald-950/40"
                      >
                        <Plus className="h-3 w-3 mr-1" />
                        Thêm Chi nhánh
                      </Button>
                    </div>

                    {/* Branches Table */}
                    {isExpanded && (
                      <div className="p-0">
                        {entityBranches.length === 0 ? (
                          <div className="p-6 text-center text-xs text-stone-500 font-mono">
                            Pháp nhân này chưa có chi nhánh nào. Nhấn "+ Thêm Chi nhánh" để bắt đầu.
                          </div>
                        ) : (
                          <div className="overflow-x-auto">
                            <table className="w-full text-left text-xs">
                              <thead className="bg-stone-950/60 text-stone-400 font-mono border-b border-stone-800">
                                <tr>
                                  <th className="py-2.5 px-4 font-medium">Mã Chi nhánh</th>
                                  <th className="py-2.5 px-4 font-medium">Tên Chi nhánh / Quán</th>
                                  <th className="py-2.5 px-4 font-medium">Múi giờ</th>
                                  <th className="py-2.5 px-4 font-medium">Trạng thái</th>
                                  <th className="py-2.5 px-4 font-medium">Ngày tạo</th>
                                  <th className="py-2.5 px-4 font-medium text-right">Thao tác</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-stone-800/60 text-stone-200">
                                {entityBranches.map((branch) => (
                                  <tr
                                    key={branch.id}
                                    className="hover:bg-stone-800/30 transition-colors"
                                  >
                                    <td className="py-2.5 px-4 font-mono font-bold text-emerald-400">
                                      {branch.branchCode}
                                    </td>
                                    <td className="py-2.5 px-4 font-medium text-stone-100">
                                      {branch.name}
                                    </td>
                                    <td className="py-2.5 px-4 font-mono text-stone-400 text-[11px]">
                                      {branch.timeZone}
                                    </td>
                                    <td className="py-2.5 px-4">
                                      <Badge
                                        variant="outline"
                                        className={
                                          branch.status === 'ACTIVE'
                                            ? 'border-emerald-500/40 text-emerald-300 bg-emerald-950/40 text-[10px]'
                                            : branch.status === 'DRAFT'
                                            ? 'border-amber-500/40 text-amber-300 bg-amber-950/40 text-[10px]'
                                            : 'border-stone-700 text-stone-400 text-[10px]'
                                        }
                                      >
                                        {branch.status}
                                      </Badge>
                                    </td>
                                    <td className="py-2.5 px-4 font-mono text-[11px] text-stone-400">
                                      {new Date(branch.createdAt).toLocaleDateString('vi-VN')}
                                    </td>
                                    <td className="py-2.5 px-4 text-right">
                                      <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={() => {
                                          setSelectedBranchForBank(branch)
                                          setIsBankModalOpen(true)
                                        }}
                                        className="h-7 px-2.5 text-xs text-emerald-400 hover:text-emerald-300 hover:bg-emerald-950/40 inline-flex items-center space-x-1"
                                        title="Quản lý tài khoản ngân hàng & Go-live readiness"
                                      >
                                        <CreditCard className="h-3.5 w-3.5 mr-1" />
                                        <span>Tài khoản NH</span>
                                      </Button>
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Users & Scoped Role Grants */}
      {activeTab === 'users' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-4">
            <div className="relative w-full max-w-sm">
              <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-stone-500" />
              <Input
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
                placeholder="Tìm theo tên đăng nhập hoặc tên hiển thị..."
                className="pl-9 bg-stone-950 border-stone-800 text-xs text-stone-100 placeholder:text-stone-600 focus:border-emerald-500"
              />
            </div>
            <div className="text-xs text-stone-400 font-mono">
              Tổng số tài khoản: <span className="text-emerald-400 font-bold">{users.length}</span>
            </div>
          </div>

          {isLoading ? (
            <div className="flex h-48 items-center justify-center rounded-xl border border-stone-800 bg-stone-900/30">
              <Loader2 className="h-6 w-6 animate-spin text-emerald-500" />
            </div>
          ) : filteredUsers.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center rounded-xl border border-dashed border-stone-800 bg-stone-900/20">
              <Users className="h-8 w-8 text-stone-600 mb-2" />
              <p className="text-xs text-stone-400">Chưa có người dùng nào phù hợp với tìm kiếm.</p>
            </div>
          ) : (
            <div className="rounded-xl border border-stone-800 bg-stone-900/70 overflow-hidden shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-stone-950/80 text-stone-400 font-mono border-b border-stone-800">
                    <tr>
                      <th className="py-3 px-4 font-medium">Người dùng</th>
                      <th className="py-3 px-4 font-medium">Trạng thái</th>
                      <th className="py-3 px-4 font-medium">Quyền hạn theo Chi nhánh (Scoped Grants)</th>
                      <th className="py-3 px-4 font-medium text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-800/60 text-stone-200">
                    {filteredUsers.map((user) => {
                      const userGrants = user.grants || []

                      return (
                        <tr key={user.id} className="hover:bg-stone-800/30 transition-colors">
                          <td className="py-3 px-4">
                            <div className="font-semibold text-stone-100 text-sm">
                              {user.displayName}
                            </div>
                            <div className="font-mono text-xs text-emerald-400/90">
                              @{user.loginIdentifier}
                            </div>
                          </td>

                          <td className="py-3 px-4">
                            <Badge
                              variant="outline"
                              className={
                                user.status === 'ACTIVE'
                                  ? 'border-emerald-500/40 text-emerald-300 bg-emerald-950/40 text-[10px]'
                                  : 'border-red-500/40 text-red-300 bg-red-950/40 text-[10px]'
                              }
                            >
                              {user.status}
                            </Badge>
                          </td>

                          <td className="py-3 px-4 max-w-md">
                            {userGrants.length === 0 ? (
                              <span className="text-[11px] text-stone-500 italic font-mono">
                                Chưa có phân quyền chi nhánh
                              </span>
                            ) : (
                              <div className="flex flex-wrap gap-1.5">
                                {userGrants.map((grant) => {
                                  // Find branch name
                                  const branch = allBranches.find((b) => b.id === grant.branchId)
                                  const isGlobalAdmin = grant.roleKey === 'SYSTEM_ADMIN'

                                  return (
                                    <div
                                      key={grant.id}
                                      className="group inline-flex items-center space-x-1 rounded-md border border-stone-700 bg-stone-800/80 px-2 py-0.5 text-[11px] font-mono text-stone-200"
                                    >
                                      <span
                                        className={
                                          isGlobalAdmin
                                            ? 'text-amber-400 font-bold'
                                            : 'text-emerald-400 font-semibold'
                                        }
                                      >
                                        {grant.roleKey}
                                      </span>
                                      <span className="text-stone-400">@</span>
                                      <span className="text-stone-300">
                                        {isGlobalAdmin
                                          ? 'TOÀN HỆ THỐNG'
                                          : branch?.branchCode || grant.branchId?.slice(0, 8)}
                                      </span>
                                      <button
                                        onClick={() =>
                                          handleRevokeGrant(grant.id, user.loginIdentifier)
                                        }
                                        title="Thu hồi quyền"
                                        className="ml-1 text-stone-500 hover:text-red-400 transition-colors"
                                      >
                                        <Trash2 className="h-3 w-3" />
                                      </button>
                                    </div>
                                  )
                                })}
                              </div>
                            )}
                          </td>

                          <td className="py-3 px-4 text-right">
                            <div className="flex items-center justify-end space-x-1.5">
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => {
                                  setSelectedUserForGrant(user.id)
                                  setIsGrantModalOpen(true)
                                }}
                                className="h-8 px-2 text-xs text-emerald-400 hover:bg-emerald-950/40"
                              >
                                <ShieldCheck className="h-3.5 w-3.5 mr-1" />
                                Cấp quyền
                              </Button>

                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleToggleUserStatus(user)}
                                className={`h-8 px-2 text-xs ${
                                  user.status === 'ACTIVE'
                                    ? 'text-stone-400 hover:text-amber-400 hover:bg-amber-950/20'
                                    : 'text-stone-400 hover:text-emerald-400 hover:bg-emerald-950/20'
                                }`}
                                title={user.status === 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
                              >
                                {user.status === 'ACTIVE' ? (
                                  <Lock className="h-3.5 w-3.5" />
                                ) : (
                                  <Unlock className="h-3.5 w-3.5" />
                                )}
                              </Button>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modals */}
      <CreateEntityModal
        isOpen={isEntityModalOpen}
        onClose={() => setIsEntityModalOpen(false)}
        onSuccess={(newEntity) => {
          setEntities((prev) => [...prev, newEntity])
          setBranchesByEntity((prev) => ({ ...prev, [newEntity.id]: [] }))
          setExpandedEntities((prev) => ({ ...prev, [newEntity.id]: true }))
          showToast(`Đã tạo thành công pháp nhân [${newEntity.entityCode}]`)
        }}
      />

      <CreateBranchModal
        isOpen={isBranchModalOpen}
        entities={entities}
        selectedEntityId={selectedEntityForBranch}
        onClose={() => setIsBranchModalOpen(false)}
        onSuccess={(newBranch) => {
          setBranchesByEntity((prev) => ({
            ...prev,
            [newBranch.businessEntityId]: [
              ...(prev[newBranch.businessEntityId] || []),
              newBranch,
            ],
          }))
          showToast(`Đã tạo thành công chi nhánh [${newBranch.branchCode}]`)
        }}
      />

      <CreateUserModal
        isOpen={isUserModalOpen}
        onClose={() => setIsUserModalOpen(false)}
        onSuccess={(newUser) => {
          setUsers((prev) => [...prev, newUser])
          showToast(`Đã tạo thành công tài khoản @${newUser.loginIdentifier}`)
        }}
      />

      <GrantRoleModal
        isOpen={isGrantModalOpen}
        users={users}
        entities={entities}
        branches={allBranches}
        preselectedUserId={selectedUserForGrant}
        onClose={() => setIsGrantModalOpen(false)}
        onSuccess={(newGrant) => {
          // Update user's grants list
          setUsers((prev) =>
            prev.map((u) => {
              if (u.id === newGrant.userId) {
                return {
                  ...u,
                  grants: [...(u.grants || []), newGrant],
                }
              }
              return u
            })
          )
          showToast(`Đã cấp quyền ${newGrant.roleKey} thành công`)
        }}
      />

      <BranchBankModal
        isOpen={isBankModalOpen}
        branch={selectedBranchForBank}
        onClose={() => {
          setIsBankModalOpen(false)
          setSelectedBranchForBank(null)
        }}
        onUpdated={async () => {
          await loadData()
          if (selectedBranchForBank) {
            try {
              const branches = await organizationApi.getBranches(
                selectedBranchForBank.businessEntityId
              )
              const updated = branches.find((b) => b.id === selectedBranchForBank.id)
              if (updated) setSelectedBranchForBank(updated)
            } catch {
              // ignore
            }
          }
        }}
      />
    </div>
  )
}
export default OrganizationAdminPage
