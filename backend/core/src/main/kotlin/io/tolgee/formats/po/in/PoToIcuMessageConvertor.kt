package io.tolgee.formats.po.`in`

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import io.tolgee.formats.DEFAULT_PLURAL_ARGUMENT_NAME
import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.MessageConvertorResult
import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.importCommon.BaseImportRawDataConverter
import io.tolgee.formats.importCommon.ImportMessageConvertor
import io.tolgee.formats.pluralData.PluralData

class PoToIcuMessageConvertor(
  private val canContainIcu: Boolean = false,
  private val paramConvertorFactory: (() -> ToIcuPlaceholderConvertor)?,
) : ImportMessageConvertor {
  override fun convert(
    rawData: Any?,
    languageTag: String,
    convertPlaceholders: Boolean,
    isProjectIcuEnabled: Boolean,
  ): MessageConvertorResult {
    val baseImportRawDataConverter =
      BaseImportRawDataConverter(
        canContainIcu = canContainIcu,
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
    val formsConverted =
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
    val argName =
      formsConverted.values.firstNotNullOfOrNull { it.pluralArgName }
        ?: DEFAULT_PLURAL_ARGUMENT_NAME
    val form =
      formsConverted
        .mapNotNull {
          val message = it.value.message ?: return@mapNotNull null
          it.key to message
        }.toMap()
    val converted = FormsToIcuPluralConvertor(form, addNewLines = true, argName = argName).convert()
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
  ): MessageConvertorResult {
    return convertMessage(
      message,
      isInPlural,
      convertPlaceholders,
      isProjectIcuEnabled,
      convertorFactory = paramConvertorFactory,
    )
  }
}
