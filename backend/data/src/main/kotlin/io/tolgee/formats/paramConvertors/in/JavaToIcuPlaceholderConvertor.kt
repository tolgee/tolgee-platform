package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.po.`in`.CLikeParameterParser

class JavaToIcuPlaceholderConvertor() : ToIcuPlaceholderConvertor {
  private val parser = CLikeParameterParser()
  private var index = 0

  override val pluralArgName: String = "0"

  override val regex: Regex
    get() = JAVA_PLACEHOLDER_REGEX

  private val baseToIcuPlaceholderConvertor = BaseToIcuPlaceholderConvertor()

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    return baseToIcuPlaceholderConvertor.convert(matchResult, isInPlural)
  }

  companion object {
    val JAVA_PLACEHOLDER_REGEX =
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
