import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  FileText,
  ImageIcon,
  Upload,
  RefreshCw,
  Search,
  Eye,
  ShieldAlert,
  ShieldCheck,
  FolderOpen,
  Lock,
  Loader2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { Input } from '@/shared/components/ui/input'
import { evidenceApi } from '../evidenceApi'
import { EvidenceDropzone } from './EvidenceDropzone'
import { EvidencePreviewModal } from './EvidencePreviewModal'
import {
  formatBytes,
  truncateChecksum,
  isImageContentType,
  isPdfContentType,
} from '../utils'
import type { EvidenceFileDto } from '../types'
import type { BranchDto } from '@/features/organization/types'

export interface BranchEvidenceManagerModalProps {
  isOpen: boolean
  branch: BranchDto | null
  onClose: () => void
}

export const BranchEvidenceManagerModal: React.FC<BranchEvidenceManagerModalProps> = ({
  isOpen,
  branch,
  onClose,
}) => {
  const [evidenceList, setEvidenceList] = useState<EvidenceFileDto[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [activeTab, setActiveTab] = useState<'list' | 'upload'>('list')

  // Preview Modal state
  const [previewFile, setPreviewFile] = useState<EvidenceFileDto | null>(null)
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false)

  const loadEvidence = useCallback(async (branchId: string) => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await evidenceApi.listBranchEvidence(branchId)
      // Sort newest first
      data.sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      )
      setEvidenceList(data)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      const msg =
        apiErr.response?.data?.detail ||
        apiErr.response?.data?.message ||
        'Không thể tải danh sách chứng từ chi nhánh. Vui lòng kiểm tra quyền truy cập.'
      setError(msg)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (isOpen && branch?.id) {
      loadEvidence(branch.id)
      setActiveTab('list')
      setSearchQuery('')
    } else {
      setEvidenceList([])
      setError(null)
      setPreviewFile(null)
      setIsPreviewModalOpen(false)
    }
  }, [isOpen, branch?.id, loadEvidence])

  // Keyboard shortcut: ESC to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isPreviewModalOpen) {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, isPreviewModalOpen, onClose])

  const filteredEvidence = useMemo(() => {
    if (!searchQuery.trim()) return evidenceList
    const query = searchQuery.toLowerCase().trim()
    return evidenceList.filter((item) =>
      item.originalFilename.toLowerCase().includes(query)
    )
  }, [evidenceList, searchQuery])

  const handleUploadSuccess = (newFile: EvidenceFileDto) => {
    setEvidenceList((prev) => [newFile, ...prev.filter((f) => f.id !== newFile.id)])
    // Switch to list tab to see uploaded file
    setActiveTab('list')
  }

  const handleOpenPreview = (file: EvidenceFileDto) => {
    setPreviewFile(file)
    setIsPreviewModalOpen(true)
  }

  if (!isOpen || !branch) return null

  return (
    <>
      <AnimatePresence>
        <div className="fixed inset-0 z-40 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
          <motion.div
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.96 }}
            className="bg-stone-900 border border-stone-800 rounded-xl w-full max-w-4xl max-h-[90vh] shadow-2xl flex flex-col overflow-hidden text-stone-100"
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between p-5 border-b border-stone-800 bg-stone-950/70 shrink-0">
              <div className="flex items-center space-x-3">
                <div className="p-2.5 rounded-lg bg-emerald-950/50 border border-emerald-500/30 text-emerald-400">
                  <FolderOpen className="h-5 w-5" />
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <h3 className="text-base font-semibold text-stone-100">
                      Chứng từ Chi nhánh: {branch.name}
                    </h3>
                    <Badge variant="outline" className="font-mono text-emerald-400 border-emerald-500/30 bg-emerald-950/30 text-xs">
                      {branch.branchCode}
                    </Badge>
                  </div>
                  <p className="text-xs text-stone-400 mt-0.5">
                    Lưu trữ chứng từ pháp lý, hóa đơn, biên bản chi nhánh (Bảo mật thời hạn TTL 15 phút, SHA-256)
                  </p>
                </div>
              </div>

              <Button
                variant="ghost"
                size="sm"
                onClick={onClose}
                className="h-8 w-8 p-0 text-stone-400 hover:text-stone-200 hover:bg-stone-800 rounded-lg"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>

            {/* Navigation Tabs */}
            <div className="flex items-center justify-between px-5 pt-3 border-b border-stone-800/80 bg-stone-950/40 shrink-0">
              <div className="flex space-x-1">
                <button
                  onClick={() => setActiveTab('list')}
                  className={`px-4 py-2 text-xs font-medium border-b-2 transition-colors flex items-center space-x-2 ${
                    activeTab === 'list'
                      ? 'border-emerald-500 text-emerald-400'
                      : 'border-transparent text-stone-400 hover:text-stone-200 hover:border-stone-700'
                  }`}
                >
                  <FileText className="h-3.5 w-3.5" />
                  <span>Danh sách chứng từ ({evidenceList.length})</span>
                </button>
                <button
                  onClick={() => setActiveTab('upload')}
                  className={`px-4 py-2 text-xs font-medium border-b-2 transition-colors flex items-center space-x-2 ${
                    activeTab === 'upload'
                      ? 'border-emerald-500 text-emerald-400'
                      : 'border-transparent text-stone-400 hover:text-stone-200 hover:border-stone-700'
                  }`}
                >
                  <Upload className="h-3.5 w-3.5" />
                  <span>Tải lên chứng từ mới</span>
                </button>
              </div>

              {activeTab === 'list' && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => loadEvidence(branch.id)}
                  disabled={isLoading}
                  className="h-7 px-2 text-xs text-stone-400 hover:text-stone-200 mb-1"
                  title="Làm mới danh sách"
                >
                  <RefreshCw className={`h-3 w-3 mr-1 ${isLoading ? 'animate-spin' : ''}`} />
                  <span>Làm mới</span>
                </Button>
              )}
            </div>

            {/* Tab Contents */}
            <div className="p-5 flex-1 overflow-y-auto min-h-[300px]">
              {/* Tab 1: Upload Dropzone */}
              {activeTab === 'upload' && (
                <div className="space-y-4">
                  <div className="bg-stone-950/40 border border-stone-800/80 rounded-lg p-3 text-xs text-stone-400 flex items-start space-x-2">
                    <ShieldCheck className="h-4 w-4 text-emerald-400 shrink-0 mt-0.5" />
                    <p className="leading-relaxed">
                      Chứng từ được tải lên sẽ gắn liền với chi nhánh{' '}
                      <span className="font-semibold text-stone-200">{branch.name}</span> và tự động
                      tính toán mã băm SHA-256 để bảo đảm tính toàn vẹn dữ liệu. Quyền truy cập tuân thủ ranh giới bảo mật đa chi nhánh (TC-043).
                    </p>
                  </div>

                  <EvidenceDropzone
                    branchId={branch.id}
                    onUploadSuccess={handleUploadSuccess}
                  />
                </div>
              )}

              {/* Tab 2: Evidence List */}
              {activeTab === 'list' && (
                <div className="space-y-4">
                  {/* Search Bar */}
                  <div className="flex items-center space-x-2">
                    <div className="relative flex-1">
                      <Search className="h-3.5 w-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-stone-500" />
                      <Input
                        type="text"
                        placeholder="Tìm kiếm theo tên chứng từ..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="pl-9 h-8 text-xs bg-stone-950/60 border-stone-800 focus:border-stone-700"
                      />
                    </div>
                  </div>

                  {/* Error display */}
                  {error && (
                    <div className="p-3 text-xs bg-rose-950/40 border border-rose-500/40 rounded-lg text-rose-300 flex items-start space-x-2">
                      <ShieldAlert className="h-4 w-4 text-rose-400 shrink-0 mt-0.5" />
                      <span>{error}</span>
                    </div>
                  )}

                  {/* Loading */}
                  {isLoading && (
                    <div className="py-12 flex flex-col items-center justify-center space-y-2 text-stone-400">
                      <Loader2 className="h-6 w-6 animate-spin text-emerald-400" />
                      <span className="text-xs font-mono">Đang tải danh sách chứng từ...</span>
                    </div>
                  )}

                  {/* Empty state */}
                  {!isLoading && filteredEvidence.length === 0 && (
                    <div className="py-12 text-center space-y-3">
                      <div className="w-12 h-12 rounded-full bg-stone-800/80 border border-stone-700/60 flex items-center justify-center mx-auto text-stone-400">
                        <FolderOpen className="h-6 w-6" />
                      </div>
                      <h4 className="text-sm font-semibold text-stone-300">
                        {searchQuery ? 'Không tìm thấy chứng từ phù hợp' : 'Chưa có chứng từ nào'}
                      </h4>
                      <p className="text-xs text-stone-500 max-w-sm mx-auto">
                        {searchQuery
                          ? 'Thử thay đổi từ khóa tìm kiếm.'
                          : 'Chi nhánh này chưa có tệp chứng từ nào được tải lên.'}
                      </p>
                      {!searchQuery && (
                        <Button
                          size="sm"
                          onClick={() => setActiveTab('upload')}
                          className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs mt-2"
                        >
                          <Upload className="h-3.5 w-3.5 mr-1" />
                          <span>Tải chứng từ đầu tiên</span>
                        </Button>
                      )}
                    </div>
                  )}

                  {/* Table */}
                  {!isLoading && filteredEvidence.length > 0 && (
                    <div className="overflow-x-auto rounded-lg border border-stone-800 bg-stone-950/50">
                      <table className="w-full text-left text-xs">
                        <thead className="bg-stone-950 text-stone-400 font-mono border-b border-stone-800">
                          <tr>
                            <th className="py-2.5 px-4 font-medium">Tên chứng từ</th>
                            <th className="py-2.5 px-3 font-medium">Dung lượng</th>
                            <th className="py-2.5 px-3 font-medium">Mã SHA-256</th>
                            <th className="py-2.5 px-3 font-medium">Phân loại</th>
                            <th className="py-2.5 px-3 font-medium">Ngày tải lên</th>
                            <th className="py-2.5 px-4 font-medium text-right">Thao tác</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-stone-800/60 text-stone-200">
                          {filteredEvidence.map((file) => {
                            const isImg = isImageContentType(file.contentType)
                            const isPdf = isPdfContentType(file.contentType)
                            return (
                              <tr
                                key={file.id}
                                className="hover:bg-stone-800/40 transition-colors group"
                              >
                                <td className="py-2.5 px-4">
                                  <div className="flex items-center space-x-2.5 min-w-0">
                                    <div className="p-1.5 rounded bg-stone-900 border border-stone-800 shrink-0 text-stone-300">
                                      {isImg ? (
                                        <ImageIcon className="h-4 w-4 text-emerald-400" />
                                      ) : isPdf ? (
                                        <FileText className="h-4 w-4 text-red-400" />
                                      ) : (
                                        <FileText className="h-4 w-4 text-stone-400" />
                                      )}
                                    </div>
                                    <span
                                      className="font-medium text-stone-100 truncate max-w-xs"
                                      title={file.originalFilename}
                                    >
                                      {file.originalFilename}
                                    </span>
                                  </div>
                                </td>
                                <td className="py-2.5 px-3 font-mono text-[11px] text-stone-400 whitespace-nowrap">
                                  {formatBytes(file.byteSize)}
                                </td>
                                <td className="py-2.5 px-3 font-mono text-[11px] text-stone-400 whitespace-nowrap">
                                  <span
                                    className="bg-stone-900 px-1 py-0.5 rounded border border-stone-800/80 text-stone-300 select-all"
                                    title={file.checksumSha256}
                                  >
                                    {truncateChecksum(file.checksumSha256, 6, 6)}
                                  </span>
                                </td>
                                <td className="py-2.5 px-3 whitespace-nowrap">
                                  <Badge
                                    variant="outline"
                                    className="border-stone-700 text-stone-300 text-[10px] font-mono"
                                  >
                                    {file.visibilityClass}
                                  </Badge>
                                </td>
                                <td className="py-2.5 px-3 font-mono text-[11px] text-stone-400 whitespace-nowrap">
                                  {new Date(file.createdAt).toLocaleDateString('vi-VN')}
                                </td>
                                <td className="py-2.5 px-4 text-right whitespace-nowrap">
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => handleOpenPreview(file)}
                                    className="h-7 px-2.5 text-xs text-emerald-400 hover:text-emerald-300 hover:bg-emerald-950/50 inline-flex items-center space-x-1"
                                    title="Xem trước an toàn bằng Token tạm thời"
                                  >
                                    <Eye className="h-3.5 w-3.5 mr-1" />
                                    <span>Xem trước</span>
                                  </Button>
                                </td>
                              </tr>
                            )
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Footer */}
            <div className="flex items-center justify-between p-4 border-t border-stone-800 bg-stone-950/60 shrink-0 text-xs text-stone-400">
              <span className="flex items-center space-x-1 text-[11px]">
                <Lock className="h-3.5 w-3.5 text-emerald-400" />
                <span>Bảo mật chứng từ theo chi nhánh & hạn chế thời lượng truy cập</span>
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={onClose}
                className="h-8 border-stone-700 text-stone-300 hover:bg-stone-800 text-xs"
              >
                Đóng
              </Button>
            </div>
          </motion.div>
        </div>
      </AnimatePresence>

      {/* Embedded Secure Preview Modal */}
      <EvidencePreviewModal
        isOpen={isPreviewModalOpen}
        file={previewFile}
        onClose={() => {
          setIsPreviewModalOpen(false)
          setPreviewFile(null)
        }}
      />
    </>
  )
}
export default BranchEvidenceManagerModal
