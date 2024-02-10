package io.tolgee.formats.apple.out

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToAppleMessageConvertor(
  private val message: String,
) {
  fun convert(): PossiblePluralConversionResult {
    return BaseIcuMessageConvertor(
      message = message,
      argumentConverter = AppleFromIcuParamConvertor(),
    ).convert()
  }
}
