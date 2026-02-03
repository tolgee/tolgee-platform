package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.convertFloatToIcu
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.po.`in`.ParsedCLikeParam
import io.tolgee.formats.usesUnsupportedFeature

private val DEFAULT_NUMBER_MATCHER: (param: ParsedCLikeParam) -> Boolean = {
  it.specifier == "d" && !usesUnsupportedFeature(it)
}

class BaseToIcuPlaceholderConvertor(
  private val noTypeSpecifiers: Array<String?> = arrayOf(null, "s"),
  private val numberMatcher: (param: ParsedCLikeParam) -> Boolean = DEFAULT_NUMBER_MATCHER,
  private val validReplaceNumberMatcher: (param: ParsedCLikeParam) -> Boolean = DEFAULT_NUMBER_MATCHER,
) {
  private val parser = CLikeParameterParser()
  private var index = 0

  var pluralArgName: String? = null

  fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val parsed = parser.parse(matchResult) ?: return matchResult.value.escapeIcu(isInPlural)

    if (parsed.specifier == "%") {
      return "%"
    }

    index++

    val zeroIndexedArgNum = parsed.argNum?.toIntOrNull()?.minus(1)
    val name = parsed.argName ?: zeroIndexedArgNum?.toString() ?: ((index - 1).toString())

    val isNumber = numberMatcher(parsed)
    if (isInPlural && validReplaceNumberMatcher(parsed)) {
      val isFirstParam = zeroIndexedArgNum == 0 || index == 1
      if (isFirstParam) {
        pluralArgName = name
        return "#"
      }
    }

    if (isNumber) {
      return "{$name, number}"
    }

    if (usesUnsupportedFeature(parsed)) {
      return matchResult.value.escapeIcu(isInPlural)
    }

    when (parsed.specifier) {
      in noTypeSpecifiers -> return "{$name}"
      "e" -> return "{$name, number, scientific}"
      "f" -> return convertFloatToIcu(parsed, name) ?: matchResult.value.escapeIcu(isInPlural)
    }

    return matchResult.value.escapeIcu(isInPlural)
  }
}
