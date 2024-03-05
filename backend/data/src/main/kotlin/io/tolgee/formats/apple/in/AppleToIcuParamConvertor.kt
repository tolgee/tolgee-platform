package io.tolgee.formats.apple.`in`

import io.tolgee.formats.ToIcuParamConvertor
import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.usesUnsupportedFeature

class AppleToIcuParamConvertor() : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val regex: Regex
    get() = REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val parsed = parser.parse(matchResult) ?: return matchResult.value.escapeIcu(isInPlural)
    if (parsed.specifier == "%") {
      return "%"
    }

    index++
    val zeroIndexedArgNum = parsed.argNum?.toIntOrNull()?.minus(1)
    val name = zeroIndexedArgNum?.toString() ?: ((index - 1).toString())
    val isLld = parsed.length == "ll" && parsed.specifier == "d" && name == "0"

    if (isInPlural && isLld) {
      val isFirstParam = zeroIndexedArgNum == 0 || index == 1
      if (isFirstParam) {
        return "#"
      }
    }

    if (isLld) {
      return "{$name, number}"
    }

    if (usesUnsupportedFeature(parsed)) {
      return parsed.fullMatch.escapeIcu(isInPlural)
    }

    when (parsed.specifier) {
      "@" -> return "{$name}"
      "e" -> return "{$name, number, scientific}"
      "f" -> return convertFloatToIcu(parsed, name) ?: parsed.fullMatch.escapeIcu(isInPlural)
    }

    return parsed.fullMatch.escapeIcu(isInPlural)
  }

  companion object {
    val REGEX =
      """
      (?x)(
      %
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>[-+\s0\#]+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<length>ll|hh|h|l|z|j|t|L)?
      (?<specifier>@\w+@|[diuoxXfFeEgGaAcspn@l%])
      )
      """.trimIndent().toRegex()
  }
}
