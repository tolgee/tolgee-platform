package io.tolgee.formats.paramConvertors.out

import com.ibm.icu.text.MessagePattern
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil

class IcuToRubyPlaceholderConvertor : FromIcuPlaceholderConvertor {
  private var argIndex = -1
  private var wasNumberedArg = false

  override fun convert(
    node: MessagePatternUtil.ArgNode,
    isInPlural: Boolean,
  ): String {
    argIndex++
    val argNameString = getArgNameString(node)
    val type = node.argType

    if (type == MessagePattern.ArgType.NONE && node.argNumOrNull == null) {
      return "%$argNameString"
    }

    if (type == MessagePattern.ArgType.SIMPLE) {
      when (node.typeName) {
        "number" -> return convertNumber(node)
      }
    }

    return "%${argNameString}s"
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String {
    return "%($argName)d"
  }

  private fun convertNumber(node: MessagePatternUtil.ArgNode): String {
    if (node.simpleStyle.trim() == "scientific") {
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
    val argType = node.argType ?: MessagePattern.ArgType.NONE
    val argNum = node.argNumOrNull?.toInt()
    val argName = node.name
    return when {
      argType != MessagePattern.ArgType.NONE -> {
        when {
          argNum != null -> getArgNumString(argNum)
          argName != null -> "<$argName>"
          else -> ""
        }
      }

      else ->
        when {
          argNum != null -> getArgNumString(argNum)
          argName != null -> "{$argName}"
          else -> ""
        }
    }
  }

  private fun getArgNumString(icuArgNum: Int?): String {
    if ((icuArgNum != argIndex || wasNumberedArg) && icuArgNum != null) {
      wasNumberedArg = true
      return "${icuArgNum + 1}$"
    }
    return ""
  }

  private val MessagePatternUtil.ArgNode.argNumOrNull get() = this.name?.toLongOrNull()

  companion object {
    val ICU_PRECISION_REGEX = """.*\.(?<precision>0+)""".toRegex()
  }
}
