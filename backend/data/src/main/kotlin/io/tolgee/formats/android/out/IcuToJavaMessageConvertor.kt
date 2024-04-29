package io.tolgee.formats.android.out

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.paramConvertors.out.IcuToJavaPlaceholderConvertor

class IcuToJavaMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean? = null,
  private val isProjectIcuPlaceholdersEnabled: Boolean,
) {
  fun convert(): PossiblePluralConversionResult {
    return MessageConvertorFactory(message, forceIsPlural, isProjectIcuPlaceholdersEnabled) {
      IcuToJavaPlaceholderConvertor()
    }.create().convert()
  }
}
