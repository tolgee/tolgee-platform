package io.tolgee.formats

interface ToIcuPlaceholderConvertor {
  fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String

  val regex: Regex

  val pluralArgName: String?
}
