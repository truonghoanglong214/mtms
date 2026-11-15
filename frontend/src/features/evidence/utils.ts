export const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB

export const ALLOWED_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'application/pdf',
] as const

export const ALLOWED_FILE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.webp', '.pdf']

export function formatBytes(bytes: number, decimals = 2): string {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
}

export function truncateChecksum(checksum: string, prefixLen = 8, suffixLen = 8): string {
  if (!checksum || checksum.length <= prefixLen + suffixLen) return checksum || ''
  return `${checksum.slice(0, prefixLen)}...${checksum.slice(-suffixLen)}`
}

export function isImageContentType(contentType: string): boolean {
  return contentType.startsWith('image/')
}

export function isPdfContentType(contentType: string): boolean {
  return contentType === 'application/pdf'
}

export function validateEvidenceFile(file: File): { valid: boolean; error?: string } {
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return {
      valid: false,
      error: `Tệp "${file.name}" dung lượng ${formatBytes(file.size)}, vượt quá giới hạn cho phép (tối đa 5 MB).`,
    }
  }

  const matchesMime = ALLOWED_MIME_TYPES.includes(file.type as typeof ALLOWED_MIME_TYPES[number])
  const lowerName = file.name.toLowerCase()
  const matchesExt = ALLOWED_FILE_EXTENSIONS.some((ext) => lowerName.endsWith(ext))

  if (!matchesMime && !matchesExt) {
    return {
      valid: false,
      error: `Định dạng tệp không được hỗ trợ. Hệ thống chỉ cho phép JPG, PNG, WEBP hoặc PDF.`,
    }
  }

  return { valid: true }
}
