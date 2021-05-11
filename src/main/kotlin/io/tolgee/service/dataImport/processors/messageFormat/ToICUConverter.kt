package io.tolgee.service.dataImport.processors.messageFormat

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import io.tolgee.service.dataImport.processors.messageFormat.data.PluralData

class ToICUConverter(
        val locale: ULocale,
        val format: SupportedFormat?
) {

    fun convertPoPlural(pluralForms: Map<Int, String>): String {
        return when (format) {
            SupportedFormat.PHP -> convertPhpPlural(pluralForms)
            else -> ""
        }
    }

    fun convert(message: String): String {
        return when (format) {
            SupportedFormat.PHP -> convertPhp(message)
            else -> ""
        }
    }

    private fun convertPhpPlural(pluralForms: Map<Int, String>): String {
        val icuMsg = StringBuffer("{0, plural,\n")
        pluralForms.entries.forEach { (key, value) ->
            val example = findSuitableExample(key)
            val keyword = PluralRules.forLocale(locale).select(example.toDouble())
            icuMsg.append("$keyword {${value}}\n")
        }
        icuMsg.append("}")
        return icuMsg.replace("([^%]|^)%[d|s]".toRegex(), "$1{0}")
    }

    private fun findSuitableExample(key: Int): Int {
        val examples = PluralData.DATA[locale.language]?.examples ?: PluralData.DATA["en"]!!.examples
        return examples.find { it.plural == key }?.sample ?: examples[0].sample
    }

    private fun convertPhp(message: String): String {
        var result = message
        var iterations = 0
        var keyIdx = 0
        val regexp = "(.*?[^%]|^)%(?:(\\d+)\\\$)?([ds])(.*)".toRegex()
        while (result.matches(regexp)) {
            //just to be sure to avoid infinite loop
            if (iterations > 1000) {
                break
            }

            result = result.replace(regexp) {
                var paramName = keyIdx
                val type = it.groups[3]!!.value
                try {
                    it.groups[2]?.let { grp ->
                        paramName = grp.value.toInt() - 1
                    } ?: let {
                        paramName = keyIdx
                    }
                } catch (e: NumberFormatException) {
                    paramName = keyIdx
                }
                val typeStr = if (type == "d") ", number" else ""
                "${it.groups[1]!!.value}{${paramName}${typeStr}}${it.groups[4]!!.value}"
            }
            keyIdx++
            iterations++
        }

        return result
    }
}
