package io.tolgee.formats

interface FromIcuParamConvertor {
  fun convert(
    node: MessagePatternUtil.ArgNode,
    isInPlural: Boolean,
  ): String

  /**
   * How to # in ICU plural form
   */
  fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String? = null,
  ): String
}
