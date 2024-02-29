package io.tolgee.formats

enum class ExportFormat(val extension: String, val mediaType: String) {
  JSON("json", "application/json"),
  XLIFF("xlf", "application/x-xliff+xml"),
  PO("po", "text/x-gettext-translation"),
  APPLE_STRINGS_STRINGSDICT("", ""),
  APPLE_XLIFF("xliff", "application/x-xliff+xml"),
  ANDROID_XML("xml", "application/xml"),
  FLUTTER_ARB("arb", "application/json"),
}
