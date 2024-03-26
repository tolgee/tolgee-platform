package io.tolgee.formats.flutter.out

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.PossiblePluralConversionResult

class IcuToFlutterArbMessageConvertor(
  private val message: String,
  private val forceIsPlural: Boolean,
  private val isProjectIcuPlaceholdersEnabled: Boolean,
) {
  fun convert(): PossiblePluralConversionResult {
    return MessageConvertorFactory(message, forceIsPlural, isProjectIcuPlaceholdersEnabled) {
      FlutterArbFromIcuPlaceholderConvertor()
    }.create().convert()
  }
}
