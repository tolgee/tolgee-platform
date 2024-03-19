package io.tolgee.formats

interface ToIcuParamConvertor {
  fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String

  val regex: Regex
}
