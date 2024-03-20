package io.tolgee.formats.importMessageFormat

import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.toIcuPluralString

/**
 * This class handles raw data to message conversion for formats, which store plural forms in form of map, where keys
 * are standard ICU plural keywords
 */
class GenericMapPluralImportRawDataConvertor(
  private val canContainIcu: Boolean = false,
  private val optimizePlurals: Boolean = false,
  private val toIcuPlaceholderConvertorFactory: (() -> ToIcuPlaceholderConvertor)?,
) : ImportMessageConvertor {
  @Suppress("UNCHECKED_CAST")
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    if (rawData == null) {
      return MessageConvertorResult(null, false)
    }

    val baseImportRawDataConverter =
      BaseImportRawDataConverter(
        canContainIcu = canContainIcu,
        toIcuPlaceholderConvertorFactory = toIcuPlaceholderConvertorFactory,
        convertPlaceholders = convertPlaceholders,
        isProjectIcuEnabled = isProjectIcuEnabled,
      )

    baseImportRawDataConverter.tryConvertStringValue(rawData)?.let {
      return it
    }

    if (rawData is Map<*, *>) {
      val rawDataMap = rawData as Map<String, String>
      val converted = convertPlural(rawDataMap, baseImportRawDataConverter)
      return MessageConvertorResult(converted, true)
    }

    throw IllegalArgumentException("Unsupported type of message")
  }

  private fun convertPlural(
    rawData: Map<String, String>,
    baseImportRawDataConverter: BaseImportRawDataConverter,
  ): String {
    return rawData.mapNotNull {
      it.key to baseImportRawDataConverter.convertMessage(it.value, true)
    }.toMap().toIcuPluralString(optimize = optimizePlurals, argName = "0")
  }
}
