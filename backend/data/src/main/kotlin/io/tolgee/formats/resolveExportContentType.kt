package io.tolgee.formats

import io.tolgee.formats.apple.out.getAppleExportPathContentType
import io.tolgee.util.nullIfEmpty

private const val ZIP_CONTENT_TYPE = "application/zip"
private const val UTF_8_CHARSET_SUFFIX = "; charset=UTF-8"
private const val TEXT_TYPE_PREFIX = "text/"

fun resolveExportContentType(
  format: ExportFormat,
  zip: Boolean,
  exportRelativePath: String,
): String? {
  if (zip) {
    return ZIP_CONTENT_TYPE
  }

  return rawMediaType(format, exportRelativePath)?.withCharsetIfText()
}

private fun rawMediaType(
  format: ExportFormat,
  exportRelativePath: String,
): String? {
  if (format == ExportFormat.APPLE_STRINGS_STRINGSDICT) {
    return getAppleExportPathContentType(exportRelativePath)
  }
  return format.mediaType.nullIfEmpty
}

private fun String.withCharsetIfText(): String {
  if (!this.startsWith(TEXT_TYPE_PREFIX)) {
    return this
  }
  return this + UTF_8_CHARSET_SUFFIX
}
