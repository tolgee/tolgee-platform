package io.tolgee.formats.po.out.php

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.service.export.exporters.ConversionResult

class ToPhpPoMessageConverter(
  val message: String,
  val languageTag: String = "en",
) : ToPoMessageConverter {
  private val baseIcuMessageToPoConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = PhpFromIcuParamConvertor(),
    )

  override fun convert(): ConversionResult {
    return baseIcuMessageToPoConvertor.convert()
  }
}
