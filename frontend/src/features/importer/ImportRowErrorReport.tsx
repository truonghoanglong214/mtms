import React, { useState, useMemo } from 'react'
import {
  AlertOctagon,
  Search,
  Filter,
  Copy,
  Check,
  ChevronDown,
  ChevronRight,
  Code2,
} from 'lucide-react'
import { Badge } from '@/shared/components/ui/badge'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import type { ImportRow } from './types'

interface ImportRowErrorReportProps {
  errorRows: ImportRow[]
  onReuploadRequested?: () => void
}

const getErrorMeta = (errorCode: string) => {
  const code = (errorCode || '').toUpperCase()
  if (code.includes('DUPLICATE')) {
    return {
      label: 'TC-045: Trùng lặp mã',
      colorClass: 'bg-red-950/80 text-red-300 border-red-500/40',
      isDuplicate: true,
    }
  }
  if (code.includes('REQUIRED') || code.includes('MISSING')) {
    return {
      label: 'Thiếu dữ liệu bắt buộc',
      colorClass: 'bg-amber-950/80 text-amber-300 border-amber-500/40',
      isDuplicate: false,
    }
  }
  if (code.includes('UNIT')) {
    return {
      label: 'Đơn vị tính không hợp lệ',
      colorClass: 'bg-orange-950/80 text-orange-300 border-orange-500/40',
      isDuplicate: false,
    }
  }
  if (code.includes('NEGATIVE') || code.includes('LESS_THAN')) {
    return {
      label: 'Giá trị / Số lượng âm',
      colorClass: 'bg-rose-950/80 text-rose-300 border-rose-500/40',
      isDuplicate: false,
    }
  }
  if (code.includes('FORMULA') || code.includes('INJECTION')) {
    return {
      label: 'Bảo mật: CSV Injection',
      colorClass: 'bg-purple-950/80 text-purple-300 border-purple-500/40',
      isDuplicate: false,
    }
  }
  return {
    label: errorCode || 'Lỗi định dạng',
    colorClass: 'bg-stone-800 text-stone-300 border-stone-700',
    isDuplicate: false,
  }
}

export const ImportRowErrorReport: React.FC<ImportRowErrorReportProps> = ({
  errorRows,
  onReuploadRequested,
}) => {
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedErrorType, setSelectedErrorType] = useState<string>('ALL')
  const [expandedRowIds, setExpandedRowIds] = useState<Record<string, boolean>>({})
  const [copied, setCopied] = useState(false)

  // Extract distinct error codes for filter dropdown
  const distinctErrorTypes = useMemo(() => {
    const types = new Set<string>()
    errorRows.forEach((row) => {
      row.errors?.forEach((err) => {
        if (err.error) types.add(err.error)
      })
    })
    return Array.from(types)
  }, [errorRows])

  // Count duplicate identifier errors (TC-045)
  const duplicateErrorsCount = useMemo(() => {
    return errorRows.reduce((acc, row) => {
      const hasDup = row.errors?.some((e) =>
        (e.error || '').toUpperCase().includes('DUPLICATE')
      )
      return hasDup ? acc + 1 : acc
    }, 0)
  }, [errorRows])

  // Filter rows
  const filteredRows = useMemo(() => {
    return errorRows.filter((row) => {
      // Type filter
      if (selectedErrorType !== 'ALL') {
        const matchesType = row.errors?.some((e) => e.error === selectedErrorType)
        if (!matchesType) return false
      }

      // Query filter
      if (!searchQuery.trim()) return true
      const q = searchQuery.trim().toLowerCase()

      // Match row number
      if (String(row.rowNumber).includes(q)) return true

      // Match in raw data
      const rawText = JSON.stringify(row.rawData || {}).toLowerCase()
      if (rawText.includes(q)) return true

      // Match in errors
      const errorText = row.errors
        ?.map((e) => `${e.field} ${e.error} ${e.message}`)
        .join(' ')
        .toLowerCase()
      if (errorText?.includes(q)) return true

      return false
    })
  }, [errorRows, searchQuery, selectedErrorType])

  const toggleExpand = (rowId: string) => {
    setExpandedRowIds((prev) => ({
      ...prev,
      [rowId]: !prev[rowId],
    }))
  }

  const handleCopyErrorReport = () => {
    const reportText = errorRows
      .map((r) => {
        const errList = r.errors
          ?.map((e) => `[${e.field} - ${e.error}]: ${e.message}`)
          .join('; ')
        const rawJson = JSON.stringify(r.rawData)
        return `Dòng ${r.rowNumber}: ${errList} | Raw: ${rawJson}`
      })
      .join('\n')

    navigator.clipboard.writeText(reportText).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2500)
    })
  }

  return (
    <div className="space-y-4">
      {/* Alert Header Banner */}
      <div className="rounded-lg border border-red-500/30 bg-red-950/30 p-4">
        <div className="flex items-start justify-between">
          <div className="flex items-start space-x-3">
            <AlertOctagon className="h-5 w-5 shrink-0 text-red-400 mt-0.5" />
            <div>
              <h4 className="text-sm font-semibold text-red-200">
                Phát hiện {errorRows.length} dòng dữ liệu không hợp lệ
              </h4>
              <p className="mt-1 text-xs text-red-300/80 leading-relaxed">
                Hệ thống MTMS tuân thủ nguyên tắc kiểm soát dữ liệu nghiêm ngặt (BR-SEC-02).
                Quá trình nhập dữ liệu (Commit) bị chặn cho đến khi toàn bộ lỗi được sửa trên file gốc và tải lên lại.
              </p>
              {duplicateErrorsCount > 0 && (
                <div className="mt-2 inline-flex items-center space-x-1.5 rounded bg-red-900/60 px-2 py-0.5 text-[11px] font-mono text-red-200 border border-red-500/40">
                  <span className="font-bold">TC-045:</span>
                  <span>Có {duplicateErrorsCount} dòng trùng lặp mã định danh</span>
                </div>
              )}
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleCopyErrorReport}
              className="border-stone-800 bg-stone-900/80 text-stone-300 hover:bg-stone-800 text-xs"
              title="Sao chép danh sách lỗi để đối chiếu với Excel"
            >
              {copied ? (
                <>
                  <Check className="h-3.5 w-3.5 mr-1 text-emerald-400" />
                  <span className="text-emerald-400">Đã sao chép</span>
                </>
              ) : (
                <>
                  <Copy className="h-3.5 w-3.5 mr-1" />
                  <span>Sao chép lỗi</span>
                </>
              )}
            </Button>
            {onReuploadRequested && (
              <Button
                size="sm"
                onClick={onReuploadRequested}
                className="bg-red-600 hover:bg-red-500 text-white text-xs"
              >
                Tải lên lại
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-stone-950/60 p-2.5 rounded-lg border border-stone-800">
        <div className="relative w-full sm:max-w-xs">
          <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-stone-500" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo số dòng, mã, lỗi..."
            className="pl-8 bg-stone-900 border-stone-800 text-xs text-stone-100 placeholder:text-stone-500 h-8"
          />
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <Filter className="h-3.5 w-3.5 text-stone-400" />
          <span className="text-xs text-stone-400">Lọc theo mã lỗi:</span>
          <select
            value={selectedErrorType}
            onChange={(e) => setSelectedErrorType(e.target.value)}
            className="rounded-md border border-stone-800 bg-stone-900 px-2 py-1 text-xs text-stone-200 focus:outline-none focus:border-emerald-500"
          >
            <option value="ALL">Tất cả ({errorRows.length})</option>
            {distinctErrorTypes.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Detailed Error Rows Table */}
      <div className="rounded-lg border border-stone-800 overflow-hidden bg-stone-900/60">
        <div className="max-h-96 overflow-y-auto">
          <table className="w-full text-left text-xs">
            <thead className="sticky top-0 bg-stone-950 text-stone-400 font-mono border-b border-stone-800 z-10">
              <tr>
                <th className="py-2.5 px-3 w-16 text-center">Dòng #</th>
                <th className="py-2.5 px-3 w-32">Cột / Trường</th>
                <th className="py-2.5 px-3 w-44">Phân loại lỗi</th>
                <th className="py-2.5 px-3">Chi tiết nguyên nhân & Giá trị tệp</th>
                <th className="py-2.5 px-3 w-16 text-right">Chi tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-800/60 text-stone-200">
              {filteredRows.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-stone-500 font-mono">
                    Không tìm thấy dòng lỗi nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                filteredRows.map((row) => {
                  const isExpanded = expandedRowIds[row.id] ?? false
                  const rawValues = row.rawData || {}

                  return (
                    <React.Fragment key={row.id}>
                      <tr className="hover:bg-stone-800/30 transition-colors">
                        {/* Row Number Badge */}
                        <td className="py-2.5 px-3 text-center align-top">
                          <span className="inline-flex items-center justify-center font-mono font-bold text-xs bg-red-950/70 border border-red-500/30 text-red-400 rounded px-1.5 py-0.5">
                            #{row.rowNumber}
                          </span>
                        </td>

                        {/* Faulty Fields */}
                        <td className="py-2.5 px-3 align-top font-mono text-[11px]">
                          {row.errors?.map((err, idx) => (
                            <div key={idx} className="text-stone-300 font-semibold mb-1">
                              {err.field || 'Toàn bộ dòng'}
                            </div>
                          ))}
                        </td>

                        {/* Error Type Badge */}
                        <td className="py-2.5 px-3 align-top">
                          {row.errors?.map((err, idx) => {
                            const meta = getErrorMeta(err.error)
                            return (
                              <div key={idx} className="mb-1">
                                <Badge
                                  variant="outline"
                                  className={`text-[10px] font-mono px-1.5 py-0.5 ${meta.colorClass}`}
                                >
                                  {meta.label}
                                </Badge>
                              </div>
                            )
                          })}
                        </td>

                        {/* Vietnamese Explanation & Faulty Value */}
                        <td className="py-2.5 px-3 align-top space-y-1">
                          {row.errors?.map((err, idx) => {
                            const faultyValue =
                              err.field && rawValues[err.field] !== undefined
                                ? String(rawValues[err.field])
                                : undefined

                            return (
                              <div key={idx} className="space-y-0.5">
                                <div className="text-stone-200 leading-relaxed font-medium">
                                  {err.message || 'Dữ liệu không đáp ứng quy tắc kiểm tra'}
                                </div>
                                {faultyValue !== undefined && (
                                  <div className="flex items-center space-x-1.5 text-[11px] font-mono">
                                    <span className="text-stone-500">Giá trị đọc được:</span>
                                    <code className="bg-stone-950 px-1.5 py-0.2 rounded border border-stone-800 text-amber-300">
                                      {faultyValue === '' ? '<khoảng trống>' : `"${faultyValue}"`}
                                    </code>
                                  </div>
                                )}
                              </div>
                            )
                          })}
                        </td>

                        {/* Expand Action */}
                        <td className="py-2.5 px-3 align-top text-right">
                          <button
                            onClick={() => toggleExpand(row.id)}
                            className="p-1 text-stone-400 hover:text-stone-200 hover:bg-stone-800 rounded transition-colors"
                            title={isExpanded ? 'Thu gọn dòng dữ liệu' : 'Xem toàn bộ dữ liệu dòng'}
                          >
                            {isExpanded ? (
                              <ChevronDown className="h-4 w-4" />
                            ) : (
                              <ChevronRight className="h-4 w-4" />
                            )}
                          </button>
                        </td>
                      </tr>

                      {/* Expanded View: All Raw Data for this row */}
                      {isExpanded && (
                        <tr className="bg-stone-950/80">
                          <td colSpan={5} className="p-3 border-t border-stone-800/80">
                            <div className="space-y-2">
                              <div className="flex items-center justify-between">
                                <div className="flex items-center space-x-2 text-[11px] font-mono text-stone-400">
                                  <Code2 className="h-3.5 w-3.5 text-stone-500" />
                                  <span>Dữ liệu thô từ file Excel (Dòng {row.rowNumber}):</span>
                                </div>
                              </div>
                              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2 text-xs">
                                {Object.entries(rawValues).map(([key, val]) => {
                                  const isFaultyField = row.errors?.some(
                                    (e) => e.field?.toLowerCase() === key.toLowerCase()
                                  )

                                  return (
                                    <div
                                      key={key}
                                      className={`p-2 rounded border ${
                                        isFaultyField
                                          ? 'border-red-500/40 bg-red-950/20'
                                          : 'border-stone-800 bg-stone-900/60'
                                      }`}
                                    >
                                      <div className="text-[10px] font-mono text-stone-400 truncate flex items-center justify-between">
                                        <span>{key}</span>
                                        {isFaultyField && (
                                          <span className="text-[9px] text-red-400 font-bold">LỖI</span>
                                        )}
                                      </div>
                                      <div
                                        className={`font-mono text-xs mt-0.5 break-all ${
                                          isFaultyField
                                            ? 'text-red-300 font-semibold'
                                            : 'text-stone-200'
                                        }`}
                                      >
                                        {val === null || val === undefined || val === '' ? (
                                          <span className="text-stone-600 italic">null</span>
                                        ) : (
                                          String(val)
                                        )}
                                      </div>
                                    </div>
                                  )
                                })}
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
export default ImportRowErrorReport
