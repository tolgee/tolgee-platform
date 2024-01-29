package io.tolgee.formats.po.out.php

import com.ibm.icu.text.MessagePattern
import com.ibm.icu.text.MessagePatternUtil
import io.tolgee.formats.po.FromIcuParamConvertor

class PhpFromIcuParamConvertor : FromIcuParamConvertor {
  private var argIndex = -1
  private var wasNumberedArg = false

  override fun convert(node: MessagePatternUtil.ArgNode): String {
    argIndex++
    val argNum = node.name?.toIntOrNull()
    val argNumString = getArgNumString(argNum)
    val type = node.argType

    if (type == MessagePattern.ArgType.SIMPLE) {
      when (node.typeName) {
        "number" -> return convertNumber(node, argNum)
      }
    }

    return "%${argNumString}s"
  }

  private fun convertNumber(
    node: MessagePatternUtil.ArgNode,
    argNum: Int?,
  ): String {
    if (node.simpleStyle?.trim() == "scientific") {
      return "%${getArgNumString(argNum)}e"
    }
    val precision = getPrecision(node)
    if (precision == 6) {
      return "%${getArgNumString(argNum)}f"
    }
    if (precision != null) {
      return "%${getArgNumString(argNum)}.${precision}f"
    }

    return "%${getArgNumString(argNum)}d"
  }

  private fun getPrecision(node: MessagePatternUtil.ArgNode): Int? {
    val precisionMatch = ICU_PRECISION_REGEX.matchEntire(node.simpleStyle ?: "")
    precisionMatch ?: return null
    return precisionMatch.groups["precision"]?.value?.length
  }

  private fun getArgNumString(icuArgNum: Int?): String {
    if ((icuArgNum != argIndex || wasNumberedArg) && icuArgNum != null) {
      wasNumberedArg = true
      return "${icuArgNum + 1}$"
    }
    return ""
  }

  companion object {
    val ICU_PRECISION_REGEX = """.*\.(?<precision>0+)""".toRegex()
  }
}
