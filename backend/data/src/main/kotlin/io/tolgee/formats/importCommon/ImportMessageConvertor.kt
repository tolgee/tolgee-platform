package io.tolgee.formats.importCommon

import io.tolgee.formats.MessageConvertorResult

interface ImportMessageConvertor {
  fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean = true,
    isProjectIcuEnabled: Boolean = true,
  ): MessageConvertorResult
}
