package io.tolgee.formats.android.`in`

import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.ImportMessageConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.convertMessage
import io.tolgee.formats.escapeIcu

class AndroidToIcuMessageConvertor : ImportMessageConvertor {
  @Suppress("UNCHECKED_CAST")
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    forceEscapePluralForms: Boolean,
  ): MessageConvertorResult {
    val stringValue = rawData as? String ?: (rawData as? Map<*, *>)?.get("_stringValue") as? String

    if (stringValue is String) {
      val converted = convert(stringValue, false, convertPlaceholders)
      return MessageConvertorResult(converted, false)
    }

    if (rawData is Map<*, *>) {
      val rawDataMap = rawData as Map<String, String>
      val converted = convertPlural(rawDataMap, convertPlaceholders, forceEscapePluralForms)
      return MessageConvertorResult(converted, true)
    }

    if (rawData == null) {
      return MessageConvertorResult(null, false)
    }

    throw IllegalArgumentException("Unsupported type of message")
  }

  private fun convertPlural(
    rawData: Map<String, String>,
    convertPlaceholders: Boolean,
    forceEscapePluralForms: Boolean,
  ): String {
    val forms =
      rawData.mapNotNull {
        val converted = convert(it.value, true, convertPlaceholders)
        it.key to (converted ?: return@mapNotNull null)
      }.toMap()

    return FormsToIcuPluralConvertor(
      forms,
      forceEscape = forceEscapePluralForms,
      addNewLines = true,
      argName = "0",
    ).convert()
  }

  private fun convert(
    message: String,
    isInPlural: Boolean = false,
    convertPlaceholders: Boolean,
  ): String {
    if (!convertPlaceholders) return message.escapeIcu(isInPlural)
    return convertMessage(message, isInPlural) {
      JavaToIcuParamConvertor()
    }
  }
}
