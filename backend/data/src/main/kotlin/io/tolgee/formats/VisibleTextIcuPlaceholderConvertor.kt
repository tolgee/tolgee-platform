package io.tolgee.formats

/**
 * A [FromIcuPlaceholderConvertor] that strips all non-visible content
 * (ICU variables, plural `#`, HTML-like tags) so that only user-visible
 * text remains. Used for character-limit validation.
 */
class VisibleTextIcuPlaceholderConvertor : FromIcuPlaceholderConvertor {
  override fun convert(node: MessagePatternUtil.ArgNode): String = ""

  override fun convertText(
    node: MessagePatternUtil.TextNode,
    keepEscaping: Boolean,
  ): String {
    return node.getText(keepEscaping).replace(HTML_TAG_REGEX, "")
  }

  override fun convertReplaceNumber(
    node: MessagePatternUtil.MessageContentsNode,
    argName: String?,
  ): String = ""

  companion object {
    private val HTML_TAG_REGEX = Regex("<[^>]+>")
  }
}
