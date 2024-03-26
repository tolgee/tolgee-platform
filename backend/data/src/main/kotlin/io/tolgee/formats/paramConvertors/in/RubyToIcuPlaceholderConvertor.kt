package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.usesUnsupportedFeature

class RubyToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val regex: Regex
    get() = RUBY_PLACEHOLDER_REGEX

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
    val name = parsed.argName ?: zeroIndexedArgNum ?: ((index - 1).toString())

    when (parsed.specifier) {
      null -> return "{$name}"
      "s" -> return "{$name}"
      "d" -> return "{$name, number}"
      "e" -> return "{$name, number, scientific}"
      "f" -> return convertFloatToIcu(parsed, name) ?: matchResult.value.escapeIcu(isInPlural)
    }

    return matchResult.value.escapeIcu(isInPlural)
  }

  companion object {
    val RUBY_PLACEHOLDER_REGEX =
"""
(?x)(
%
(?:(?<argnum>\d+)?${"\\$"}|<(?<argname>\w+)>)?
(?<flags>[+\-\s0\#*]+)?
(?<width>\d+)?
(?:\.(?<precision>\d+))?
(?<specifier>[bBdiouxXeEfgGaAcps%])|
%\{(?<argname2>\w+)}
)
""".trimIndent().toRegex()
  }
}
