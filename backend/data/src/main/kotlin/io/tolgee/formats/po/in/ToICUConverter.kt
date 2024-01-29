package io.tolgee.formats.po.`in`

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import io.tolgee.service.dataImport.processors.messageFormat.data.PluralData

class ToICUConverter(
  private val locale: ULocale,
  private val format: SupportedFormat?,
) {
  fun convert(message: String): String {
    val convertor =
      format?.paramConvertorFactory?.invoke()
        ?: SupportedFormat.C.paramConvertorFactory()
    return message.replace(convertor.regex) {
      convertor.convert(it)
    }
  }

  fun convertPoPlural(pluralForms: Map<Int, String>): String {
    val icuMsg = StringBuffer("{0, plural,\n")
    pluralForms.entries.forEach { (key, value) ->
      val example = findSuitableExample(key)
      val keyword = PluralRules.forLocale(locale).select(example.toDouble())
      icuMsg.append("$keyword {${convert(value)}}\n")
    }
    icuMsg.append("}")
    return icuMsg.toString()
  }

  private fun findSuitableExample(key: Int): Int {
    val examples = PluralData.DATA[locale.language]?.examples ?: PluralData.DATA["en"]!!.examples
    return examples.find { it.plural == key }?.sample ?: examples[0].sample
  }
}
