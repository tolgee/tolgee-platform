package io.tolgee.service.export

enum class ExportFilePathPlaceholder(
  val value: String,
) {
  NAMESPACE("namespace"),
  LANGUAGE_TAG("languageTag"),
  ANDROID_LANGUAGE_TAG("androidLanguageTag"),
  SNAKE_LANGUAGE_TAG("snakeLanguageTag"),
  EXTENSION("extension"),
  ;

  val placeholder = "{$value}"

  val placeholderForRegex = "\\{$value\\}"
}
