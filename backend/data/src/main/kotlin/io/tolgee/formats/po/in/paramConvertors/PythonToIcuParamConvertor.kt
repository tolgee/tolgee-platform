package io.tolgee.formats.po.`in`

import io.tolgee.formats.convertFloatToIcu

class PythonToIcuParamConvertor : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()

  override val regex: Regex
    get() = PYTHON_PARAM_REGEX

  override fun convert(matchResult: MatchResult): String {
    val parsed = parser.parse(matchResult)
    if (parsed?.specifier == "%") {
      return "%"
    }

    val argName = parsed?.argName ?: throw IllegalArgumentException("Python spec requires named arguments")

    when (parsed.specifier) {
      "d", "i", "u", "g", "G" -> return "{$argName, number}"
      "f", "F" -> return convertFloatToIcu(parsed, argName)
      "e", "E" -> return "{$argName, number, scientific}"
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
