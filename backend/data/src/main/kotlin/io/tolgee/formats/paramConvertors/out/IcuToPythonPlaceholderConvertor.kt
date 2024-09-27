package io.tolgee.formats.paramConvertors.out

import com.ibm.icu.text.MessagePattern
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil
import io.tolgee.formats.escapePercentSign

class IcuToPythonPlaceholderConvertor : FromIcuPlaceholderConvertor {
  private var argIndex = -1

  override fun convert(node: MessagePatternUtil.ArgNode): String {
    argIndex++
    val argNumString = getArgNameString(node)
    val type = node.argType

    if (type == MessagePattern.ArgType.SIMPLE) {
      when (node.typeName) {
        "number" -> return convertNumber(node)
      }
    }

    return "%${argNumString}s"
  }

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String {
    return escapePercentSign(node.getText(keepEscaping))
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String {
    return "%($argName)d"
  }

  private fun convertNumber(node: MessagePatternUtil.ArgNode): String {
    if (node.simpleStyle?.trim() == "scientific") {
      return "%${getArgNameString(node)}e"
    }
    val precision = getPrecision(node)
    if (precision == 6) {
      return "%${getArgNameString(node)}f"
    }
    if (precision != null) {
      return "%${getArgNameString(node)}.${precision}f"
    }

    return "%${getArgNameString(node)}d"
  }

  private fun getPrecision(node: MessagePatternUtil.ArgNode): Int? {
    val precisionMatch = ICU_PRECISION_REGEX.matchEntire(node.simpleStyle ?: "")
    precisionMatch ?: return null
    return precisionMatch.groups["precision"]?.value?.length
  }

  private fun getArgNameString(node: MessagePatternUtil.ArgNode): String {
    return "(${node.name})"
  }

  companion object {
    val ICU_PRECISION_REGEX = """.*\.(?<precision>0+)""".toRegex()
  }
}
