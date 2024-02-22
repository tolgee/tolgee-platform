package io.tolgee.formats.po.`in`.messageConvertors

import io.tolgee.formats.ImportMessageConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.po.`in`.paramConvertors.PhpToIcuParamConvertor

class PoPhpToIcuImportMessageConvertor : ImportMessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
  ): MessageConvertorResult {
    return BasePoToIcuMessageConvertor { PhpToIcuParamConvertor() }.convert(rawData, languageTag, convertPlaceholders)
  }
}
