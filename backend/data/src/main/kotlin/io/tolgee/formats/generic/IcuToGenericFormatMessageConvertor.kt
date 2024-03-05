package io.tolgee.formats.generic

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.DEFAULT_PLURAL_ARGUMENT_NAME
import io.tolgee.formats.FromIcuParamConvertor
import io.tolgee.formats.NoOpFromIcuParamConvertor
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
  private val paramConvertorFactory: () -> FromIcuParamConvertor = { NoOpFromIcuParamConvertor() },
) {
  fun convert(): String? {
    message ?: return null
    val converted =
      BaseIcuMessageConvertor(
        message = message,
        argumentConvertor = paramConvertorFactory(),
        forceIsPlural = forceIsPlural,
        keepEscaping = isProjectIcuPlaceholdersEnabled,
      ).convert()

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
}
