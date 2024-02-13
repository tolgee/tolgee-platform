package io.tolgee.formats.android.`in`

import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.po.`in`.ToIcuParamConvertor
import io.tolgee.formats.usesUnsupportedFeature

class JavaToIcuParamConvertor() : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val regex: Regex
    get() = REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    index++
    val parsed = parser.parse(matchResult) ?: return escapeIcu(matchResult.value, isInPlural)

    if (usesUnsupportedFeature(parsed)) {
      return escapeIcu(parsed.fullMatch, isInPlural)
    }

    if (parsed.specifier == "%") {
      return "%"
    }

    val zeroIndexedArgNum = parsed.argNum?.toIntOrNull()?.minus(1)
    val name = zeroIndexedArgNum?.toString() ?: ((index - 1).toString())
    val isValidPluralReplaceNumber = parsed.specifier == "d" && name == "0"

    if (isInPlural && isValidPluralReplaceNumber) {
      return "#"
    }

    if (isValidPluralReplaceNumber) {
      return "{$name, number}"
    }

    when (parsed.specifier) {
      "s" -> return "{$name}"
      "e" -> return "{$name, number, scientific}"
      "d" -> return "{$name, number}"
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
      (?<flags>[-\#+\s0,(]+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<specifier>[bBhHsScCdoxXeEfgGaAtT%nRrDF])
      )
      """.trimIndent().toRegex()
  }
}
