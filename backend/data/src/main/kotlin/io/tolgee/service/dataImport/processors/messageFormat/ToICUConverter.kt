package io.tolgee.service.dataImport.processors.messageFormat

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.messageFormat.data.PluralData

class ToICUConverter(
  private val locale: ULocale,
  private val format: SupportedFormat?,
  private val context: FileProcessorContext,
) {
  companion object {
    val PHP_PARAM_REGEX =
      """
      (?x)(
      %
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>(?:[-+\s0]|'.)+)?
      (?<width>\d+)?
      (?<precision>.\d+)?
      (?<specifier>[bcdeEfFgGhHosuxX%])
      )
      """.trimIndent().toRegex()

    val C_PARAM_REGEX =
      """
      (?x)(
      %
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>[-+\s0\#]+)?
      (?<width>\d+)?
      (?<precision>.\d+)?
      (?<length>hh|h|l|ll|j|z|t|L)?
      (?<specifier>[diuoxXfFeEgGaAcspn%])
      )
      """.trimIndent().toRegex()

    val PYTHON_PARAM_REGEX =
      """
      (?x)(
      %
      (?:\((?<argname>[\w-]+)\))?
      (?<flags>[-+\s0\#]+)?
      (?<width>[\d*]+)?
      (?<precision>.[\d*]+)?
      (?<length>[hlL])?
      (?<specifier>[diouxXeEfFgGcrs%])
      )
      """.trimIndent().toRegex()

    val PHP_NUMBER_SPECIFIERS = "dfeEfFgGhH"
    val C_NUMBER_SPECIFIERS = "diuoxXfFeEgG"
    val PYTHON_NUMBER_SPECIFIERS = "diouxXeEfFgG"
  }

  fun convert(message: String): String {
    return when (format) {
      SupportedFormat.PHP -> convertPhp(message)
      SupportedFormat.C -> convertC(message)
      SupportedFormat.PYTHON -> convertPython(message)
      else -> convertC(message)
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

  private fun convertPhp(message: String): String {
    return convertCLike(message, PHP_PARAM_REGEX, PHP_NUMBER_SPECIFIERS)
  }

  private fun convertC(message: String): String {
    return convertCLike(message, C_PARAM_REGEX, C_NUMBER_SPECIFIERS)
  }

  private fun convertPython(message: String): String {
    return convertCLike(message, PYTHON_PARAM_REGEX, PYTHON_NUMBER_SPECIFIERS)
  }

  private fun convertCLike(
    message: String,
    regex: Regex,
    numberSpecifiers: String,
  ): String {
    var result = message
    var keyIdx = 0

    result =
      result.replace(regex) {
        var paramName = keyIdx.toString()
        val specifier = it.groups["specifier"]!!.value

        if (specifier == "%") {
          return@replace "%"
        }

        it.groups.getGroupOrNull("argnum")?.let { grp ->
          paramName = (grp.value.toInt() - 1).toString()
        }

        it.groups.getGroupOrNull("argname")?.let { grp ->
          paramName = grp.value
        }

        val typeStr = if (numberSpecifiers.contains(specifier)) ", number" else ""
        keyIdx++
        "{${paramName}$typeStr}"
      }

    return result
  }

  fun MatchGroupCollection.getGroupOrNull(name: String): MatchGroup? {
    try {
      return this[name]
    } catch (e: IllegalArgumentException) {
      if (e.message?.contains("No group with name") != true) {
        throw e
      }
      return null
    }
  }
}
