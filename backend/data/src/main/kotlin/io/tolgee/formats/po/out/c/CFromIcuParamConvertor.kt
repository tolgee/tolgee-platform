package io.tolgee.formats.po.out.c

import com.ibm.icu.text.MessagePattern
import com.ibm.icu.text.MessagePatternUtil
import io.tolgee.formats.po.FromIcuParamConvertor

class CFromIcuParamConvertor : FromIcuParamConvertor {
  private var argIndex = -1

  override fun convert(node: MessagePatternUtil.ArgNode): String {
    argIndex++
    val argNum = node.name?.toIntOrNull()
    val type = node.argType

    if (type == MessagePattern.ArgType.SIMPLE) {
      when (node.typeName) {
        "number" -> return convertNumber(node)
      }
    }

    return "%s"
  }

  private fun convertNumber(node: MessagePatternUtil.ArgNode): String {
    if (node.simpleStyle?.trim() == "scientific") {
      return "%e"
    }
    val precision = getPrecision(node)
    if (precision == 6) {
      return "%f"
    }
    if (precision != null) {
      return "%.${precision}f"
    }

    return "%d"
  }

  private fun getPrecision(node: MessagePatternUtil.ArgNode): Int? {
    val precisionMatch = ICU_PRECISION_REGEX.matchEntire(node.simpleStyle ?: "")
    precisionMatch ?: return null
    return precisionMatch.groups["precision"]?.value?.length
  }

  companion object {
    val ICU_PRECISION_REGEX = """.*\.(?<precision>0+)""".toRegex()
  }
}
