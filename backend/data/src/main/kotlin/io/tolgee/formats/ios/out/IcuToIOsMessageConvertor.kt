package io.tolgee.formats.ios.out

import io.tolgee.formats.BaseIcuMessageToCLikeConvertor
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToIOsMessageConvertor(
  private val message: String,
) {
  fun convert(): PossiblePluralConversionResult {
    return BaseIcuMessageToCLikeConvertor(
      message = message,
      argumentConverter = IOsFromIcuParamConvertor(),
    ).convert()
  }
}
