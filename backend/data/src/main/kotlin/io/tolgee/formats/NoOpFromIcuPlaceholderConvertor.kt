package io.tolgee.formats

class NoOpFromIcuPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String {
    return node.patternString
  }

  override fun convertText(string: String): String {
    return string
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String = node.patternString
}
