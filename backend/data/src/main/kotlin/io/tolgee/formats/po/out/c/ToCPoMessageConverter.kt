package io.tolgee.formats.po.out.c

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.ToPoConversionResult
import io.tolgee.formats.po.out.ToPoMessageConverter

class ToCPoMessageConverter(
  val message: String,
  val languageTag: String = "en",
) : ToPoMessageConverter {
  private val baseIcuMessageToClikeConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = CFromIcuParamConvertor(),
    )

  override fun convert(): ToPoConversionResult {
    return baseIcuMessageToClikeConvertor.convert()
  }
}
