package io.tolgee.formats

import io.tolgee.service.export.ExportFilePathProvider

enum class ExportFormat(
  val extension: String,
  val mediaType: String,
  val defaultFileStructureTemplate: String = ExportFilePathProvider.DEFAULT_TEMPLATE,
) {
  JSON("json", "application/json"),
  JSON_TOLGEE("json", "application/json"),
  XLIFF("xliff", "application/x-xliff+xml"),
  PO("po", "text/x-gettext-translation"),
  APPLE_STRINGS_STRINGSDICT(
    "",
    "",
    defaultFileStructureTemplate = "{namespace}/{languageTag}.lproj/Localizable.{extension}",
  ),
  APPLE_XLIFF(
    "xliff",
    "application/x-xliff+xml",
    defaultFileStructureTemplate = "{languageTag}.{extension}",
  ),
  ANDROID_XML(
    "xml",
    "application/xml",
    defaultFileStructureTemplate = "values-{androidLanguageTag}/strings.{extension}",
  ),
  FLUTTER_ARB(
    "arb",
    "application/json",
    defaultFileStructureTemplate = "app_{snakeLanguageTag}.{extension}",
  ),
  PROPERTIES("properties", "text/plain"),
  YAML_RUBY("yaml", "application/x-yaml"),
  YAML("yaml", "application/x-yaml"),
  JSON_I18NEXT("json", "application/json"),
  CSV("csv", "text/csv"),
}
