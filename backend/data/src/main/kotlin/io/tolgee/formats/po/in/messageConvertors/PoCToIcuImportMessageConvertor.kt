package io.tolgee.formats.po.`in`.messageConvertors

import io.tolgee.formats.ImportMessageConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.paramConvertors.`in`.CToIcuParamConvertor

class PoCToIcuImportMessageConvertor : ImportMessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    return BasePoToIcuMessageConvertor { CToIcuParamConvertor() }.convert(
      rawData,
      languageTag,
      convertPlaceholders,
      isProjectIcuEnabled,
    )
  }
}
