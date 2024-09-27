package io.tolgee.formats

interface ToIcuPlaceholderConvertor {
  fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String

  val regex: Regex

  val pluralArgName: String?

  val customValuesModifier: ((customValues: MutableMap<String, Any?>, memory: MutableMap<String, Any?>) -> Unit)?
    get() = null
}
