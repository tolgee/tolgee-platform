package io.tolgee.formats.po.`in`.messageConvertors

import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.importMessageFormat.ImportMessageConvertor
import io.tolgee.formats.paramConvertors.`in`.PythonToIcuPlaceholderConvertor

class PoPythonToIcuImportMessageConvertor : ImportMessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    return BasePoToIcuMessageConvertor { PythonToIcuPlaceholderConvertor() }.convert(
      rawData,
      languageTag,
      convertPlaceholders,
      isProjectIcuEnabled,
    )
  }
}
