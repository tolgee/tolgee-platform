package io.tolgee.formats.po.out.python

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.ToPoConversionResult
import io.tolgee.formats.po.out.ToPoMessageConvertor

class ToPythonPoMessageConvertor(
  val message: String,
  val languageTag: String = "en",
  forceIsPlural: Boolean,
  projectIcuPlaceholdersSupport: Boolean = true,
) : ToPoMessageConvertor {
  private val baseIcuMessageToClikeConvertor =
    BaseIcuMessageToPoConvertor(
      message = message,
      languageTag = languageTag,
      argumentConverter = PythonFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
    )

  override fun convert(): ToPoConversionResult {
    return baseIcuMessageToClikeConvertor.convert()
  }
}
