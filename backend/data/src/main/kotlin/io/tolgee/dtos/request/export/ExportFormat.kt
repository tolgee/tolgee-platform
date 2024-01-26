package io.tolgee.dtos.request.export

enum class ExportFormat(val extension: String, val mediaType: String) {
  JSON("json", "application/json"),
  XLIFF("xlf", "application/x-xliff+xml"),
  PO("po", "text/x-gettext-translation"),
}
