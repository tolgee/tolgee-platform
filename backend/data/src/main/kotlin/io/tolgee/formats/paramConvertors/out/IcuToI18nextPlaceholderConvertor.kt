package io.tolgee.formats.paramConvertors.out

import com.ibm.icu.text.MessagePattern
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil

class IcuToI18nextPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String {
    val type = node.argType

    if (type == MessagePattern.ArgType.SIMPLE) {
      when (node.typeName) {
        "number" -> return convertNumber(node)
      }
    }

    return "{{${node.name}}}"
  }

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String {
    // We should escape {{ and $t, but there doesn't seem to be a documented
    // way how to escape either {{ or $t in i18next
    return node.getText(keepEscaping)
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String {
    return "{{$argName, number}}"
  }

  private fun convertNumber(node: MessagePatternUtil.ArgNode): String {
    if (node.simpleStyle.trim() == "scientific") {
      return "{{${node.name}, number}}"
    }
    val precision = node.getPrecision()
    if (precision != null) {
      return "{{${node.name}, number(minimumFractionDigits: $precision; maximumFractionDigits: $precision)}}"
    }

    return "{{${node.name}, number}}"
  }

  private fun MessagePatternUtil.ArgNode.getPrecision(): Int? {
    val precisionMatch = ICU_PRECISION_REGEX.matchEntire(this.simpleStyle)
    precisionMatch ?: return null
    return precisionMatch.groups["precision"]?.value?.length
  }

  companion object {
    val ICU_PRECISION_REGEX = """.*\.(?<precision>0+)""".toRegex()
  }
}
