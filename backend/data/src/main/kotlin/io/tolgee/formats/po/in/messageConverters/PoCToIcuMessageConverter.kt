package io.tolgee.formats.po.`in`.messageConverters

import io.tolgee.formats.MessageConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.po.`in`.paramConvertors.CToIcuParamConvertor

class PoCToIcuMessageConverter : MessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
  ): MessageConvertorResult {
    return BasePoToIcuMessageConverter { CToIcuParamConvertor() }.convert(rawData, languageTag, convertPlaceholders)
  }
}
