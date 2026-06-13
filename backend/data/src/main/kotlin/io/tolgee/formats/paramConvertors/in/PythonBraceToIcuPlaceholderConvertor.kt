package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.escapeIcu

class PythonBraceToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  private var autoIndex = 0
  private var placeholderCount = 0

  override val pluralArgName: String = "0"

  override val regex: Regex
    get() = PYTHON_BRACE_PLACEHOLDER_REGEX

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val value = matchResult.value
    if (value == "{{") return "{".escapeIcu(isInPlural)
    if (value == "}}") return "}".escapeIcu(isInPlural)

    val field = matchResult.groups[GROUP_FIELD]?.value ?: ""
    val name = resolveName(field) ?: return value.escapeIcu(isInPlural)
    placeholderCount++

    val conversion = matchResult.groups[GROUP_CONVERSION]?.value
    if (conversion != null) {
      // not supported
      return value.escapeIcu(isInPlural)
    }

    val formatSpec = matchResult.groups[GROUP_FORMAT_SPEC]?.value
    if (formatSpec == null) {
      return "{$name}"
    }

    val numberSpec = NUMBER_SPEC_REGEX.matchEntire(formatSpec) ?: return value.escapeIcu(isInPlural)
    val type = numberSpec.groups[GROUP_TYPE]!!.value
    val precision = numberSpec.groups[GROUP_PRECISION]?.value?.toInt()

    if (type == "e") return "{$name, number, scientific}"

    if (type == "f" || type == "F") {
      val fractionDigits = precision ?: DEFAULT_FLOAT_PRECISION
      if (fractionDigits > MAX_FLOAT_PRECISION) return value.escapeIcu(isInPlural)
      if (fractionDigits == 0) return "{$name, number}"
      return "{$name, number, .${"0".repeat(fractionDigits)}}"
    }

    val isFirstParam = name == "0" || placeholderCount == 1
    if (isInPlural && isFirstParam) return "#"
    return "{$name, number}"
  }

  private fun resolveName(field: String): String? {
    if (field.isEmpty()) return (autoIndex++).toString()
    if (field.all { it.isDigit() }) return field
    if (IDENTIFIER_REGEX.matches(field)) return field
    return null
  }

  companion object {
    private const val GROUP_FIELD = "field"
    private const val GROUP_CONVERSION = "conversion"
    private const val GROUP_FORMAT_SPEC = "formatspec"
    private const val GROUP_TYPE = "type"
    private const val GROUP_PRECISION = "precision"

    private const val DEFAULT_FLOAT_PRECISION = 6
    private const val MAX_FLOAT_PRECISION = 50

    private val IDENTIFIER_REGEX = """[A-Za-z_][A-Za-z0-9_]*""".toRegex()

    private val NUMBER_SPEC_REGEX = """(?:\.(?<precision>\d+))?(?<type>[dnefF])""".toRegex()

    val PYTHON_BRACE_PLACEHOLDER_REGEX =
      """
      (?x)(
      \{\{
      |
      }}
      |
      \{
      (?<field>[^{}!:]*)
      (?<conversion>![rsa])?
      (?::(?<formatspec>[^{}]*))?
      }
      )
      """.trimIndent().toRegex()

    val PYTHON_BRACE_DETECTION_REGEX =
      """
      (?x)(
      (^|\W+)
      \{
      (?<field>[^{}!:]*)
      (?<conversion>![rsa])?
      (?::(?<formatspec>[^{}]*))?
      }
      )
      """.trimIndent().toRegex()
  }
}
