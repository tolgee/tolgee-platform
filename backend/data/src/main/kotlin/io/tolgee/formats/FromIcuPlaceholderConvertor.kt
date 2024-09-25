package io.tolgee.formats

interface FromIcuPlaceholderConvertor {
  fun convert(
    node: MessagePatternUtil.ArgNode,
    customValues: Map<String, Any?>?,
  ): String {
    return convert(node)
  }

  fun convert(node: MessagePatternUtil.ArgNode): String

  /**
   * This method is called on the text parts (not argument parts) of the message
   */
  fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
    customValues: Map<String, Any?>?,
  ): String {
    return convertText(node, keepEscaping)
  }

  fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String

  /**
   * How to # in ICU plural form
   */
  fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    customValues: Map<String, Any?>?,
    argName: String? = null,
  ): String {
    return convertReplaceNumber(node, argName)
  }

  fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String? = null,
  ): String
}
