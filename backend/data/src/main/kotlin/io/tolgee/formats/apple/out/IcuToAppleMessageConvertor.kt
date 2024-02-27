package io.tolgee.formats.apple.out

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToAppleMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean?,
) {
  fun convert(): PossiblePluralConversionResult {
    return BaseIcuMessageConvertor(
      message = message,
      argumentConvertor = AppleFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
    ).convert()
  }
}
