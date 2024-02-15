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
      argumentConverter = AppleFromIcuParamConvertor(),
      forceIsPlural = forceIsPlural,
    ).convert()
  }
}