package io.tolgee.formats.apple.out

import io.tolgee.formats.BaseIcuMessageToCLikeConvertor
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToAppleMessageConvertor(
  private val message: String,
) {
  fun convert(): PossiblePluralConversionResult {
    return BaseIcuMessageToCLikeConvertor(
      message = message,
      argumentConverter = AppleFromIcuParamConvertor(),
    ).convert()
  }
}
