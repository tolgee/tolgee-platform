package io.tolgee.formats.genericStructuredFile.`in`

import com.ibm.icu.text.PluralRules
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.importCommon.unwrapString
import java.util.*

class GenericStructuredRawDataToTextConvertor(
  private val format: ImportFormat,
  private val languageTag: String,
) : StructuredRawDataConvertor {
  private val availablePluralKeywords by lazy {
    val locale = Locale.forLanguageTag(languageTag)
    PluralRules.forLocale(locale).keywords.toSet()
  }

  override fun convert(
    rawData: Any?,
    projectIcuPlaceholdersEnabled: Boolean,
    convertPlaceholdersToIcu: Boolean,
  ): List<MessageConvertorResult>? {
    tryConvertToSingle(rawData, projectIcuPlaceholdersEnabled, convertPlaceholdersToIcu)
      ?.let {
        return it
      }
    tryConvertToPlural(rawData, projectIcuPlaceholdersEnabled, convertPlaceholdersToIcu)
      ?.let { return it }

    return null
  }

  private fun tryConvertToSingle(
    rawData: Any?,
    projectIcuPlaceholdersEnabled: Boolean,
    convertPlaceholdersToIcu: Boolean,
  ): List<MessageConvertorResult>? {
    if (rawData is Number || rawData is Boolean) {
      return listOf(MessageConvertorResult(rawData.toString(), null))
    }

    if (rawData == null) {
      return listOf(MessageConvertorResult(null, null))
    }

    val stringValue = getStringValue(rawData) ?: return null

    return convertStringValue(
      stringValue,
      convertPlaceholdersToIcu,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun convertStringValue(
    stringValue: String,
    convertPlaceholdersToIcu: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ): List<MessageConvertorResult> {
    val converted =
      format.messageConvertor.convert(
        rawData = stringValue,
        languageTag = languageTag,
        convertPlaceholders = convertPlaceholdersToIcu,
        isProjectIcuEnabled = projectIcuPlaceholdersEnabled,
      )

    return listOf(converted)
  }

  private fun getStringValue(rawData: Any?) = unwrapString(rawData)

  private fun tryConvertToPlural(
    rawData: Any?,
    projectIcuPlaceholdersEnabled: Boolean,
    convertPlaceholdersToIcu: Boolean,
  ): List<MessageConvertorResult>? {
    val map = rawData as? Map<*, *> ?: return null

    if (!format.pluralsViaNesting) {
      return null
    }

    if (!map.keys.all { it in availablePluralKeywords }) {
      return null
    }

    val safePluralMap =
      map.entries.map {
        val key = it.key as? String ?: return null
        val value = it.value as? String ?: return null
        key to value
      }.toMap()

    val converted =
      format.messageConvertor.convert(
        rawData = safePluralMap,
        languageTag = languageTag,
        convertPlaceholders = convertPlaceholdersToIcu,
        isProjectIcuEnabled = projectIcuPlaceholdersEnabled,
      )

    return listOf(converted)
  }
}
