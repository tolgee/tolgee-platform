package io.tolgee.formats

interface FromIcuPlaceholderConvertor {
  fun convert(node: MessagePatternUtil.ArgNode): String

  /**
   * This method is called on the text parts (not argument parts) of the message
   */
  fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String

  /**
   * How to # in ICU plural form
   */
  fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String? = null,
  ): String
}
