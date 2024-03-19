package io.tolgee.formats.flutter.out

import io.tolgee.formats.FromIcuParamConvertor
import io.tolgee.formats.MessagePatternUtil

class FlutterArbFromIcuParamConvertor : FromIcuParamConvertor {
  override fun convert(
    node: MessagePatternUtil.ArgNode,
    isInPlural: Boolean,
  ): String {
    return "{${node.name}}"
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String = "{$argName}"
}
