package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor

class RubyToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  override val regex: Regex
    get() = RUBY_PLACEHOLDER_REGEX

  private val baseToIcuPlaceholderConvertor =
    BaseToIcuPlaceholderConvertor(
      numberMatcher = { it.specifier in setOf("d") },
      validReplaceNumberMatcher = { it.specifier in setOf("d", null) },
    )

  override val pluralArgName: String?
    get() = baseToIcuPlaceholderConvertor.pluralArgName

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    return baseToIcuPlaceholderConvertor.convert(matchResult, isInPlural)
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

    val RUBY_DETECTION_REGEX =
      """
      (?x)(
      (^|\W+)%
      (?:(?<argnum>\d+)?${"\\$"}|<(?<argname>\w+)>)?
      (?<flags>[+\-0\#*]+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<specifier>[bBdiouxXeEfgGaAcps%])|
      %\{(?<argname2>\w+)}
      )
      """.trimIndent().toRegex()
  }
}
