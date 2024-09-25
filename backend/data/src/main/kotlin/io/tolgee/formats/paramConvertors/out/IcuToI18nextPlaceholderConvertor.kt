package io.tolgee.formats.paramConvertors.out

import com.ibm.icu.text.MessagePattern
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil
import io.tolgee.formats.i18next.I18NEXT_UNESCAPED_FLAG_CUSTOM_KEY

class IcuToI18nextPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(
    node: MessagePatternUtil.ArgNode,
    customValues: Map<String, Any?>?,
  ): String {
    val type = node.argType

    if (type == MessagePattern.ArgType.SIMPLE) {
      when (node.typeName) {
        "number" -> return convertNumber(node)
      }
    }

    if (customValues.hasUnescapedFlag(node.name)) {
      return "{{- ${node.name}]}"
    }

    return "{{${node.name}}}"
  }

  override fun convert(node: MessagePatternUtil.ArgNode): String {
    return convert(node, null)
  }

  private fun Map<String, Any?>?.hasUnescapedFlag(name: String?): Boolean {
    if (this == null || name == null) {
      return false
    }
    return this[I18NEXT_UNESCAPED_FLAG_CUSTOM_KEY]
      ?.let { it as? List<*> }
      ?.mapNotNull { it as? String }
      ?.contains(name)
      ?: false
  }

  override fun convertText(string: String): String {
    // We should escape {{ and $t, but there doesn't seem to be a documented
    // way how to escape either {{ or $t in i18next
    return string
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
