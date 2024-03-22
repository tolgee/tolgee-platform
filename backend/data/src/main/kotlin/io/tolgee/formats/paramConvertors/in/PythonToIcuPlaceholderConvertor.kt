package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor

class PythonToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  override val pluralArgName: String = "0"

  override val regex: Regex
    get() = PYTHON_PARAM_REGEX

  private val baseToIcuPlaceholderConvertor = BaseToIcuPlaceholderConvertor()

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    return baseToIcuPlaceholderConvertor.convert(matchResult, isInPlural)
  }

  companion object {
    val PYTHON_PARAM_REGEX =
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
  }
}
