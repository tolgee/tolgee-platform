package io.tolgee.formats.genericStructuredFile.`in`

import com.ibm.icu.text.PluralRules
import io.tolgee.formats.forceEscapePluralForms
import io.tolgee.formats.importMessageFormat.ImportMessageFormat
import io.tolgee.service.dataImport.processors.STRING_WRAPPER_VALUE_ITEM
import java.util.*

class GenericStructuredRawDataToTextConvertor(
  private val format: ImportMessageFormat,
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
  ): List<StructuredRawDataConversionResult>? {
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
  ): List<StructuredRawDataConversionResult>? {
    if (rawData is Number || rawData is Boolean) {
      return listOf(StructuredRawDataConversionResult(rawData.toString(), isPlural = false))
    }

    if (rawData == null) {
      return listOf(StructuredRawDataConversionResult(null, isPlural = false))
    }

    val stringValue = getStringValue(rawData) ?: return null

    tryHandleIcuPlural(rawData, projectIcuPlaceholdersEnabled)?.let {
      return it
    }

    return convertSingleValue(
      stringValue,
      convertPlaceholdersToIcu,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun convertSingleValue(
    stringValue: String,
    convertPlaceholdersToIcu: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ): List<StructuredRawDataConversionResult> {
    val converted =
      format.messageConvertor.convert(
        rawData = stringValue,
        languageTag = languageTag,
        convertPlaceholders = convertPlaceholdersToIcu,
        isProjectIcuEnabled = projectIcuPlaceholdersEnabled,
      ).message

    return listOf(StructuredRawDataConversionResult(converted, null))
  }

  private fun getStringValue(rawData: Any?) =
    rawData as? String ?: (rawData as? Map<*, *>)
      ?.get(STRING_WRAPPER_VALUE_ITEM) as? String

  private fun tryHandleIcuPlural(
    rawData: Any?,
    projectIcuPlaceholdersEnabled: Boolean,
  ): List<StructuredRawDataConversionResult>? {
    if (format.canContainIcu && !projectIcuPlaceholdersEnabled) {
      val stringValue = getStringValue(rawData) ?: return null
      val escapedPlural = stringValue.forceEscapePluralForms()
      escapedPlural?.let {
        return listOf(StructuredRawDataConversionResult(escapedPlural, true))
      }
    }
    return null
  }

  private fun tryConvertToPlural(
    rawData: Any?,
    projectIcuPlaceholdersEnabled: Boolean,
    convertPlaceholdersToIcu: Boolean,
  ): List<StructuredRawDataConversionResult>? {
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
      ).message

    return listOf(StructuredRawDataConversionResult(converted, true))
  }
}
