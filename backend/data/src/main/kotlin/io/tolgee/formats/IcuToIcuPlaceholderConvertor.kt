package io.tolgee.formats

class IcuToIcuPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String = node.patternString

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String = node.patternString

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String = node.patternString
}
