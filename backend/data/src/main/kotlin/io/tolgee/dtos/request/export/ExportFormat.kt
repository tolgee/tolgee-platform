package io.tolgee.dtos.request.export

enum class ExportFormat(val extension: String, val mediaType: String) {
  JSON("json", "application/json"),
  XLIFF("xlf", "application/x-xliff+xml"),
  PO_PHP("po", "text/x-gettext-translation"),
  PO_C("po", "text/x-gettext-translation"),
  PO_PYTHON("po", "text/x-gettext-translation"),
}
