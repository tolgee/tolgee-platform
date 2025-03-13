package io.tolgee.formats.paramConvertors.out

import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil
import io.tolgee.formats.escapePercentSign

class IcuToApplePlaceholderConvertor(
  private val preserveFormatSpecifiers: Boolean = false
) : FromIcuPlaceholderConvertor {
  private val baseToCLikePlaceholderConvertor =
    BaseToCLikePlaceholderConvertor(
      defaultSpecifier = "@",
      numberSpecifier = "lld",
      preserveFormatSpecifiers = preserveFormatSpecifiers
    )

  override fun convert(node: MessagePatternUtil.ArgNode): String {
    return baseToCLikePlaceholderConvertor.convert(node)
  }

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String {
    return escapePercentSign(node.getText(keepEscaping), preserveFormatSpecifiers = preserveFormatSpecifiers)
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String {
    return "%lld"
  }
}
