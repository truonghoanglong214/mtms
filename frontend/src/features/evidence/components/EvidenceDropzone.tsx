import React, { useState, useRef, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  UploadCloud,
  FileText,
  ImageIcon,
  X,
  AlertCircle,
  CheckCircle2,
  Loader2,
  Lock,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { evidenceApi } from '../evidenceApi'
import {
  formatBytes,
  validateEvidenceFile,
  ALLOWED_FILE_EXTENSIONS,
} from '../utils'
import type { EvidenceFileDto, VisibilityClass } from '../types'

export interface EvidenceDropzoneProps {
  branchId: string
  onUploadSuccess?: (file: EvidenceFileDto) => void
  defaultVisibility?: VisibilityClass
  disabled?: boolean
  className?: string
}

export const EvidenceDropzone: React.FC<EvidenceDropzoneProps> = ({
  branchId,
  onUploadSuccess,
  defaultVisibility = 'BRANCH_PRIVATE',
  disabled = false,
  className = '',
}) => {
  const [isDragging, setIsDragging] = useState(false)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [visibility, setVisibility] = useState<VisibilityClass>(defaultVisibility)
  const [uploadProgress, setUploadProgress] = useState<number>(0)
  const [isUploading, setIsUploading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // Clean up object URL on unmount or file change to prevent memory leaks
  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    }
  }, [previewUrl])

  const clearSelection = useCallback(() => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl)
      setPreviewUrl(null)
    }
    setSelectedFile(null)
    setErrorMessage(null)
    setUploadProgress(0)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }, [previewUrl])

  const handleFile = useCallback(
    (file: File) => {
      setErrorMessage(null)
      setSuccessToast(null)

      const validation = validateEvidenceFile(file)
      if (!validation.valid) {
        setErrorMessage(validation.error || 'Tệp không hợp lệ.')
        return
      }

      // If valid, revoke previous preview if any
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
        setPreviewUrl(null)
      }

      setSelectedFile(file)

      // Create preview for image files
      if (file.type.startsWith('image/')) {
        const url = URL.createObjectURL(file)
        setPreviewUrl(url)
      }
    },
    [previewUrl]
  )

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (!disabled && !isUploading) {
      setIsDragging(true)
    }
  }

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
    if (disabled || isUploading) return

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFile(e.dataTransfer.files[0])
    }
  }

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleFile(e.target.files[0])
    }
  }

  const handleUpload = async () => {
    if (!selectedFile || !branchId || isUploading) return

    setIsUploading(true)
    setUploadProgress(0)
    setErrorMessage(null)
    setSuccessToast(null)

    try {
      const uploaded = await evidenceApi.uploadEvidence(
        branchId,
        selectedFile,
        visibility,
        (percent) => {
          setUploadProgress(percent)
        }
      )

      setSuccessToast(`Đã tải lên thành công: ${uploaded.originalFilename}`)
      clearSelection()
      if (onUploadSuccess) {
        onUploadSuccess(uploaded)
      }

      // Auto dismiss success toast after 4s
      setTimeout(() => {
        setSuccessToast(null)
      }, 4000)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      const msg =
        apiErr.response?.data?.detail ||
        apiErr.response?.data?.message ||
        'Không thể tải lên chứng từ. Vui lòng kiểm tra lại quyền truy cập hoặc dung lượng tệp.'
      setErrorMessage(msg)
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <div className={`space-y-3 ${className}`}>
      {/* Toast Notification */}
      <AnimatePresence>
        {successToast && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="flex items-center space-x-2 p-3 text-xs bg-emerald-950/70 border border-emerald-500/50 rounded-lg text-emerald-300 shadow-sm"
          >
            <CheckCircle2 className="h-4 w-4 text-emerald-400 shrink-0" />
            <span className="flex-1 font-medium">{successToast}</span>
            <button
              onClick={() => setSuccessToast(null)}
              className="text-emerald-400 hover:text-emerald-200"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Error Alert */}
      <AnimatePresence>
        {errorMessage && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="flex items-start space-x-2 p-3 text-xs bg-rose-950/60 border border-rose-500/40 rounded-lg text-rose-300 shadow-sm"
          >
            <AlertCircle className="h-4 w-4 text-rose-400 shrink-0 mt-0.5" />
            <span className="flex-1">{errorMessage}</span>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-rose-400 hover:text-rose-200"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Drop Area */}
      {!selectedFile ? (
        <div
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => !disabled && fileInputRef.current?.click()}
          className={`relative border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center ${
            disabled
              ? 'opacity-50 cursor-not-allowed border-stone-800 bg-stone-900/30'
              : isDragging
              ? 'border-emerald-500 bg-emerald-950/30 scale-[1.008] shadow-lg shadow-emerald-950/50'
              : 'border-stone-800 hover:border-stone-700 bg-stone-950/40 hover:bg-stone-900/40'
          }`}
        >
          <input
            ref={fileInputRef}
            type="file"
            className="hidden"
            accept={ALLOWED_FILE_EXTENSIONS.join(',')}
            onChange={handleFileInputChange}
            disabled={disabled || isUploading}
          />

          <div className="w-12 h-12 rounded-full bg-stone-900 border border-stone-800 flex items-center justify-center mb-3 shadow-inner text-emerald-400">
            <UploadCloud className="h-6 w-6" />
          </div>

          <h4 className="text-sm font-semibold text-stone-200 mb-1">
            Kéo thả tệp chứng từ vào đây hoặc{' '}
            <span className="text-emerald-400 underline decoration-emerald-500/50 underline-offset-2 hover:text-emerald-300">
              chọn từ máy tính
            </span>
          </h4>
          <p className="text-xs text-stone-400 max-w-sm mb-2">
            Định dạng hỗ trợ: <span className="font-mono text-stone-300">JPG, PNG, WebP, PDF</span> (tối đa 5 MB)
          </p>
          <div className="flex items-center space-x-1.5 text-[11px] text-stone-500 font-mono">
            <Lock className="h-3 w-3 text-stone-500" />
            <span>Tệp được lưu trữ riêng tư, mã hóa SHA-256 & phân quyền theo chi nhánh</span>
          </div>
        </div>
      ) : (
        /* Selected File Card & Preview */
        <motion.div
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          className="border border-stone-800 bg-stone-950/60 rounded-xl p-4 space-y-4"
        >
          <div className="flex items-start justify-between space-x-3">
            <div className="flex items-start space-x-3 min-w-0">
              {/* Preview Thumbnail or File Icon */}
              {previewUrl ? (
                <div className="w-16 h-16 rounded-lg overflow-hidden border border-stone-700 bg-stone-900 shrink-0 relative group">
                  <img
                    src={previewUrl}
                    alt="Preview"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <ImageIcon className="h-4 w-4 text-white" />
                  </div>
                </div>
              ) : (
                <div className="w-16 h-16 rounded-lg border border-red-900/60 bg-red-950/20 text-red-400 flex flex-col items-center justify-center shrink-0">
                  <FileText className="h-6 w-6" />
                  <span className="text-[10px] font-mono font-bold mt-1">PDF</span>
                </div>
              )}

              <div className="min-w-0 flex-1 space-y-1">
                <p className="text-xs font-semibold text-stone-200 truncate" title={selectedFile.name}>
                  {selectedFile.name}
                </p>
                <div className="flex items-center space-x-2 text-[11px] text-stone-400 font-mono">
                  <span>{formatBytes(selectedFile.size)}</span>
                  <span>•</span>
                  <span>{selectedFile.type || 'document'}</span>
                </div>
                <div className="flex items-center space-x-2 pt-1">
                  <Badge variant="outline" className="text-[10px] border-stone-700 text-stone-400">
                    Sẵn sàng tải lên
                  </Badge>
                </div>
              </div>
            </div>

            {/* Cancel Button */}
            {!isUploading && (
              <Button
                variant="ghost"
                size="sm"
                onClick={clearSelection}
                className="h-7 w-7 p-0 text-stone-400 hover:text-stone-200 hover:bg-stone-800 rounded-lg"
                title="Hủy chọn tệp"
              >
                <X className="h-4 w-4" />
              </Button>
            )}
          </div>

          {/* Visibility Class Selector */}
          <div className="pt-2 border-t border-stone-800/80 flex flex-wrap items-center justify-between gap-2 text-xs">
            <label className="text-stone-400 flex items-center space-x-1.5 text-[11px]">
              <Lock className="h-3 w-3 text-stone-400" />
              <span>Phân loại quyền truy cập:</span>
            </label>
            <select
              value={visibility}
              onChange={(e) => setVisibility(e.target.value as VisibilityClass)}
              disabled={isUploading}
              className="bg-stone-900 border border-stone-700 rounded-md px-2.5 py-1 text-xs text-stone-200 focus:outline-none focus:border-emerald-500"
            >
              <option value="BRANCH_PRIVATE">Nội bộ Chi nhánh (BRANCH_PRIVATE)</option>
              <option value="ENTITY_PRIVATE">Pháp nhân quản lý (ENTITY_PRIVATE)</option>
              <option value="GLOBAL_ADMIN_ONLY">Chỉ Quản trị viên (GLOBAL_ADMIN_ONLY)</option>
            </select>
          </div>

          {/* Progress Bar */}
          {isUploading && (
            <div className="space-y-1.5 pt-1">
              <div className="flex justify-between text-[11px] text-stone-400 font-mono">
                <span>Đang tải lên và tính toán mã băm SHA-256...</span>
                <span>{uploadProgress}%</span>
              </div>
              <div className="h-1.5 w-full bg-stone-800 rounded-full overflow-hidden">
                <motion.div
                  className="h-full bg-emerald-500 rounded-full"
                  initial={{ width: 0 }}
                  animate={{ width: `${uploadProgress}%` }}
                  transition={{ ease: 'easeOut', duration: 0.2 }}
                />
              </div>
            </div>
          )}

          {/* Action Bar */}
          <div className="flex items-center justify-end space-x-2 pt-1">
            <Button
              variant="outline"
              size="sm"
              onClick={clearSelection}
              disabled={isUploading}
              className="border-stone-700 text-stone-300 hover:bg-stone-800 text-xs h-8"
            >
              Chọn tệp khác
            </Button>
            <Button
              size="sm"
              onClick={handleUpload}
              disabled={isUploading}
              className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs h-8 font-medium shadow-sm flex items-center space-x-1.5"
            >
              {isUploading ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin mr-1" />
                  <span>Đang tải...</span>
                </>
              ) : (
                <>
                  <UploadCloud className="h-3.5 w-3.5 mr-1" />
                  <span>Bắt đầu tải lên</span>
                </>
              )}
            </Button>
          </div>
        </motion.div>
      )}
    </div>
  )
}
export default EvidenceDropzone
