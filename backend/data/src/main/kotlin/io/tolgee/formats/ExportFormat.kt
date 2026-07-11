package io.tolgee.formats

import io.tolgee.service.export.ExportFilePathProvider

enum class ExportFormat(
  val extension: String,
  val mediaType: String,
  val defaultFileStructureTemplate: String = ExportFilePathProvider.DEFAULT_TEMPLATE,
  val multiLanguage: Boolean = false,
  val fileStructureExtensionRequired: Boolean = false,
) {
  JSON("json", "application/json"),
  JSON_TOLGEE("json", "application/json"),
  XLIFF("xliff", "application/x-xliff+xml"),
  PO("po", "text/x-gettext-translation"),
  APPLE_STRINGS_STRINGSDICT(
    "",
    "",
    defaultFileStructureTemplate = "{namespace}/{languageTag}.lproj/Localizable.{extension}",
    fileStructureExtensionRequired = true,
  ),
  APPLE_XLIFF(
    "xliff",
    "application/x-xliff+xml",
    defaultFileStructureTemplate = "{namespace}/{languageTag}.{extension}",
  ),
  ANDROID_XML(
    "xml",
    "application/xml",
    defaultFileStructureTemplate = "{namespace}/values-{androidLanguageTag}/strings.{extension}",
  ),
  COMPOSE_XML(
    "xml",
    "application/xml",
    defaultFileStructureTemplate = "{namespace}/values-{androidLanguageTag}/strings.{extension}",
  ),
  FLUTTER_ARB(
    "arb",
    "application/json",
    defaultFileStructureTemplate = "{namespace}/app_{snakeLanguageTag}.{extension}",
  ),
  PROPERTIES("properties", "text/plain"),
  YAML_RUBY("yaml", "application/x-yaml"),
  YAML("yaml", "application/x-yaml"),
  JSON_I18NEXT("json", "application/json"),
  CSV("csv", "text/csv"),
  RESX_ICU("resx", "text/microsoft-resx"),
  XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
  APPLE_XCSTRINGS(
    "xcstrings",
    "application/json",
    defaultFileStructureTemplate = "{namespace}/Localizable.{extension}",
    multiLanguage = true,
  ),
  ANDROID_SDK(
    "json",
    "application/json",
  ),
  APPLE_SDK(
    "json",
    "application/json",
  ),
}
