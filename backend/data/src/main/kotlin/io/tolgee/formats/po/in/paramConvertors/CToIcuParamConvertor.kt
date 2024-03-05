package io.tolgee.formats.po.`in`.paramConvertors

import io.tolgee.formats.ToIcuParamConvertor
import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.usesUnsupportedFeature

class CToIcuParamConvertor : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val regex: Regex
    get() = C_PARAM_REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val parsed = parser.parse(matchResult) ?: return matchResult.value.escapeIcu(isInPlural)

    if (usesUnsupportedFeature(parsed)) {
      return matchResult.value.escapeIcu(isInPlural)
    }

    if (parsed.specifier == "%") {
      return "%"
    }

    index++
    val name = ((index - 1).toString())

    when (parsed.specifier) {
      "s" -> return "{$name}"
      "d" -> return "{$name, number}"
      "e" -> return "{$name, number, scientific}"
      "f" -> return convertFloatToIcu(parsed, name) ?: parsed.fullMatch.escapeIcu(isInPlural)
    }

    return matchResult.value.escapeIcu(isInPlural)
  }

  companion object {
    val C_PARAM_REGEX =
      """
      (?x)(
      %
      (?<flags>[-+\s0\#]+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<length>hh|h|l|ll|j|z|t|L)?
      (?<specifier>[diuoxXfFeEgGaAcspn%])
      )
      """.trimIndent().toRegex()
  }
}
