package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor

class PhpToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  override val pluralArgName: String = "0"

  override val regex: Regex
    get() = PHP_PLACEHOLDER_REGEX

  private val baseToIcuPlaceholderConvertor = BaseToIcuPlaceholderConvertor()

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    return baseToIcuPlaceholderConvertor.convert(matchResult, isInPlural)
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

    val PHP_DETECTION_REGEX =
      """
      (?x)(
      (^|\W+)%
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>[\-+0']+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<specifier>[bcdeEfFgGhHosuxX%])
      )
      """.trimIndent().toRegex()
  }
}
