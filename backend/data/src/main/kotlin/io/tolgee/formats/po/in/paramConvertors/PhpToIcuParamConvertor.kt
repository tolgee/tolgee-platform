package io.tolgee.formats.po.`in`.paramConvertors

import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.po.`in`.ToIcuParamConvertor

class PhpToIcuParamConvertor : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val regex: Regex
    get() = REGEX

  override fun convert(matchResult: MatchResult): String {
    val parsed = parser.parse(matchResult)
    if (parsed?.specifier == "%") {
      return "%"
    }

    index++
    val zeroIndexedArgNum = parsed?.argNum?.toIntOrNull()?.minus(1)?.toString()
    val name = zeroIndexedArgNum ?: ((index - 1).toString())

    when (parsed?.specifier) {
      "s" -> return "{$name}"
      "d" -> return "{$name, number}"
      "u" -> return "{$name, number}"
      "e" -> return "{$name, number, scientific}"
      "E" -> return "{$name, number, scientific}"
      "f" -> return convertFloatToIcu(parsed, name)
    }

    return "{$name}"
  }

  companion object {
    val REGEX =
      """
      (?x)(
      %
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>(?:[-+\s0]|'.)+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<specifier>[bcdeEfFgGhHosuxX%])
      )
      """.trimIndent().toRegex()
  }
}
