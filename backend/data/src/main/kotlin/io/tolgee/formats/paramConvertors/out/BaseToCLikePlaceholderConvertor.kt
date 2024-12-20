package io.tolgee.formats.paramConvertors.out

import com.ibm.icu.text.MessagePattern
import io.tolgee.formats.MessagePatternUtil
import io.tolgee.formats.escapePercentSign

class BaseToCLikePlaceholderConvertor(
  private val defaultSpecifier: String = "s",
  private val numberSpecifier: String = "d",
  numberAllArgs: Boolean = false,
  private val argNameStringProvider: (BaseToCLikePlaceholderConvertor.(MessagePatternUtil.ArgNode) -> String)? = null,
) {
  private var argIndex = -1
  private var wasNumberedArg = numberAllArgs

  fun convert(node: MessagePatternUtil.ArgNode): String {
    argIndex++
    val argNameString = getArgNameString(node)
    val type = node.argType

    if (isSimpleNamedArgument(type, node) && argNameString.isNotEmpty()) {
      return "%$argNameString"
    }

    if (type == MessagePattern.ArgType.SIMPLE) {
      when (node.typeName) {
        "number" -> return convertNumber(node)
      }
    }

    return "%${argNameString}$defaultSpecifier"
  }

  private fun isSimpleNamedArgument(
    type: MessagePattern.ArgType?,
    node: MessagePatternUtil.ArgNode,
  ) = type == MessagePattern.ArgType.NONE && node.argNumOrNull == null

  fun convertText(string: String): String {
    return escapePercentSign(string)
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

    return "%${getArgNameString(node)}$numberSpecifier"
  }

  private fun getPrecision(node: MessagePatternUtil.ArgNode): Int? {
    val precisionMatch = ICU_PRECISION_REGEX.matchEntire(node.simpleStyle ?: "")
    precisionMatch ?: return null
    return precisionMatch.groups["precision"]?.value?.length
  }

  private fun getArgNameString(node: MessagePatternUtil.ArgNode): String {
    return argNameStringProvider?.invoke(this, node) ?: getArgNumString(node.argNumOrNull?.toInt())
  }

  fun getArgNumString(icuArgNum: Int?): String {
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
