package io.tolgee.formats.po.out.c

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.ToPoConversionResult
import io.tolgee.formats.po.out.ToPoMessageConvertor

class ToCPoMessageConvertor(
  val message: String,
  val languageTag: String = "en",
  forceIsPlural: Boolean,
  projectIcuPlaceholdersSupport: Boolean = true,
) : ToPoMessageConvertor {
  private val baseIcuMessageToClikeConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = CFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
    )

  override fun convert(): ToPoConversionResult {
    return baseIcuMessageToClikeConvertor.convert()
  }
}
