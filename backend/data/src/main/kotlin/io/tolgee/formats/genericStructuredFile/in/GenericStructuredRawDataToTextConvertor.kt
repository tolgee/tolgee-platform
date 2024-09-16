package io.tolgee.formats.genericStructuredFile.`in`

import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.allPluralKeywords
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.importCommon.unwrapString

class GenericStructuredRawDataToTextConvertor(
  private val format: ImportFormat,
  private val languageTag: String,
) : StructuredRawDataConvertor {
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

    if (!format.pluralsViaNesting && format.pluralsViaSuffixesParser == null) {
      return null
    }

    if (!map.keys.all { it in allPluralKeywords }) {
      return null
    }

    if (map.size < 2) {
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
