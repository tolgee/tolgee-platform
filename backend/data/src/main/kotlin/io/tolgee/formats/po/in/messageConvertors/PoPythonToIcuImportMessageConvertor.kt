package io.tolgee.formats.po.`in`.messageConvertors

import io.tolgee.formats.ImportMessageConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.paramConvertors.`in`.PythonToIcuParamConvertor

class PoPythonToIcuImportMessageConvertor : ImportMessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    return BasePoToIcuMessageConvertor { PythonToIcuParamConvertor() }.convert(
      rawData,
      languageTag,
      convertPlaceholders,
      isProjectIcuEnabled,
    )
  }
}
