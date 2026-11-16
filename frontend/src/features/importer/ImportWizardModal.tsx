import React, { useState, useRef, useMemo, useEffect } from 'react'
import { motion } from 'motion/react'
import {
  X,
  FileSpreadsheet,
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  ChevronRight,
  ChevronLeft,
  Download,
  RotateCcw,
  Check,
  ShieldCheck,
  Layers,
  Package,
  Truck,
  Database,
  Loader2,
  Store,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { importApi } from './importApi'
import { ImportRowErrorReport } from './ImportRowErrorReport'
import {
  type ImportBatch,
  type ImportRow,
  type ImportKind,
  IMPORT_KIND_CONFIGS,
} from './types'
import type { BranchDto } from '@/features/organization/types'

interface ImportWizardModalProps {
  isOpen: boolean
  branch: BranchDto | null
  branches?: BranchDto[]
  onClose: () => void
  onSuccess?: (batch: ImportBatch) => void
}

type WizardStep = 1 | 2 | 3

export const ImportWizardModal: React.FC<ImportWizardModalProps> = ({
  isOpen,
  branch,
  branches = [],
  onClose,
  onSuccess,
}) => {
  // Wizard state
  const [currentStep, setCurrentStep] = useState<WizardStep>(1)
  const [selectedBranchId, setSelectedBranchId] = useState<string>(branch?.id || '')
  const [selectedKind, setSelectedKind] = useState<ImportKind>('OPENING_INVENTORY')

  // File upload state
  const [file, setFile] = useState<File | null>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [uploadProgress, setUploadProgress] = useState<number | null>(null)
  const [isStaging, setIsStaging] = useState(false)
  const [stageError, setStageError] = useState<string | null>(null)

  // Staged batch state
  const [batch, setBatch] = useState<ImportBatch | null>(null)
  const [errorRows, setErrorRows] = useState<ImportRow[]>([])
  const [validRows, setValidRows] = useState<ImportRow[]>([])
  const [isLoadingRows, setIsLoadingRows] = useState(false)

  // Commit state
  const [isCommitting, setIsCommitting] = useState(false)
  const [commitError, setCommitError] = useState<string | null>(null)

  const fileInputRef = useRef<HTMLInputElement>(null)

  // Sync selected branch when prop changes
  useEffect(() => {
    if (branch?.id) {
      setSelectedBranchId(branch.id)
    } else if (branches.length > 0 && !selectedBranchId) {
      setSelectedBranchId(branches[0].id)
    }
  }, [branch, branches, selectedBranchId])

  // Reset modal on open/close
  const handleReset = () => {
    setCurrentStep(1)
    setFile(null)
    setUploadProgress(null)
    setIsStaging(false)
    setStageError(null)
    setBatch(null)
    setErrorRows([])
    setValidRows([])
    setIsLoadingRows(false)
    setIsCommitting(false)
    setCommitError(null)
  }

  const handleClose = () => {
    if (isStaging || isCommitting) return // Prevent accidental close during in-flight network mutation
    handleReset()
    onClose()
  }

  const activeBranch = useMemo(() => {
    if (branch && branch.id === selectedBranchId) return branch
    return branches.find((b) => b.id === selectedBranchId) || branch
  }, [branch, branches, selectedBranchId])

  const kindConfig = IMPORT_KIND_CONFIGS[selectedKind]

  // File drag & drop handlers
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
  }

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }

  const validateAndSetFile = (selectedFile: File) => {
    setStageError(null)
    const validExtensions = ['.xlsx', '.xls', '.csv']
    const hasValidExt = validExtensions.some((ext) =>
      selectedFile.name.toLowerCase().endsWith(ext)
    )

    if (!hasValidExt) {
      setStageError('Chỉ hỗ trợ định dạng bảng tính Excel (.xlsx, .xls) hoặc file CSV (.csv).')
      return
    }

    // 10MB limit
    if (selectedFile.size > 10 * 1024 * 1024) {
      setStageError('Kích thước tệp vượt quá giới hạn 10MB cho phép.')
      return
    }

    setFile(selectedFile)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      validateAndSetFile(e.dataTransfer.files[0])
    }
  }

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndSetFile(e.target.files[0])
    }
  }

  // Template download in browser (UTF-8 BOM CSV)
  const handleDownloadTemplate = () => {
    const config = IMPORT_KIND_CONFIGS[selectedKind]
    const headerRow = config.columns.map((c) => c.key).join(',')
    const sampleRow = config.columns.map((c) => `"${c.example}"`).join(',')
    const csvContent = '\uFEFF' + headerRow + '\n' + sampleRow + '\n'

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `mau_nhap_lieu_${selectedKind.toLowerCase()}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  // Step 1 -> Step 2: Upload file and stage
  const handleStageFile = async () => {
    if (!selectedBranchId) {
      setStageError('Vui lòng chọn chi nhánh đích để nhập dữ liệu.')
      return
    }
    if (!file) {
      setStageError('Vui lòng chọn hoặc kéo thả tệp Excel/CSV.')
      return
    }

    setIsStaging(true)
    setStageError(null)
    setUploadProgress(0)

    try {
      const stagedBatch = await importApi.stageImportFile(
        selectedBranchId,
        selectedKind,
        file,
        (percent) => setUploadProgress(percent)
      )

      setBatch(stagedBatch)
      setCurrentStep(2)

      // Fetch row details based on batch status
      setIsLoadingRows(true)
      try {
        if (stagedBatch.errorRows > 0) {
          const errs = await importApi.getImportRows(selectedBranchId, stagedBatch.id, 'ERROR')
          setErrorRows(errs)
        } else {
          const valids = await importApi.getImportRows(selectedBranchId, stagedBatch.id, 'VALID')
          setValidRows(valids)
        }
      } catch (rowFetchErr: any) {
        console.warn('Could not fetch row details:', rowFetchErr)
      } finally {
        setIsLoadingRows(false)
      }
    } catch (err: any) {
      const detail =
        err.response?.data?.detail ||
        err.response?.data?.message ||
        'Không thể xử lý và phân tích tệp nhập liệu. Vui lòng kiểm tra định dạng dữ liệu.'
      setStageError(detail)
    } finally {
      setIsStaging(false)
      setUploadProgress(null)
    }
  }

  // Step 3: Commit the batch
  const handleCommit = async () => {
    if (!batch || !selectedBranchId) return
    if (batch.errorRows > 0) {
      setCommitError('Không thể cam kết tệp chứa dữ liệu lỗi.')
      return
    }

    setIsCommitting(true)
    setCommitError(null)

    try {
      const committedBatch = await importApi.commitImportBatch(selectedBranchId, batch.id)
      setBatch(committedBatch)
      if (onSuccess) {
        onSuccess(committedBatch)
      }
    } catch (err: any) {
      const detail =
        err.response?.data?.detail ||
        err.response?.data?.message ||
        'Không thể cam kết dữ liệu. Vui lòng thử lại.'
      setCommitError(detail)
    } finally {
      setIsCommitting(false)
    }
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm overflow-y-auto">
      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.96 }}
        transition={{ duration: 0.15 }}
        className="relative w-full max-w-4xl rounded-2xl border border-stone-800 bg-stone-900 shadow-2xl overflow-hidden flex flex-col my-8 max-h-[90vh]"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-stone-800 px-6 py-4 bg-stone-950/70">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-950/80 border border-emerald-500/30 text-emerald-400">
              <FileSpreadsheet className="h-5 w-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 className="text-base font-bold text-stone-100">
                  Trình hướng dẫn nhập liệu Excel / CSV
                </h2>
                <Badge
                  variant="outline"
                  className="border-emerald-500/30 text-emerald-400 bg-emerald-950/50 text-[10px] font-mono"
                >
                  BR-SEC-02
                </Badge>
              </div>
              <p className="text-xs text-stone-400 mt-0.5">
                Quy trình 3 bước: Tải lên staging → Kiểm tra hợp lệ (TC-045) → Cam kết an toàn (Idempotent Commit)
              </p>
            </div>
          </div>

          <button
            onClick={handleClose}
            disabled={isStaging || isCommitting}
            className="rounded-lg p-1.5 text-stone-400 hover:text-stone-200 hover:bg-stone-800 transition-colors disabled:opacity-40"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Wizard Stepper Progress Bar */}
        <div className="bg-stone-950/40 border-b border-stone-800 px-6 py-3">
          <div className="flex items-center justify-between max-w-2xl mx-auto">
            {/* Step 1 indicator */}
            <div className="flex items-center space-x-2">
              <div
                className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-mono font-bold transition-colors ${
                  currentStep === 1
                    ? 'bg-emerald-500 text-stone-950 ring-4 ring-emerald-500/20'
                    : currentStep > 1
                    ? 'bg-emerald-950 border border-emerald-500/40 text-emerald-400'
                    : 'bg-stone-800 text-stone-400'
                }`}
              >
                {currentStep > 1 ? <Check className="h-4 w-4" /> : '1'}
              </div>
              <span
                className={`text-xs font-medium ${
                  currentStep === 1 ? 'text-emerald-400 font-semibold' : 'text-stone-400'
                }`}
              >
                1. Chọn & Tải lên
              </span>
            </div>

            <div className="h-0.5 flex-1 mx-3 bg-stone-800">
              <div
                className={`h-full bg-emerald-500 transition-all duration-300 ${
                  currentStep === 1 ? 'w-0' : currentStep === 2 ? 'w-1/2' : 'w-full'
                }`}
              />
            </div>

            {/* Step 2 indicator */}
            <div className="flex items-center space-x-2">
              <div
                className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-mono font-bold transition-colors ${
                  currentStep === 2
                    ? 'bg-emerald-500 text-stone-950 ring-4 ring-emerald-500/20'
                    : currentStep > 2
                    ? 'bg-emerald-950 border border-emerald-500/40 text-emerald-400'
                    : 'bg-stone-800 text-stone-400'
                }`}
              >
                {currentStep > 2 ? <Check className="h-4 w-4" /> : '2'}
              </div>
              <span
                className={`text-xs font-medium ${
                  currentStep === 2 ? 'text-emerald-400 font-semibold' : 'text-stone-400'
                }`}
              >
                2. Kiểm tra & Xem trước
              </span>
            </div>

            <div className="h-0.5 flex-1 mx-3 bg-stone-800">
              <div
                className={`h-full bg-emerald-500 transition-all duration-300 ${
                  currentStep === 3 ? 'w-full' : 'w-0'
                }`}
              />
            </div>

            {/* Step 3 indicator */}
            <div className="flex items-center space-x-2">
              <div
                className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-mono font-bold transition-colors ${
                  currentStep === 3
                    ? batch?.status === 'COMMITTED'
                      ? 'bg-emerald-500 text-stone-950'
                      : 'bg-amber-500 text-stone-950 ring-4 ring-amber-500/20'
                    : 'bg-stone-800 text-stone-400'
                }`}
              >
                {batch?.status === 'COMMITTED' ? <Check className="h-4 w-4" /> : '3'}
              </div>
              <span
                className={`text-xs font-medium ${
                  currentStep === 3 ? 'text-amber-400 font-semibold' : 'text-stone-400'
                }`}
              >
                3. Cam kết nhập liệu
              </span>
            </div>
          </div>
        </div>

        {/* Modal Body - Scrollable */}
        <div className="p-6 space-y-6 overflow-y-auto flex-1">
          {/* Branch Information Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3 rounded-xl border border-stone-800 bg-stone-950/60 text-xs">
            <div className="flex items-center space-x-2.5">
              <Store className="h-4 w-4 text-emerald-400 shrink-0" />
              <span className="text-stone-400">Chi nhánh đích:</span>
              {branches.length > 1 && !branch ? (
                <select
                  value={selectedBranchId}
                  onChange={(e) => setSelectedBranchId(e.target.value)}
                  disabled={currentStep > 1}
                  className="rounded border border-stone-800 bg-stone-900 px-2.5 py-1 text-xs text-stone-100 focus:outline-none focus:border-emerald-500 font-semibold"
                >
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      [{b.branchCode}] {b.name}
                    </option>
                  ))}
                </select>
              ) : (
                <div className="flex items-center space-x-2">
                  <span className="font-mono font-bold text-emerald-400 bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-500/30">
                    {activeBranch?.branchCode || 'N/A'}
                  </span>
                  <span className="font-semibold text-stone-200">
                    {activeBranch?.name || 'Chi nhánh hiện tại'}
                  </span>
                </div>
              )}
            </div>

            <div className="flex items-center space-x-2 text-stone-500 text-[11px] font-mono">
              <span>Múi giờ: {activeBranch?.timeZone || 'Asia/Ho_Chi_Minh'}</span>
              <span>•</span>
              <span>Phân quyền: @branchAuth (Scoped)</span>
            </div>
          </div>

          {/* ================= STEP 1: SELECT KIND & UPLOAD ================= */}
          {currentStep === 1 && (
            <div className="space-y-6">
              {/* Kind selector cards */}
              <div>
                <label className="block text-xs font-semibold text-stone-300 uppercase tracking-wider mb-2.5">
                  1. Chọn danh mục nhập liệu
                </label>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                  {(
                    ['OPENING_INVENTORY', 'ITEMS_CATALOG', 'SUPPLIERS'] as ImportKind[]
                  ).map((kind) => {
                    const cfg = IMPORT_KIND_CONFIGS[kind]
                    const isSelected = selectedKind === kind
                    const IconComponent =
                      kind === 'OPENING_INVENTORY'
                        ? Layers
                        : kind === 'ITEMS_CATALOG'
                        ? Package
                        : Truck

                    return (
                      <button
                        key={kind}
                        type="button"
                        onClick={() => setSelectedKind(kind)}
                        className={`text-left p-4 rounded-xl border transition-all relative ${
                          isSelected
                            ? 'border-emerald-500 bg-emerald-950/20 shadow-md ring-1 ring-emerald-500/30'
                            : 'border-stone-800 bg-stone-900/60 hover:bg-stone-800/40 hover:border-stone-700'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <div
                            className={`p-2 rounded-lg ${
                              isSelected
                                ? 'bg-emerald-500/20 text-emerald-400'
                                : 'bg-stone-800 text-stone-400'
                            }`}
                          >
                            <IconComponent className="h-5 w-5" />
                          </div>
                          <Badge
                            variant="outline"
                            className={`text-[9px] font-mono ${
                              isSelected
                                ? 'border-emerald-500/40 text-emerald-300 bg-emerald-950/60'
                                : 'border-stone-800 text-stone-500'
                            }`}
                          >
                            {cfg.badgeText}
                          </Badge>
                        </div>
                        <h3
                          className={`text-xs font-bold mb-1 ${
                            isSelected ? 'text-emerald-300' : 'text-stone-200'
                          }`}
                        >
                          {cfg.title}
                        </h3>
                        <p className="text-[11px] text-stone-400 line-clamp-2 leading-relaxed">
                          {cfg.description}
                        </p>
                      </button>
                    )
                  })}
                </div>
              </div>

              {/* Required Columns & Template Download */}
              <div className="rounded-xl border border-stone-800 bg-stone-950/50 p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Database className="h-4 w-4 text-emerald-400" />
                    <span className="text-xs font-semibold text-stone-200">
                      Cột dữ liệu yêu cầu cho: {kindConfig.title}
                    </span>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleDownloadTemplate}
                    className="border-stone-800 bg-stone-900 text-stone-300 hover:bg-stone-800 text-xs h-7"
                  >
                    <Download className="h-3 w-3 mr-1.5 text-emerald-400" />
                    Tải file mẫu (.csv)
                  </Button>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2 text-xs">
                  {kindConfig.columns.map((col) => (
                    <div
                      key={col.key}
                      className="p-2 rounded-lg bg-stone-900 border border-stone-800"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono text-[11px] font-bold text-stone-200">
                          {col.key}
                        </span>
                        {col.required ? (
                          <span className="text-[9px] text-red-400 font-bold">*</span>
                        ) : (
                          <span className="text-[9px] text-stone-500">tùy chọn</span>
                        )}
                      </div>
                      <div className="text-[10px] text-stone-400 truncate mt-0.5">
                        {col.label}
                      </div>
                      <div className="text-[10px] text-stone-500 font-mono italic truncate mt-0.5">
                        Ví dụ: {col.example}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* File Upload Zone */}
              <div>
                <label className="block text-xs font-semibold text-stone-300 uppercase tracking-wider mb-2.5">
                  2. Chọn tệp bảng tính Excel hoặc CSV
                </label>
                <div
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-all ${
                    isDragging
                      ? 'border-emerald-500 bg-emerald-950/20 scale-[0.99]'
                      : file
                      ? 'border-emerald-500/50 bg-stone-950/60'
                      : 'border-stone-800 bg-stone-950/30 hover:border-stone-700 hover:bg-stone-900/30'
                  }`}
                >
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".xlsx,.xls,.csv"
                    className="hidden"
                    onChange={handleFileInputChange}
                  />

                  {file ? (
                    <div className="flex flex-col items-center space-y-2">
                      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-400">
                        <FileSpreadsheet className="h-6 w-6" />
                      </div>
                      <div className="text-sm font-semibold text-stone-100">
                        {file.name}
                      </div>
                      <div className="text-xs font-mono text-stone-400">
                        {(file.size / 1024).toFixed(1)} KB • Định dạng:{' '}
                        {file.name.split('.').pop()?.toUpperCase()}
                      </div>
                      <p className="text-[11px] text-emerald-400/90 pt-1">
                        Nhấn hoặc kéo tệp khác để thay thế
                      </p>
                    </div>
                  ) : (
                    <div className="flex flex-col items-center space-y-2">
                      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-stone-900 border border-stone-800 text-stone-400">
                        <UploadCloud className="h-6 w-6" />
                      </div>
                      <div className="text-sm font-semibold text-stone-200">
                        Kéo thả tệp vào đây, hoặc{' '}
                        <span className="text-emerald-400 underline underline-offset-2">
                          chọn từ máy tính
                        </span>
                      </div>
                      <p className="text-xs text-stone-500">
                        Hỗ trợ .xlsx, .xls, .csv (Tối đa 10MB)
                      </p>
                      <div className="pt-2 text-[10px] text-stone-500 flex items-center space-x-1">
                        <ShieldCheck className="h-3 w-3 text-emerald-500/80" />
                        <span>Bảo vệ chống Formula Injection (=, +, -, @) theo BR-SEC-02</span>
                      </div>
                    </div>
                  )}
                </div>

                {uploadProgress !== null && (
                  <div className="mt-3 space-y-1">
                    <div className="flex justify-between text-xs font-mono text-stone-400">
                      <span>Đang tải lên và phân tích...</span>
                      <span>{uploadProgress}%</span>
                    </div>
                    <div className="h-1.5 w-full bg-stone-800 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-emerald-500 transition-all duration-200"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  </div>
                )}

                {stageError && (
                  <div className="mt-3 flex items-start space-x-2 rounded-lg border border-red-500/30 bg-red-950/40 p-3 text-xs text-red-300">
                    <AlertTriangle className="h-4 w-4 shrink-0 text-red-400 mt-0.5" />
                    <span>{stageError}</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* ================= STEP 2: PREVIEW & VALIDATION ================= */}
          {currentStep === 2 && batch && (
            <div className="space-y-6">
              {/* Batch KPI Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
                <div className="p-3.5 rounded-xl border border-stone-800 bg-stone-950/60">
                  <div className="text-[11px] font-medium text-stone-400">Tệp nguồn</div>
                  <div className="mt-1 text-xs font-mono font-bold text-stone-200 truncate" title={batch.sourceFilename}>
                    {batch.sourceFilename}
                  </div>
                  <div className="mt-1 text-[10px] text-stone-500 font-mono">
                    Loại: {batch.importKind}
                  </div>
                </div>

                <div className="p-3.5 rounded-xl border border-stone-800 bg-stone-950/60">
                  <div className="text-[11px] font-medium text-stone-400">Tổng số dòng đọc được</div>
                  <div className="mt-1 text-xl font-mono font-bold text-stone-100">
                    {batch.totalRows}
                  </div>
                  <div className="mt-1 text-[10px] text-stone-500 font-mono">
                    Trạng thái: {batch.status}
                  </div>
                </div>

                <div className="p-3.5 rounded-xl border border-emerald-500/30 bg-emerald-950/20">
                  <div className="text-[11px] font-medium text-emerald-400">Dòng hợp lệ</div>
                  <div className="mt-1 text-xl font-mono font-bold text-emerald-300">
                    {batch.validRows}
                  </div>
                  <div className="mt-1 text-[10px] text-emerald-400/80 font-mono">
                    {batch.totalRows > 0
                      ? `${Math.round((batch.validRows / batch.totalRows) * 100)}% thành công`
                      : '0%'}
                  </div>
                </div>

                <div
                  className={`p-3.5 rounded-xl border ${
                    batch.errorRows > 0
                      ? 'border-red-500/40 bg-red-950/30'
                      : 'border-stone-800 bg-stone-950/60'
                  }`}
                >
                  <div
                    className={`text-[11px] font-medium ${
                      batch.errorRows > 0 ? 'text-red-400' : 'text-stone-400'
                    }`}
                  >
                    Dòng phát hiện lỗi
                  </div>
                  <div
                    className={`mt-1 text-xl font-mono font-bold ${
                      batch.errorRows > 0 ? 'text-red-300' : 'text-stone-400'
                    }`}
                  >
                    {batch.errorRows}
                  </div>
                  <div className="mt-1 text-[10px] text-stone-500 font-mono">
                    {batch.errorRows > 0 ? 'Chặn cam kết (Gated)' : 'Không có lỗi'}
                  </div>
                </div>
              </div>

              {/* Preview Summary details if any */}
              {batch.previewSummary && Object.keys(batch.previewSummary).length > 0 && (
                <div className="rounded-xl border border-stone-800 bg-stone-950/40 p-3.5">
                  <div className="text-xs font-semibold text-stone-300 mb-2 flex items-center space-x-1.5">
                    <Database className="h-3.5 w-3.5 text-emerald-400" />
                    <span>Tổng hợp ước tính sơ bộ (Preview Summary):</span>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                    {Object.entries(batch.previewSummary).map(([key, val]) => (
                      <div key={key} className="bg-stone-900/60 p-2 rounded border border-stone-800/80">
                        <div className="text-[10px] text-stone-400 uppercase font-mono">{key}</div>
                        <div className="font-mono text-stone-200 font-bold mt-0.5">
                          {typeof val === 'number' ? val.toLocaleString('vi-VN') : String(val)}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* If there are errors -> Show Row Error Report */}
              {batch.errorRows > 0 ? (
                <div className="space-y-4">
                  {isLoadingRows ? (
                    <div className="flex h-36 items-center justify-center rounded-xl border border-stone-800 bg-stone-950/40">
                      <Loader2 className="h-5 w-5 animate-spin text-red-400 mr-2" />
                      <span className="text-xs text-stone-400">Đang nạp chi tiết các dòng lỗi...</span>
                    </div>
                  ) : (
                    <ImportRowErrorReport
                      errorRows={errorRows}
                      onReuploadRequested={() => setCurrentStep(1)}
                    />
                  )}
                </div>
              ) : (
                /* All rows valid -> Show Success Banner & Valid Rows Preview Table */
                <div className="space-y-4">
                  <div className="flex items-center space-x-3 rounded-xl border border-emerald-500/30 bg-emerald-950/30 p-4">
                    <CheckCircle2 className="h-6 w-6 text-emerald-400 shrink-0" />
                    <div>
                      <h4 className="text-sm font-semibold text-emerald-200">
                        Toàn bộ dữ liệu hợp lệ (100% Valid)
                      </h4>
                      <p className="mt-0.5 text-xs text-emerald-300/80">
                        Không phát hiện mã trùng lặp (TC-045) hoặc lỗi định dạng. Dữ liệu đã sẵn sàng để cam kết nhập kho chính thức.
                      </p>
                    </div>
                  </div>

                  {/* Preview Table of Valid Rows */}
                  <div className="rounded-xl border border-stone-800 bg-stone-950/60 overflow-hidden">
                    <div className="px-4 py-2.5 border-b border-stone-800 bg-stone-900/80 flex items-center justify-between text-xs">
                      <span className="font-semibold text-stone-200">
                        Bản xem trước dữ liệu hợp lệ ({validRows.length} dòng)
                      </span>
                      <span className="text-stone-500 font-mono text-[11px]">
                        Hiển thị dữ liệu đã chuẩn hóa (Normalized)
                      </span>
                    </div>

                    <div className="max-h-72 overflow-y-auto">
                      <table className="w-full text-left text-xs">
                        <thead className="sticky top-0 bg-stone-950 text-stone-400 font-mono border-b border-stone-800">
                          <tr>
                            <th className="py-2 px-3 w-16 text-center">#</th>
                            {kindConfig.columns.map((col) => (
                              <th key={col.key} className="py-2 px-3">
                                {col.label} ({col.key})
                              </th>
                            ))}
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-stone-800/60 text-stone-200">
                          {validRows.slice(0, 50).map((row) => {
                            const data = row.normalizedData || row.rawData || {}
                            return (
                              <tr key={row.id} className="hover:bg-stone-800/30">
                                <td className="py-2 px-3 text-center font-mono text-stone-500">
                                  {row.rowNumber}
                                </td>
                                {kindConfig.columns.map((col) => {
                                  const val = data[col.key]
                                  return (
                                    <td key={col.key} className="py-2 px-3 font-mono text-[11px]">
                                      {val !== undefined && val !== null ? String(val) : '-'}
                                    </td>
                                  )
                                })}
                              </tr>
                            )
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ================= STEP 3: COMMIT & CONFIRMATION ================= */}
          {currentStep === 3 && batch && (
            <div className="space-y-6">
              {batch.status === 'COMMITTED' ? (
                /* Committed State */
                <div className="flex flex-col items-center justify-center p-8 text-center space-y-4 rounded-2xl border border-emerald-500/40 bg-emerald-950/20">
                  <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-400">
                    <CheckCircle2 className="h-10 w-10" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-emerald-200">
                      Nhập dữ liệu thành công!
                    </h3>
                    <p className="mt-1 text-xs text-stone-300 max-w-md mx-auto">
                      Lô nhập liệu đã được ghi nhận chính thức vào hệ thống với mã lô{' '}
                      <code className="text-emerald-400 font-mono font-bold">{batch.id.substring(0, 8)}</code>.
                    </p>
                  </div>

                  <div className="p-4 rounded-xl border border-stone-800 bg-stone-900/80 text-xs w-full max-w-md text-left space-y-2">
                    <div className="flex justify-between py-1 border-b border-stone-800 text-stone-400">
                      <span>Mã định danh lô (Batch ID):</span>
                      <span className="font-mono text-stone-200">{batch.id}</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-stone-800 text-stone-400">
                      <span>Loại danh mục:</span>
                      <span className="font-semibold text-emerald-400">{batch.importKind}</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-stone-800 text-stone-400">
                      <span>Tổng số dòng cam kết:</span>
                      <span className="font-mono font-bold text-stone-200">{batch.validRows} dòng</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-stone-800 text-stone-400">
                      <span>Chi nhánh áp dụng:</span>
                      <span className="font-semibold text-stone-200">{activeBranch?.name}</span>
                    </div>
                    {batch.committedAt && (
                      <div className="flex justify-between py-1 text-stone-400">
                        <span>Thời gian cam kết:</span>
                        <span className="font-mono text-stone-200">
                          {new Date(batch.committedAt).toLocaleString('vi-VN')}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              ) : (
                /* Ready to Commit */
                <div className="space-y-4">
                  <div className="rounded-xl border border-stone-800 bg-stone-950/60 p-4 space-y-3">
                    <div className="flex items-center space-x-2 text-stone-200 font-semibold text-sm">
                      <ShieldCheck className="h-5 w-5 text-emerald-400" />
                      <span>Xác nhận cam kết nhập liệu vào cơ sở dữ liệu</span>
                    </div>
                    <p className="text-xs text-stone-400 leading-relaxed">
                      Bạn đang chuẩn bị cam kết <strong className="text-stone-200">{batch.validRows} dòng dữ liệu</strong> từ tệp <code className="text-emerald-400 font-mono">{batch.sourceFilename}</code> vào chi nhánh <strong className="text-stone-200">{activeBranch?.name}</strong>.
                    </p>

                    <div className="p-3 rounded-lg border border-stone-800 bg-stone-900/60 text-xs text-stone-300 space-y-1">
                      <div className="font-semibold text-emerald-400 flex items-center space-x-1">
                        <Check className="h-3.5 w-3.5" />
                        <span>Bảo vệ toàn vẹn giao dịch (Idempotency & Concurrency)</span>
                      </div>
                      <p className="text-[11px] text-stone-400">
                        Yêu cầu cam kết này tự động đính kèm khóa giao dịch <code>Idempotency-Key</code>. Nếu mất kết nối mạng giữa chừng, việc gửi lại sẽ an toàn và không gây trùng lặp bản ghi.
                      </p>
                    </div>
                  </div>

                  {commitError && (
                    <div className="flex items-start space-x-2 rounded-lg border border-red-500/30 bg-red-950/40 p-3 text-xs text-red-300">
                      <AlertTriangle className="h-4 w-4 shrink-0 text-red-400 mt-0.5" />
                      <span>{commitError}</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer Controls */}
        <div className="flex items-center justify-between border-t border-stone-800 px-6 py-4 bg-stone-950/70">
          <div>
            {currentStep === 2 && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentStep(1)}
                className="border-stone-800 bg-stone-900 text-stone-300 hover:bg-stone-800 text-xs"
              >
                <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                Quay lại tải tệp khác
              </Button>
            )}
            {currentStep === 3 && batch?.status !== 'COMMITTED' && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setCurrentStep(2)}
                disabled={isCommitting}
                className="border-stone-800 bg-stone-900 text-stone-300 hover:bg-stone-800 text-xs"
              >
                <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                Xem lại kết quả kiểm tra
              </Button>
            )}
          </div>

          <div className="flex items-center space-x-2">
            {currentStep === 1 && (
              <>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleClose}
                  className="border-stone-800 bg-stone-900 text-stone-400 hover:bg-stone-800 text-xs"
                >
                  Hủy bỏ
                </Button>
                <Button
                  size="sm"
                  onClick={handleStageFile}
                  disabled={!file || !selectedBranchId || isStaging}
                  className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold"
                >
                  {isStaging ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                      Đang xử lý...
                    </>
                  ) : (
                    <>
                      <span>Tiếp tục: Phân tích & Kiểm tra</span>
                      <ChevronRight className="h-3.5 w-3.5 ml-1" />
                    </>
                  )}
                </Button>
              </>
            )}

            {currentStep === 2 && batch && (
              <>
                {batch.errorRows > 0 ? (
                  <Button
                    size="sm"
                    onClick={() => setCurrentStep(1)}
                    className="bg-red-600 hover:bg-red-500 text-white text-xs"
                  >
                    <RotateCcw className="h-3.5 w-3.5 mr-1.5" />
                    Tải lên file đã sửa lỗi
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    onClick={() => setCurrentStep(3)}
                    className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold"
                  >
                    <span>Tiếp tục: Xác nhận cam kết</span>
                    <ChevronRight className="h-3.5 w-3.5 ml-1" />
                  </Button>
                )}
              </>
            )}

            {currentStep === 3 && (
              <>
                {batch?.status === 'COMMITTED' ? (
                  <>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleReset}
                      className="border-stone-800 bg-stone-900 text-stone-300 hover:bg-stone-800 text-xs"
                    >
                      <RotateCcw className="h-3.5 w-3.5 mr-1.5" />
                      Nhập thêm tệp khác
                    </Button>
                    <Button
                      size="sm"
                      onClick={handleClose}
                      className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold"
                    >
                      Hoàn tất & Đóng
                    </Button>
                  </>
                ) : (
                  <>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleClose}
                      disabled={isCommitting}
                      className="border-stone-800 bg-stone-900 text-stone-400 hover:bg-stone-800 text-xs"
                    >
                      Hủy
                    </Button>
                    <Button
                      size="sm"
                      onClick={handleCommit}
                      disabled={isCommitting || !batch || batch.errorRows > 0}
                      className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold"
                    >
                      {isCommitting ? (
                        <>
                          <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                          Đang cam kết...
                        </>
                      ) : (
                        <>
                          <Check className="h-3.5 w-3.5 mr-1.5" />
                          Xác nhận & Cam kết nhập liệu
                        </>
                      )}
                    </Button>
                  </>
                )}
              </>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  )
}
export default ImportWizardModal
