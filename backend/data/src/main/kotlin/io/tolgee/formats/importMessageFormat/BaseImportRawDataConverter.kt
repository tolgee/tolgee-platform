package io.tolgee.formats.importMessageFormat

import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.forceEscapePluralForms

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
      return MessageConvertorResult(stringValue, false)
    }

    if (canContainIcu && !isProjectIcuEnabled) {
      tryEscapePlural(stringValue)?.let {
        return it
      }
    }

    val converted = convertMessage(stringValue, false)
    return MessageConvertorResult(converted, false)
  }

  private val doesNotNeedConversion =
    canContainIcu &&
      toIcuPlaceholderConvertorFactory == null &&
      isProjectIcuEnabled

  private fun tryEscapePlural(stringValue: String): MessageConvertorResult? {
    val escapedPlural = stringValue.forceEscapePluralForms()
    val escapedText = escapedPlural ?: return null
    return MessageConvertorResult(escapedText, true)
  }

  fun convertMessage(
    message: String,
    isInPlural: Boolean = false,
  ): String {
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
