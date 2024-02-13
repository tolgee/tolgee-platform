package io.tolgee.formats.po.`in`

import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.escapeIcu

class PythonToIcuParamConvertor : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()

  override val regex: Regex
    get() = PYTHON_PARAM_REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val parsed = parser.parse(matchResult) ?: return matchResult.value.escapeIcu(isInPlural)
    if (parsed.specifier == "%") {
      return "%"
    }

    val argName = parsed.argName ?: throw IllegalArgumentException("Python spec requires named arguments")

    when (parsed.specifier) {
      "d" -> return "{$argName, number}"
      "f" -> return convertFloatToIcu(parsed, argName) ?: return matchResult.value.escapeIcu(isInPlural)
      "e" -> return "{$argName, number, scientific}"
    }

    return "{$argName}"
  }

  companion object {
    val PYTHON_PARAM_REGEX =
      """
      (?x)(
      %
      (?:\((?<argname>[\w-]+)\))?
      (?<flags>[-+\s0\#]+)?
      (?<width>[\d*]+)?
      (?:\.(?<precision>\d+))?
      (?<length>[hlL])?
      (?<specifier>[diouxXeEfFgGcrs%])
      )
      """.trimIndent().toRegex()
  }
}
