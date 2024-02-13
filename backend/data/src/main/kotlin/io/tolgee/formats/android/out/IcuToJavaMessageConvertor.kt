package io.tolgee.formats.android.out

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToJavaMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean? = null,
) {
  fun convert(): PossiblePluralConversionResult {
    return BaseIcuMessageConvertor(
      message = message,
      argumentConverter = JavaFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
    ).convert()
  }
}
