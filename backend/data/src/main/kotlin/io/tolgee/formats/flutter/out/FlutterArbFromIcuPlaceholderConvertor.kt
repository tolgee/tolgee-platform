package io.tolgee.formats.flutter.out

import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.MessagePatternUtil

class FlutterArbFromIcuPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String = "{${node.name}}"

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String = node.getText(keepEscaping)

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String = "{$argName}"
}
