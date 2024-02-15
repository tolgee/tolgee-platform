package io.tolgee.formats

interface MessageConvertor {
  fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean = true,
  ): MessageConvertorResult
}
