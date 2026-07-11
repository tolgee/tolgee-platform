package io.tolgee.formats.paramConvertors.out

import com.ibm.icu.text.MessagePattern
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil
import io.tolgee.formats.escapePythonBraces

class IcuToPythonBracePlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String {
    if (node.argType == MessagePattern.ArgType.SIMPLE && node.typeName == "number") {
      return convertNumber(node)
    }

    return "{${node.name}}"
  }

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String {
    return escapePythonBraces(node.getText(keepEscaping))
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String {
    if (argName != null) return "{$argName:d}"
    return "{:d}"
  }

  private fun convertNumber(node: MessagePatternUtil.ArgNode): String {
    if (node.simpleStyle.trim() == "scientific") {
      return "{${node.name}:e}"
    }

    val precision = node.getPrecision()
    if (precision == DEFAULT_FLOAT_PRECISION) {
      return "{${node.name}:f}"
    }
    if (precision != null) {
      return "{${node.name}:.${precision}f}"
    }

    return "{${node.name}:d}"
  }

  private fun MessagePatternUtil.ArgNode.getPrecision(): Int? {
    val precisionMatch = ICU_PRECISION_REGEX.matchEntire(this.simpleStyle ?: "") ?: return null
    return precisionMatch.groups["precision"]?.value?.length
  }

  companion object {
    private const val DEFAULT_FLOAT_PRECISION = 6

    val ICU_PRECISION_REGEX = """.*\.(?<precision>0+)""".toRegex()
  }
}
