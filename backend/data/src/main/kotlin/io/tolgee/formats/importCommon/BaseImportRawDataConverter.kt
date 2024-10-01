package io.tolgee.formats.importCommon

import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.forceEscapePluralForms
import io.tolgee.formats.getPluralForms

class BaseImportRawDataConverter(
  val canContainIcu: Boolean,
  val toIcuPlaceholderConvertorFactory: (() -> ToIcuPlaceholderConvertor)?,
  val convertPlaceholders: Boolean,
  val isProjectIcuEnabled: Boolean,
) {
  fun tryConvertStringValue(string: Any?): MessageConvertorResult? {
    val stringValue = unwrapString(string)

    if (stringValue !is String) {
      return null
    }

    if (doesNotNeedConversion) {
      if (canContainIcu) {
        getPluralForms(stringValue)?.let {
          return MessageConvertorResult(it.icuString, it.argName)
        }
      }
      return MessageConvertorResult(stringValue, null)
    }

    if (canContainIcu && !isProjectIcuEnabled) {
      tryEscapePlural(stringValue)?.let {
        return it
      }
    }

    val converted = convertMessage(stringValue, false)
    return MessageConvertorResult(converted.message, null)
  }

  private val doesNotNeedConversion =
    canContainIcu &&
      toIcuPlaceholderConvertorFactory == null &&
      isProjectIcuEnabled

  private fun tryEscapePlural(stringValue: String): MessageConvertorResult? {
    val escapedPlural = stringValue.forceEscapePluralForms()
    return escapedPlural
  }

  fun convertMessage(
    message: String,
    isInPlural: Boolean = false,
  ): MessageConvertorResult {
    return convertMessage(
      message,
      isInPlural,
      convertPlaceholders = convertPlaceholders,
      isProjectIcuEnabled = isProjectIcuEnabled,
      escapeUnmatched = !canContainIcu,
      convertorFactory = toIcuPlaceholderConvertorFactory,
    )
  }
}
