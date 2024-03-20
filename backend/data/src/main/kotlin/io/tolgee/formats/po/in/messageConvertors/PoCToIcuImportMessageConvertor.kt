package io.tolgee.formats.po.`in`.messageConvertors

import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.importMessageFormat.ImportMessageConvertor
import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor

class PoCToIcuImportMessageConvertor : ImportMessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    return BasePoToIcuMessageConvertor { CToIcuPlaceholderConvertor() }.convert(
      rawData,
      languageTag,
      convertPlaceholders,
      isProjectIcuEnabled,
    )
  }
}
