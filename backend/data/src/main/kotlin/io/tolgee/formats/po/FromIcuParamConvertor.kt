package io.tolgee.formats.po

import com.ibm.icu.text.MessagePatternUtil.ArgNode
import com.ibm.icu.text.MessagePatternUtil.MessageContentsNode

interface FromIcuParamConvertor {
  fun convert(
    node: ArgNode,
    isInPlural: Boolean,
  ): String

  /**
   * How to # in ICU plural form
   */
  fun convertReplaceNumber(
    node: MessageContentsNode,
    argName: String? = null,
  ): String
}
