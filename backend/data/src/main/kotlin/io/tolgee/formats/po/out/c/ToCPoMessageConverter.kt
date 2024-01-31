package io.tolgee.formats.po.out.c

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.ToPoMessageConverter
import io.tolgee.service.export.exporters.ConversionResult

class ToCPoMessageConverter(
  val message: String,
  val languageTag: String = "en",
) : ToPoMessageConverter {
  private val baseIcuMessageToPoConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = CFromIcuParamConvertor(),
    )

  override fun convert(): ConversionResult {
    return baseIcuMessageToPoConvertor.convert()
  }
}
