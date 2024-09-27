package io.tolgee.formats.android.out

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.paramConvertors.out.IcuToJavaPlaceholderConvertor

class IcuToJavaMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean,
  private val isProjectIcuPlaceholdersEnabled: Boolean,
) {
  fun convert(): PossiblePluralConversionResult {
    return MessageConvertorFactory(
      message = message,
      forceIsPlural = forceIsPlural,
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
    ) {
      IcuToJavaPlaceholderConvertor()
    }.create().convert()
  }
}
