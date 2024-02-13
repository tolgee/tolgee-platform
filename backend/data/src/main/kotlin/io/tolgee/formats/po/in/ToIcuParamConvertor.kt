package io.tolgee.formats.po.`in`

interface ToIcuParamConvertor {
  fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String

  val regex: Regex
}
