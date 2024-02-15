package io.tolgee.formats.po.`in`.messageConverters

import io.tolgee.formats.MessageConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.po.`in`.paramConvertors.PhpToIcuParamConvertor

class PoPhpToIcuMessageConverter : MessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
  ): MessageConvertorResult {
    return BasePoToIcuMessageConverter { PhpToIcuParamConvertor() }.convert(rawData, languageTag, convertPlaceholders)
  }
}
