package io.tolgee.formats

class NoOpFromIcuPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String = node.patternString

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String = node.getText(keepEscaping)

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String = node.patternString
}
