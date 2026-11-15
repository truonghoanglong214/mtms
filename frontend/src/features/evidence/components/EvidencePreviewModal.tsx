import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Download,
  ZoomIn,
  ZoomOut,
  RotateCw,
  RefreshCw,
  Copy,
  Check,
  FileText,
  ImageIcon,
  Shield,
  Clock,
  HardDrive,
  Hash,
  ExternalLink,
  AlertTriangle,
  Loader2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { evidenceApi } from '../evidenceApi'
import {
  formatBytes,
  truncateChecksum,
  isImageContentType,
  isPdfContentType,
} from '../utils'
import type { EvidenceFileDto, DownloadTokenResponse } from '../types'

export interface EvidencePreviewModalProps {
  isOpen: boolean
  file: EvidenceFileDto | null
  onClose: () => void
}

export const EvidencePreviewModal: React.FC<EvidencePreviewModalProps> = ({
  isOpen,
  file,
  onClose,
}) => {
  const [tokenData, setTokenData] = useState<DownloadTokenResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Image manipulation states
  const [scale, setScale] = useState<number>(1)
  const [rotation, setRotation] = useState<number>(0)
  const [copiedChecksum, setCopiedChecksum] = useState(false)

  // Fetch token when opened and file is provided
  const fetchToken = useCallback(async (fileId: string) => {
    setIsLoading(true)
    setError(null)
    setScale(1)
    setRotation(0)
    try {
      const data = await evidenceApi.getEvidenceDownloadToken(fileId)
      setTokenData(data)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      const msg =
        apiErr.response?.data?.detail ||
        apiErr.response?.data?.message ||
        'Không thể lấy mã truy cập an toàn (Token). Có thể bạn không có quyền trên chi nhánh này hoặc phiên đăng nhập đã hết hạn.'
      setError(msg)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (isOpen && file?.id) {
      fetchToken(file.id)
    } else {
      setTokenData(null)
      setError(null)
      setScale(1)
      setRotation(0)
    }
  }, [isOpen, file?.id, fetchToken])

  // Keyboard shortcut: ESC to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    setCopiedChecksum(true)
    setTimeout(() => setCopiedChecksum(false), 2000)
  }

  const handleZoomIn = () => setScale((s) => Math.min(3, s + 0.25))
  const handleZoomOut = () => setScale((s) => Math.max(0.5, s - 0.25))
  const handleResetZoom = () => {
    setScale(1)
    setRotation(0)
  }
  const handleRotate = () => setRotation((r) => (r + 90) % 360)

  if (!isOpen || !file) return null

  const isImage = isImageContentType(file.contentType)
  const isPdf = isPdfContentType(file.contentType)
  const resolvedContentUrl =
    tokenData?.downloadUrl ||
    (tokenData?.token ? evidenceApi.getEvidenceContentUrl(file.id, tokenData.token) : null)

  const handleDownload = () => {
    if (!resolvedContentUrl) return
    const a = document.createElement('a')
    a.href = resolvedContentUrl
    a.download = file.originalFilename
    a.target = '_blank'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="bg-stone-900 border border-stone-800 rounded-xl w-full max-w-5xl h-[90vh] shadow-2xl flex flex-col overflow-hidden text-stone-100"
        >
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-stone-800 bg-stone-950/70 shrink-0">
            <div className="flex items-center space-x-3 min-w-0">
              <div className="p-2 rounded-lg bg-stone-800/80 border border-stone-700/80 text-emerald-400 shrink-0">
                {isImage ? (
                  <ImageIcon className="h-5 w-5" />
                ) : (
                  <FileText className="h-5 w-5 text-red-400" />
                )}
              </div>
              <div className="min-w-0">
                <div className="flex items-center space-x-2">
                  <h3
                    className="text-sm font-semibold text-stone-100 truncate max-w-md sm:max-w-xl"
                    title={file.originalFilename}
                  >
                    {file.originalFilename}
                  </h3>
                  <Badge
                    variant="outline"
                    className="border-stone-700 text-stone-300 text-[10px] uppercase font-mono"
                  >
                    {isImage ? 'ẢNH' : isPdf ? 'PDF' : 'TÀI LIỆU'}
                  </Badge>
                  <Badge
                    variant="outline"
                    className="border-emerald-500/30 text-emerald-400 bg-emerald-950/30 text-[10px] font-mono"
                  >
                    {file.visibilityClass}
                  </Badge>
                </div>
                <div className="flex items-center space-x-3 text-[11px] text-stone-400 font-mono mt-0.5">
                  <span className="flex items-center space-x-1">
                    <HardDrive className="h-3 w-3 text-stone-500" />
                    <span>{formatBytes(file.byteSize)}</span>
                  </span>
                  <span>•</span>
                  <span className="flex items-center space-x-1">
                    <Clock className="h-3 w-3 text-stone-500" />
                    <span>{new Date(file.createdAt).toLocaleString('vi-VN')}</span>
                  </span>
                </div>
              </div>
            </div>

            <div className="flex items-center space-x-2 shrink-0">
              {resolvedContentUrl && (
                <Button
                  size="sm"
                  onClick={handleDownload}
                  className="h-8 bg-emerald-600 hover:bg-emerald-500 text-white text-xs inline-flex items-center space-x-1.5 shadow-sm"
                  title="Tải tệp tin về máy tính"
                >
                  <Download className="h-3.5 w-3.5 mr-1" />
                  <span>Tải file xuống</span>
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={onClose}
                className="h-8 w-8 p-0 text-stone-400 hover:text-stone-200 hover:bg-stone-800 rounded-lg"
                title="Đóng cửa sổ (ESC)"
              >
                <X className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {/* Main Content Area */}
          <div className="flex-1 bg-stone-950/90 relative overflow-hidden flex flex-col items-center justify-center p-4">
            {isLoading && (
              <div className="flex flex-col items-center justify-center space-y-3 text-stone-400">
                <Loader2 className="h-8 w-8 animate-spin text-emerald-400" />
                <p className="text-xs font-mono">Đang cấp Token bảo mật tạm thời (TTL 15 phút)...</p>
              </div>
            )}

            {!isLoading && error && (
              <div className="p-6 max-w-md bg-stone-900 border border-rose-500/40 rounded-xl text-center space-y-3">
                <AlertTriangle className="h-10 w-10 text-rose-400 mx-auto" />
                <h4 className="text-sm font-semibold text-rose-300">Không thể xem trước tệp</h4>
                <p className="text-xs text-stone-400 leading-relaxed">{error}</p>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fetchToken(file.id)}
                  className="border-stone-700 text-stone-200 hover:bg-stone-800 text-xs inline-flex items-center space-x-1 mt-2"
                >
                  <RefreshCw className="h-3.5 w-3.5 mr-1" />
                  <span>Thử lại</span>
                </Button>
              </div>
            )}

            {!isLoading && !error && resolvedContentUrl && (
              <>
                {/* Image Viewer */}
                {isImage && (
                  <div className="w-full h-full flex flex-col items-center justify-center relative select-none">
                    {/* Floating Controls */}
                    <div className="absolute top-2 z-10 flex items-center space-x-1 bg-stone-900/90 border border-stone-700/80 rounded-lg px-2 py-1 shadow-lg backdrop-blur-md">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={handleZoomIn}
                        className="h-7 w-7 p-0 text-stone-300 hover:text-white hover:bg-stone-800"
                        title="Phóng to"
                      >
                        <ZoomIn className="h-3.5 w-3.5" />
                      </Button>
                      <span className="text-[11px] font-mono text-stone-300 px-1">
                        {Math.round(scale * 100)}%
                      </span>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={handleZoomOut}
                        className="h-7 w-7 p-0 text-stone-300 hover:text-white hover:bg-stone-800"
                        title="Thu nhỏ"
                      >
                        <ZoomOut className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={handleRotate}
                        className="h-7 w-7 p-0 text-stone-300 hover:text-white hover:bg-stone-800"
                        title="Xoay 90 độ"
                      >
                        <RotateCw className="h-3.5 w-3.5" />
                      </Button>
                      {(scale !== 1 || rotation !== 0) && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={handleResetZoom}
                          className="h-7 px-2 text-[11px] text-emerald-400 hover:text-emerald-300 hover:bg-stone-800 font-mono"
                          title="Đặt lại kích thước gốc"
                        >
                          Reset
                        </Button>
                      )}
                    </div>

                    <div className="w-full h-full flex items-center justify-center overflow-auto p-4">
                      <img
                        src={resolvedContentUrl}
                        alt={file.originalFilename}
                        style={{
                          transform: `scale(${scale}) rotate(${rotation}deg)`,
                          transition: 'transform 0.15s ease-out',
                        }}
                        className="max-h-full max-w-full object-contain rounded shadow-lg"
                      />
                    </div>
                  </div>
                )}

                {/* PDF Viewer */}
                {isPdf && (
                  <div className="w-full h-full flex flex-col">
                    <iframe
                      src={resolvedContentUrl}
                      title={file.originalFilename}
                      className="w-full flex-1 rounded border border-stone-800 bg-stone-900"
                    />
                    <div className="pt-2 flex items-center justify-between text-xs text-stone-400 px-1">
                      <span>
                        Nếu trình duyệt không tự mở được PDF, bạn có thể tải file hoặc mở tab mới.
                      </span>
                      <a
                        href={resolvedContentUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center space-x-1 text-emerald-400 hover:underline"
                      >
                        <span>Mở trong tab mới</span>
                        <ExternalLink className="h-3 w-3" />
                      </a>
                    </div>
                  </div>
                )}

                {/* Other File Types Fallback */}
                {!isImage && !isPdf && (
                  <div className="p-8 text-center space-y-3">
                    <FileText className="h-12 w-12 text-stone-400 mx-auto" />
                    <p className="text-sm text-stone-300">
                      Tệp này không hỗ trợ xem trực tiếp trong trình duyệt.
                    </p>
                    <Button
                      size="sm"
                      onClick={handleDownload}
                      className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs"
                    >
                      <Download className="h-3.5 w-3.5 mr-1" />
                      Tải về để xem
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Footer Metadata & Security Status */}
          <div className="p-3 border-t border-stone-800 bg-stone-950/80 shrink-0 flex flex-wrap items-center justify-between gap-3 text-xs text-stone-400">
            <div className="flex items-center space-x-2 font-mono text-[11px]">
              <span className="flex items-center space-x-1 text-stone-400">
                <Hash className="h-3 w-3 text-stone-500" />
                <span>SHA-256:</span>
              </span>
              <span
                className="text-stone-300 bg-stone-900 px-1.5 py-0.5 rounded border border-stone-800 select-all"
                title={file.checksumSha256}
              >
                {truncateChecksum(file.checksumSha256, 12, 12)}
              </span>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => copyToClipboard(file.checksumSha256)}
                className="h-6 w-6 p-0 text-stone-400 hover:text-stone-200"
                title="Sao chép toàn bộ mã băm SHA-256"
              >
                {copiedChecksum ? (
                  <Check className="h-3.5 w-3.5 text-emerald-400" />
                ) : (
                  <Copy className="h-3.5 w-3.5" />
                )}
              </Button>
            </div>

            <div className="flex items-center space-x-3 text-[11px] font-mono">
              <span className="flex items-center space-x-1 text-emerald-400">
                <Shield className="h-3 w-3" />
                <span>HMAC-SHA256 Token (Hết hạn sau 15 phút)</span>
              </span>
              {tokenData?.expiresAt && (
                <span className="text-stone-500">
                  Hạn dùng: {new Date(tokenData.expiresAt).toLocaleTimeString('vi-VN')}
                </span>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
export default EvidencePreviewModal
