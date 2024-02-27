package io.tolgee.formats.po.out.php

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.ToPoConversionResult
import io.tolgee.formats.po.out.ToPoMessageConvertor

class ToPhpPoMessageConvertor(
  val message: String,
  val languageTag: String = "en",
  forceIsPlural: Boolean?,
) : ToPoMessageConvertor {
  private val baseIcuMessageToPoConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = PhpFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
    )

  override fun convert(): ToPoConversionResult {
    return baseIcuMessageToPoConvertor.convert()
  }
}