package io.tolgee.formats.importCommon

import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.ToIcuPlaceholderConvertor
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
      return MessageConvertorResult(null, null)
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
      convertPlural(rawData, baseImportRawDataConverter)?.let {
        return it
      }
    }

    throw IllegalArgumentException("Unsupported type of message")
  }

  private fun convertPlural(
    rawData: Map<*, *>,
    baseImportRawDataConverter: BaseImportRawDataConverter,
  ): MessageConvertorResult? {
    val pluralArgName = "0"
    val converted =
      rawData.mapNotNull { (key, value) ->
        if (key !is String || value !is String?) {
          return null
        }
        if (value == null) {
          return@mapNotNull null
        }
        key to baseImportRawDataConverter.convertMessage(value, true)
      }.toMap().toIcuPluralString(optimize = optimizePlurals, argName = pluralArgName)

    return MessageConvertorResult(converted, pluralArgName)
  }
}