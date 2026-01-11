package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor

class PythonToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  override val pluralArgName: String = "0"

  override val regex: Regex
    get() = PYTHON_PLACEHOLDER_REGEX

  private val baseToIcuPlaceholderConvertor = BaseToIcuPlaceholderConvertor()

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    return baseToIcuPlaceholderConvertor.convert(matchResult, isInPlural)
  }

  companion object {
    val PYTHON_PLACEHOLDER_REGEX =
      """
      (?x)(
      %
      (?:\((?<argname>[\w-]+)\))?
      (?<flags>[-+\s0\#]+)?
      (?<width>[\d*]+)?
      (?:\.(?<precision>\d+))?
      (?<length>[hlL])?
      (?<specifier>[diouxXeEfFgGcrs%])
      )
      """.trimIndent().toRegex()

    val PYTHON_DETECTION_REGEX =
      """
      (?x)(
      (^|\W+)%
      (?:\((?<argname>[\w-]+)\))?
      (?<flags>[-+0\#]+)?
      (?<width>[\d*]+)?
      (?:\.(?<precision>\d+))?
      (?<length>[hlL])?
      (?<specifier>[diouxXeEfFgGcrs%])
      )
      """.trimIndent().toRegex()
  }
}
