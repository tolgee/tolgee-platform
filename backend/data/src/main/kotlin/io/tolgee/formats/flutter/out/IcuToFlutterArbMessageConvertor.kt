package io.tolgee.formats.flutter.out

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToFlutterArbMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean? = null,
) {
  fun convert(): PossiblePluralConversionResult {
    return BaseIcuMessageConvertor(
      message = message,
      argumentConvertor = FlutterArbFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
    ).convert()
  }
}
