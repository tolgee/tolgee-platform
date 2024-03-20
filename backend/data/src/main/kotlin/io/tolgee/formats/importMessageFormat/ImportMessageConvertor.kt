package io.tolgee.formats.importMessageFormat

import io.tolgee.formats.MessageConvertorResult

interface ImportMessageConvertor {
  fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean = true,
    isProjectIcuEnabled: Boolean = true,
  ): MessageConvertorResult
}
