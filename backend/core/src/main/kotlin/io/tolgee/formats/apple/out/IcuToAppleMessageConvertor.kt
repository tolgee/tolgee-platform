package io.tolgee.formats.apple.out

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.paramConvertors.out.IcuToApplePlaceholderConvertor

class IcuToAppleMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
) {
  fun convert(): PossiblePluralConversionResult {
    return MessageConvertorFactory(message, forceIsPlural, isProjectIcuPlaceholdersEnabled) {
      IcuToApplePlaceholderConvertor()
    }.create().convert()
  }
}
