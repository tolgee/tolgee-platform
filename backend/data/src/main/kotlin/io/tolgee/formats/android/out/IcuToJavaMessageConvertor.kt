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
      argumentConvertor = JavaFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
    ).convert()
  }
}
