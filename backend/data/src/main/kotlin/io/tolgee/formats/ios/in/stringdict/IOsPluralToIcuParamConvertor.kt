package io.tolgee.formats.ios.`in`.stringdict

import io.tolgee.formats.ios.`in`.strings.IOsToIcuParamConvertor.Companion.REGEX
import io.tolgee.formats.po.`in`.CLikeParameterParser
import io.tolgee.formats.po.`in`.ToIcuParamConvertor

class IOsPluralToIcuParamConvertor() : ToIcuParamConvertor {
  private val parser = CLikeParameterParser()

  override val regex: Regex
    get() = REGEX

  override fun convert(matchResult: MatchResult): String {
    val parsed = parser.parse(matchResult)
    if (parsed?.specifier == "%") {
      return "%"
    }

    return "#"
  }
}
