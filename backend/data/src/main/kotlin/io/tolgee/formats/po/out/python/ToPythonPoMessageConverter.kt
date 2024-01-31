package io.tolgee.formats.po.out.python

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.ToPoMessageConverter
import io.tolgee.service.export.exporters.ConversionResult

class ToPythonPoMessageConverter(
  val message: String,
  val languageTag: String = "en",
) : ToPoMessageConverter {
  private val baseIcuMessageToPoConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = PythonFromIcuParamConvertor(),
    )

  override fun convert(): ConversionResult {
    return baseIcuMessageToPoConvertor.convert()
  }
}
