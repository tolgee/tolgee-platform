package io.tolgee.formats.po.`in`

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import io.tolgee.formats.FormsToIcuPluralConvertor
import io.tolgee.formats.convertMessage
import io.tolgee.formats.pluralData.PluralData
import io.tolgee.formats.po.SupportedFormat

class PoToICUConverter(
  private val locale: ULocale,
  private val format: SupportedFormat?,
) {
  fun convert(
    message: String,
    isPlural: Boolean = false,
  ): String {
    return convertMessage(message, isPlural) {
      format?.paramConvertorFactory?.invoke()
        ?: SupportedFormat.C.paramConvertorFactory()
    }
  }

  fun convertPoPlural(pluralForms: Map<Int, String>): String {
    val forms =
      pluralForms.entries.associate { (key, value) ->
        val example = findSuitableExample(key)
        val keyword = PluralRules.forLocale(locale).select(example.toDouble())
        keyword to convert(value, true)
      }
    return FormsToIcuPluralConvertor(forms).convert()
  }

  private fun findSuitableExample(key: Int): Int {
    val examples = PluralData.DATA[locale.language]?.examples ?: PluralData.DATA["en"]!!.examples
    return examples.find { it.plural == key }?.sample ?: examples[0].sample
  }
}
