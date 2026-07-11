package io.tolgee.formats.generic

import io.tolgee.formats.DEFAULT_PLURAL_ARGUMENT_NAME
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.toIcuPluralString

/**
 * Converts ICU message to generic format message
 *
 * Generic format is a format like JSON or XLIFF, which potentially can be exported with different placeholder
 * format
 */
class IcuToGenericFormatMessageConvertor(
  private val message: String?,
  private val forceIsPlural: Boolean,
  private val isProjectIcuPlaceholdersEnabled: Boolean,
  private val paramConvertorFactory: () -> FromIcuPlaceholderConvertor,
) {
  fun convert(): String? {
    val converted = getConvertorResult()
    converted ?: return null

    val singleResult = converted.singleResult
    if (singleResult != null) {
      return singleResult
    }

    val formsResult = converted.formsResult ?: return ""
    return formsResult
      .toIcuPluralString(
        addNewLines = false,
        argName = converted.argName ?: DEFAULT_PLURAL_ARGUMENT_NAME,
      )
  }

  fun getForcedPluralForms(): Map<String, String>? {
    return getConvertorResult()?.formsResult
  }

  private fun getConvertorResult(): PossiblePluralConversionResult? {
    message ?: return null
    return MessageConvertorFactory(
      message = message,
      forceIsPlural = forceIsPlural,
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      paramConvertorFactory = paramConvertorFactory,
    ).create().convert()
  }
}
