package io.tolgee.formats

interface FromIcuParamConvertor {
  fun convert(
    node: MessagePatternUtil.ArgNode,
    isInPlural: Boolean,
  ): String

  /**
   * This method is called on the text parts (not argument parts) of the message
   */
  fun convertText(string: String): String

  /**
   * How to # in ICU plural form
   */
  fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String? = null,
  ): String
}
