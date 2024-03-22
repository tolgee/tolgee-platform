package io.tolgee.formats.flutter.out

import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil

class FlutterArbFromIcuPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String {
    return "{${node.name}}"
  }

  override fun convertText(string: String): String {
    return string
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String = "{$argName}"
}
