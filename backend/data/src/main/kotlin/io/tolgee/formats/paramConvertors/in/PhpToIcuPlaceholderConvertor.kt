package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.usesUnsupportedFeature

class PhpToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val regex: Regex
    get() = PHP_PLACEHOLDER_REGEX

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
    val zeroIndexedArgNum = parsed.argNum?.toIntOrNull()?.minus(1)?.toString()
    val name = zeroIndexedArgNum ?: ((index - 1).toString())

    when (parsed.specifier) {
      "s" -> return "{$name}"
      "d" -> return "{$name, number}"
      "e" -> return "{$name, number, scientific}"
      "f" -> return convertFloatToIcu(parsed, name) ?: matchResult.value.escapeIcu(isInPlural)
    }

    return matchResult.value.escapeIcu(isInPlural)
  }

  companion object {
    val PHP_PLACEHOLDER_REGEX =
      """
      (?x)(
      %
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>[\-+\s0']+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<specifier>[bcdeEfFgGhHosuxX%])
      )
      """.trimIndent().toRegex()
  }
}
