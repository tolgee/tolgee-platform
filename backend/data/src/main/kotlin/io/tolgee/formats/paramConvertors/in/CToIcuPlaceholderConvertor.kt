package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.usesUnsupportedFeature

class CToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val pluralArgName: String = "0"

  override val regex: Regex
    get() = C_PARAM_REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
    isSingleParam: Boolean,
  ): String {
    index++
    val parsed = parser.parse(matchResult) ?: return matchResult.value.escapeIcu(isInPlural)

    if (usesUnsupportedFeature(parsed)) {
      return matchResult.value.escapeIcu(isInPlural)
    }

    if (parsed.specifier == "%") {
      return "%"
    }

    val zeroIndexedArgNum = parsed.argNum?.toIntOrNull()?.minus(1)
    val name = zeroIndexedArgNum?.toString() ?: ((index - 1).toString())

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
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>[-+\s0\#]+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<length>hh|h|l|ll|j|z|t|L)?
      (?<specifier>[diuoxXfFeEgGaAcspn%])
      )
      """.trimIndent().toRegex()
  }
}
