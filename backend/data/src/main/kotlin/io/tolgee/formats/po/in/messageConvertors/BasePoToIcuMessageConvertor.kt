package io.tolgee.formats.po.`in`.messageConvertors

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.importCommon.BaseImportRawDataConverter
import io.tolgee.formats.pluralData.PluralData

class BasePoToIcuMessageConvertor(private val paramConvertorFactory: () -> ToIcuPlaceholderConvertor) {
  fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    val baseImportRawDataConverter =
      BaseImportRawDataConverter(
        canContainIcu = false,
        toIcuPlaceholderConvertorFactory = paramConvertorFactory,
        convertPlaceholders = convertPlaceholders,
        isProjectIcuEnabled = isProjectIcuEnabled,
      )

    baseImportRawDataConverter.tryConvertStringValue(rawData)?.let {
      return it
    }

    if (rawData is Map<*, *>) {
      val converted = convertPoPlural(rawData, languageTag, convertPlaceholders, isProjectIcuEnabled)
      return converted
    }

    throw IllegalArgumentException("Unsupported type of message")
  }

  private fun convertPoPlural(
    possiblePluralForms: Map<*, *>,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    val forms =
      possiblePluralForms.entries.associate { (formNumPossibleString, value) ->
        val formNumber = (formNumPossibleString as? Int) ?: (formNumPossibleString as? String)?.toIntOrNull()
        if (formNumber !is Int || value !is String) {
          throw IllegalArgumentException("Plural forms must be a map of Int to String")
        }
        val locale = getULocaleFromTag(languageTag)
        val example = findSuitableExample(formNumber, locale)
        val keyword = PluralRules.forLocale(locale).select(example.toDouble())
        keyword to (convert(value, true, convertPlaceholders, isProjectIcuEnabled))
      }
    val argName = "0"
    val converted = FormsToIcuPluralConvertor(forms, addNewLines = true, argName = argName).convert()
    return MessageConvertorResult(converted, argName)
  }

  private fun findSuitableExample(
    key: Int,
    locale: ULocale,
  ): Int {
    val examples = PluralData.DATA[locale.language]?.examples ?: PluralData.DATA["en"]!!.examples
    return examples.find { it.plural == key }?.sample ?: examples[0].sample
  }

  private fun convert(
    message: String,
    isInPlural: Boolean = false,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): String {
    return convertMessage(
      message,
      isInPlural,
      convertPlaceholders,
      isProjectIcuEnabled,
      convertorFactory = paramConvertorFactory,
    )
  }
}
