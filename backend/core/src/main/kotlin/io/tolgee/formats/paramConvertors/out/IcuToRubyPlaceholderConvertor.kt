package io.tolgee.formats.paramConvertors.out

import com.ibm.icu.text.MessagePattern
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil

class IcuToRubyPlaceholderConvertor : FromIcuPlaceholderConvertor {
  private val baseToCLikePlaceholderConvertor =
    BaseToCLikePlaceholderConvertor(
      defaultSpecifier = "s",
    ) {
      getArgNameString(it)
    }

  override fun convert(node: MessagePatternUtil.ArgNode): String {
    return baseToCLikePlaceholderConvertor.convert(node)
  }

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String {
    return baseToCLikePlaceholderConvertor.convertText(node.getText(keepEscaping))
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String {
    val argNum = argName?.toIntOrNull()
    return when {
      argNum == 0 -> "%d"
      argNum != null && argNum > 0 -> "%$argNum\$d"
      else ->
        when (argName) {
          is String -> "%{$argName}"
          else -> "%d"
        }
    }
  }

  private fun getArgNameString(node: MessagePatternUtil.ArgNode): String {
    val argType = node.argType ?: MessagePattern.ArgType.NONE
    val argNum = node.argNumOrNull?.toInt()
    val argName = node.name
    return when {
      argType != MessagePattern.ArgType.NONE -> {
        when {
          argNum != null -> baseToCLikePlaceholderConvertor.getArgNumString(argNum)
          argName != null -> "<$argName>"
          else -> ""
        }
      }

      else ->
        when {
          argNum != null -> baseToCLikePlaceholderConvertor.getArgNumString(argNum)
          argName != null -> "{$argName}"
          else -> ""
        }
    }
  }

  private val MessagePatternUtil.ArgNode.argNumOrNull get() = this.name?.toLongOrNull()
}
