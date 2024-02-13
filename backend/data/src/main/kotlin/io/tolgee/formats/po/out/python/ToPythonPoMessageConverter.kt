package io.tolgee.formats.po.out.python

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.ToPoConversionResult
import io.tolgee.formats.po.out.ToPoMessageConverter

class ToPythonPoMessageConverter(
  val message: String,
  val languageTag: String = "en",
  forceIsPlural: Boolean?,
) : ToPoMessageConverter {
  private val baseIcuMessageToClikeConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = PythonFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
    )

  override fun convert(): ToPoConversionResult {
    return baseIcuMessageToClikeConvertor.convert()
  }
}
