package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.po.`in`.ParsedCLikeParam

class AppleToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  override val pluralArgName: String = "0"

  override val regex: Regex
    get() = APPLE_PLACEHOLDER_REGEX

  private val baseToIcuPlaceholderConvertor =
    BaseToIcuPlaceholderConvertor(
      noTypeSpecifiers = arrayOf("@"),
      numberMatcher = NUMBER_MATCHER,
      validReplaceNumberMatcher = NUMBER_MATCHER,
    )

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    return baseToIcuPlaceholderConvertor.convert(matchResult, isInPlural)
  }

  companion object {
    val APPLE_PLACEHOLDER_REGEX =
      """
      (?x)(
      %
      (?:(?<argnum>\d+)${"\\$"})?
      (?<flags>[-+\s0\#]+)?
      (?<width>\d+)?
      (?:\.(?<precision>\d+))?
      (?<length>ll|hh|h|l|z|j|t|L)?
      (?<specifier>@\w+@|[diuoxXfFeEgGaAcspn@l%])
      )
      """.trimIndent().toRegex()

    private val NUMBER_MATCHER = { it: ParsedCLikeParam ->
      it.length == "ll" && it.specifier == "d" && it.width == null && it.flags == null
    }
  }
}
