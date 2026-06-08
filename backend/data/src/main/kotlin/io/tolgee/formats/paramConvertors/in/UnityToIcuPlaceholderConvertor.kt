package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.escaping.ForceIcuEscaper

/**
 * Converts Unity Smart Strings (SmartFormat) placeholder syntax to ICU. Plain `{name}`/`{0}` map to
 * the identical ICU placeholder; `{}` inside a plural maps to `#`; backslash-escaped literals
 * (`\{`, `\}`, `\\`, `\|`) become ICU literals. Any placeholder carrying a formatter suffix, property
 * access or `choose`/list/conditional syntax is preserved verbatim as an ICU literal (lossy, by design).
 */
class UnityToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  override val pluralArgName: String? = null

  override val regex: Regex = UNITY_PLACEHOLDER_REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val value = matchResult.value
    if (value.startsWith("\\")) {
      return convertEscaped(value[1])
    }
    val inner = value.substring(1, value.length - 1)
    if (inner.isEmpty()) {
      return "#"
    }
    if (inner.matches(SIMPLE_ARG)) {
      return "{$inner}"
    }
    return ForceIcuEscaper(value).escaped
  }

  private fun convertEscaped(char: Char): String {
    if (char == '{') return "'{'"
    if (char == '}') return "'}'"
    return char.toString()
  }

  companion object {
    private val UNITY_PLACEHOLDER_REGEX = """\\.|\{[^{}]*}""".toRegex()
    private val SIMPLE_ARG = """[A-Za-z0-9_]+""".toRegex()
  }
}
