package io.tolgee.formats

enum class ExportFormat(val extension: String, val mediaType: String) {
  JSON("json", "application/json"),
  XLIFF("xliff", "application/x-xliff+xml"),
  PO("po", "text/x-gettext-translation"),
  PO_PHP("po", "text/x-gettext-translation"),
  PO_C("po", "text/x-gettext-translation"),
  APPLE_STRINGS_STRINGSDICT("", ""),
  APPLE_XLIFF("xliff", "application/x-xliff+xml"),
  ANDROID_XML("xml", "application/xml"),
  FLUTTER_ARB("arb", "application/json"),
  PROPERTIES("properties", "text/plain"),
  YAML_RUBY("yaml", "application/x-yaml"),
  YAML_ICU("yaml", "application/x-yaml"),
  YAML_JAVA("yaml", "application/x-yaml"),
}
