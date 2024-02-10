package io.tolgee.formats.po.`in`

import io.tolgee.formats.IcuMessageEscaper

interface ToIcuParamConvertor {
  fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String

  val regex: Regex

  /**
   * When keeping the param as it is, we still need to escape it so it doesn't get interpreted as ICU syntax
   */
  fun escapeIcu(
    paramString: String,
    isInPlural: Boolean,
  ): String {
    return IcuMessageEscaper(paramString, isInPlural).escaped
  }
}
