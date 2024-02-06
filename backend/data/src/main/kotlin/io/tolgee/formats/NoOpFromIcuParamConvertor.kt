package io.tolgee.formats

import com.ibm.icu.text.MessagePatternUtil
import io.tolgee.formats.po.FromIcuParamConvertor

class NoOpFromIcuParamConvertor : FromIcuParamConvertor {
  override fun convert(
    node: MessagePatternUtil.ArgNode,
    isInPlural: Boolean,
  ): String {
    return node.toString()
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String {
    return "#"
  }
}
