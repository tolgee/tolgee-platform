package io.tolgee.formats.ios.strings

import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.po.`in`.ToIcuParamConvertor

class IOsToIcuParamConvertor() : ToIcuParamConvertor {
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
      "d", "u", "i" -> return "{$name, number}"
      "e", "E" -> return "{$name, number, scientific}"
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
      (?<flags>[-+\s0\#]+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<length>h|hh|l|ll|z|j|t|L)?
      (?<specifier>[diuoxXfFeEgGaAcspn@l%])
      )
      """.trimIndent().toRegex()
  }
}
