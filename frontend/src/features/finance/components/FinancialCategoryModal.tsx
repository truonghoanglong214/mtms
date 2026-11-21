import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Layers,
  Lock,
  Plus,
  Search,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ShieldCheck,
  Tag,
  ArrowDownLeft,
  ArrowUpRight,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi } from '../api/financeApi'
import {
  CATEGORY_GROUP_LABELS,
  type FinancialCategoryDto,
} from '../types/finance.types'

interface FinancialCategoryModalProps {
  isOpen: boolean
  categories: FinancialCategoryDto[]
  onClose: () => void
  onCategoryCreated: (category: FinancialCategoryDto) => void
}

type TabType = 'list' | 'create'

export const FinancialCategoryModal: React.FC<FinancialCategoryModalProps> = ({
  isOpen,
  categories,
  onClose,
  onCategoryCreated,
}) => {
  const [activeTab, setActiveTab] = useState<TabType>('list')
  const [searchQuery, setSearchQuery] = useState<string>('')
  const [selectedGroup, setSelectedGroup] = useState<string>('ALL')

  // Create form state
  const [categoryCode, setCategoryCode] = useState<string>('')
  const [categoryName, setCategoryName] = useState<string>('')
  const [description, setDescription] = useState<string>('')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  if (!isOpen) return null

  const filteredCategories = categories.filter((cat) => {
    if (selectedGroup !== 'ALL' && cat.categoryGroup !== selectedGroup) {
      return false
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim()
      const matchCode = cat.categoryCode.toLowerCase().includes(q)
      const matchName = cat.categoryName.toLowerCase().includes(q)
      const matchDesc = cat.description?.toLowerCase().includes(q)
      return matchCode || matchName || matchDesc
    }
    return true
  })

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)
    setSuccessToast(null)

    const cleanCode = categoryCode.trim().toUpperCase()
    const cleanName = categoryName.trim()

    if (!cleanCode) {
      setErrorMessage('Vui lòng nhập mã danh mục (ví dụ: OPEX_ICE).')
      return
    }

    if (!cleanName) {
      setErrorMessage('Vui lòng nhập tên danh mục chi phí.')
      return
    }

    try {
      setIsSubmitting(true)
      const created = await financeApi.createCategory({
        categoryCode: cleanCode,
        categoryName: cleanName,
        description: description.trim() || undefined,
        categoryGroup: 'OPERATING_EXPENSE',
        flowDirection: 'OUTFLOW',
      })

      setSuccessToast(`Đã tạo thành công danh mục: ${created.categoryName}`)
      setCategoryCode('')
      setCategoryName('')
      setDescription('')
      onCategoryCreated(created)

      setTimeout(() => {
        setSuccessToast(null)
        setActiveTab('list')
      }, 1200)
    } catch (err: unknown) {
      console.error('Failed to create category:', err)
      const apiErr = err as {
        response?: {
          data?: {
            title?: string
            detail?: string
            message?: string
          }
        }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tạo danh mục. Mã danh mục có thể đã tồn tại hoặc không hợp lệ.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto bg-black/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.98, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: 10 }}
          transition={{ duration: 0.18 }}
          className="relative w-full max-w-4xl rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden my-6 flex flex-col max-h-[90vh]"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50/70 shrink-0">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#304638] text-white shadow-xs">
                <Layers className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-slate-900">
                    Bảng Danh Mục Thu Chi & Kế Toán (Chart of Categories)
                  </h2>
                </div>
                <p className="text-xs text-slate-500">
                  Phân định rõ ràng giữa Doanh thu, Chi phí vận hành, Vốn góp, Nợ NCC và Chuyển quỹ nội bộ.
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
              aria-label="Đóng"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Navigation Tabs */}
          <div className="flex items-center px-6 border-b border-slate-200 bg-white shrink-0">
            <button
              type="button"
              onClick={() => setActiveTab('list')}
              className={`px-4 py-3 text-xs font-semibold border-b-2 transition-colors cursor-pointer flex items-center gap-2 ${
                activeTab === 'list'
                  ? 'border-[#304638] text-[#304638]'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <Tag className="w-4 h-4" />
              <span>Danh Mục Chuẩn Hệ Thống ({categories.length})</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('create')}
              className={`px-4 py-3 text-xs font-semibold border-b-2 transition-colors cursor-pointer flex items-center gap-2 ${
                activeTab === 'create'
                  ? 'border-[#304638] text-[#304638]'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              <Plus className="w-4 h-4" />
              <span>Thêm Danh Mục Chi Phí Con (OPEX)</span>
            </button>
          </div>

          {/* Content Area */}
          <div className="p-6 overflow-y-auto flex-1">
            {activeTab === 'list' ? (
              <div className="space-y-4">
                {/* Search & Filter Toolbar */}
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
                  <div className="relative flex-1 max-w-sm">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <Input
                      type="text"
                      placeholder="Tìm theo mã hoặc tên danh mục..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-9 text-xs"
                    />
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-xs text-slate-500">Nhóm:</span>
                    <select
                      value={selectedGroup}
                      onChange={(e) => setSelectedGroup(e.target.value)}
                      className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
                    >
                      <option value="ALL">Tất cả nhóm ({categories.length})</option>
                      <option value="OPERATING_EXPENSE">Chi phí vận hành</option>
                      <option value="REVENUE">Doanh thu bán hàng</option>
                      <option value="INTERNAL_TRANSFER">Chuyển quỹ nội bộ</option>
                      <option value="DEBT_SETTLEMENT">Nợ nhà cung cấp</option>
                      <option value="CAPITAL_FLOW">Dòng vốn chủ sở hữu</option>
                      <option value="PAYROLL">Chi lương nhân sự</option>
                    </select>
                  </div>
                </div>

                {/* Categories Table */}
                <div className="rounded-xl border border-slate-200 overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 border-b border-slate-200 font-semibold text-slate-600 uppercase">
                      <tr>
                        <th className="px-4 py-3">Mã danh mục</th>
                        <th className="px-4 py-3">Tên hiển thị</th>
                        <th className="px-4 py-3">Nhóm kế toán</th>
                        <th className="px-4 py-3">Hướng tiền</th>
                        <th className="px-4 py-3">Mô tả nghiệp vụ</th>
                        <th className="px-4 py-3 text-center">Thuộc tính</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {filteredCategories.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="py-8 text-center text-slate-400">
                            Không tìm thấy danh mục nào phù hợp với bộ lọc.
                          </td>
                        </tr>
                      ) : (
                        filteredCategories.map((cat) => (
                          <tr key={cat.id} className="hover:bg-slate-50/80 transition-colors">
                            <td className="px-4 py-2.5 font-mono font-semibold text-slate-900">
                              {cat.categoryCode}
                            </td>
                            <td className="px-4 py-2.5 font-medium text-slate-800">
                              {cat.categoryName}
                            </td>
                            <td className="px-4 py-2.5">
                              <Badge
                                variant="secondary"
                                className="text-[10px] font-normal px-2 py-0.5 bg-slate-100 text-slate-700"
                              >
                                {CATEGORY_GROUP_LABELS[cat.categoryGroup] || cat.categoryGroup}
                              </Badge>
                            </td>
                            <td className="px-4 py-2.5">
                              {cat.flowDirection === 'INFLOW' && (
                                <span className="inline-flex items-center gap-1 text-[11px] font-medium text-emerald-700">
                                  <ArrowDownLeft className="w-3 h-3 text-emerald-600" />
                                  Thu (+ Inflow)
                                </span>
                              )}
                              {cat.flowDirection === 'OUTFLOW' && (
                                <span className="inline-flex items-center gap-1 text-[11px] font-medium text-rose-700">
                                  <ArrowUpRight className="w-3 h-3 text-rose-600" />
                                  Chi (- Outflow)
                                </span>
                              )}
                              {cat.flowDirection === 'BOTH' && (
                                <span className="inline-flex items-center gap-1 text-[11px] font-medium text-sky-700">
                                  Hai chiều (Both)
                                </span>
                              )}
                            </td>
                            <td className="px-4 py-2.5 text-slate-500 max-w-xs truncate">
                              {cat.description || '—'}
                            </td>
                            <td className="px-4 py-2.5 text-center">
                              {cat.isSystem ? (
                                <Badge
                                  variant="outline"
                                  className="text-[10px] font-mono border-slate-300 text-slate-600 bg-slate-50 inline-flex items-center gap-1"
                                >
                                  <Lock className="w-2.5 h-2.5 text-slate-400" />
                                  Hệ thống
                                </Badge>
                              ) : (
                                <Badge
                                  variant="outline"
                                  className="text-[10px] font-mono border-emerald-300 text-emerald-700 bg-emerald-50"
                                >
                                  Tùy chỉnh
                                </Badge>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              /* Tab 2: Create Custom OPEX Category */
              <form onSubmit={handleCreateSubmit} className="space-y-5 max-w-xl mx-auto py-2">
                {/* Explain banner */}
                <div className="p-4 rounded-xl bg-amber-50/70 border border-amber-200/80 flex items-start gap-3 text-xs leading-relaxed text-amber-900">
                  <ShieldCheck className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
                  <div>
                    <strong className="font-semibold block text-amber-950">
                      Quy chuẩn phân loại danh mục:
                    </strong>
                    Các danh mục do người dùng tạo mới bắt buộc thuộc nhóm <strong>Chi phí vận hành (OPERATING_EXPENSE)</strong> và dòng tiền <strong>Chi ra (OUTFLOW)</strong>. Các nhóm Doanh thu, Vốn chủ sở hữu và Chuyển nội bộ là chuẩn hệ thống bất biến.
                  </div>
                </div>

                {successToast && (
                  <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-2 text-xs text-emerald-800">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>{successToast}</span>
                  </div>
                )}

                {errorMessage && (
                  <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2 text-xs text-rose-800">
                    <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                    <span>{errorMessage}</span>
                  </div>
                )}

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700">
                    Mã danh mục (Viết hoa, không dấu, bắt đầu bằng OPEX_) *
                  </label>
                  <Input
                    type="text"
                    placeholder="Ví dụ: OPEX_ICE, OPEX_UNIFORM..."
                    value={categoryCode}
                    onChange={(e) =>
                      setCategoryCode(e.target.value.toUpperCase().replace(/\s+/g, '_'))
                    }
                    className="font-mono text-xs uppercase"
                    required
                  />
                  <p className="text-[11px] text-slate-400">
                    Mã định danh duy nhất trong toàn hệ thống.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700">
                    Tên hiển thị danh mục *
                  </label>
                  <Input
                    type="text"
                    placeholder="Ví dụ: Chi phí mua đá bi bổ sung"
                    value={categoryName}
                    onChange={(e) => setCategoryName(e.target.value)}
                    className="text-xs"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700">
                    Mô tả chi tiết mục đích chi
                  </label>
                  <Input
                    type="text"
                    placeholder="Ví dụ: Tiền mua đá bi bên ngoài cấp cứu giờ cao điểm do máy đá hỏng"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="text-xs"
                  />
                </div>

                <div className="pt-3 border-t border-slate-200 flex items-center justify-end gap-3">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setActiveTab('list')}
                    className="text-xs"
                  >
                    Xem danh sách
                  </Button>
                  <Button
                    type="submit"
                    disabled={isSubmitting || !categoryCode.trim() || !categoryName.trim()}
                    className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-5 flex items-center gap-2"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        <span>Đang lưu danh mục...</span>
                      </>
                    ) : (
                      <>
                        <Plus className="w-3.5 h-3.5" />
                        <span>Lưu danh mục chi phí mới</span>
                      </>
                    )}
                  </Button>
                </div>
              </form>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
