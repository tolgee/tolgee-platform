package io.tolgee.formats.unity.out

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToUnityMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean,
  private val isProjectIcuPlaceholdersEnabled: Boolean,
) {
  fun convert(): PossiblePluralConversionResult {
    return MessageConvertorFactory(message, forceIsPlural, isProjectIcuPlaceholdersEnabled) {
      UnityFromIcuPlaceholderConvertor()
    }.create().convert()
  }
}
