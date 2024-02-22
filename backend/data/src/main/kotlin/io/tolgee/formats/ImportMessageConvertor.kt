package io.tolgee.formats

interface ImportMessageConvertor {
  fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean = true,
  ): MessageConvertorResult
}
