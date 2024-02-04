package io.tolgee.formats.ios.`in`

import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.po.`in`.ToIcuParamConvertor
import io.tolgee.formats.usesUnsupportedFeature

class IOsToIcuParamConvertor() : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val regex: Regex
    get() = REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val parsed = parser.parse(matchResult) ?: return escapeIcu(matchResult.value, isInPlural)
    if (parsed.specifier == "%") {
      return "%"
    }

    index++
    val zeroIndexedArgNum = parsed.argNum?.toIntOrNull()?.minus(1)
    val name = zeroIndexedArgNum?.toString() ?: ((index - 1).toString())
    val isLi = matchResult.value == "%li"

    if (isInPlural && isLi) {
      val isFirstParam = zeroIndexedArgNum == 0 || index == 1
      if (isFirstParam) {
        return "#"
      }
    }

    if (isLi) {
      return "{$name, number}"
    }

    if (usesUnsupportedFeature(parsed)) {
      return escapeIcu(parsed.fullMatch, isInPlural)
    }

    when (parsed.specifier) {
      "@" -> return "{$name}"
      "e" -> return "{$name, number, scientific}"
      "f" -> return convertFloatToIcu(parsed, name) ?: escapeIcu(parsed.fullMatch, isInPlural)
    }

    return escapeIcu(parsed.fullMatch, isInPlural)
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
      (?<length>h|hh|l|ll|z|j|t|L)?
      (?<specifier>@\w+@|[diuoxXfFeEgGaAcspn@l%])
      )
      """.trimIndent().toRegex()
  }
}
