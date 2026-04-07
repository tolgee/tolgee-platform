package io.tolgee.ee.service.qa.checks

import io.tolgee.formats.MessagePatternUtil

data class ArgInfo(
  val name: String,
  val positionStart: Int? = null,
  val positionEnd: Int? = null,
)

fun extractArgs(text: String): List<ArgInfo>? {
  return try {
    val node = MessagePatternUtil.buildMessageNode(text)
    val args = mutableListOf<ArgInfo>()
    collectArgsWithPositions(node, 0, args)
    args
  } catch (e: Exception) {
    // Broad catch is intentional — parsing failures are surfaced by IcuSyntaxCheck,
    // so this extractor can silently return null for malformed text.
    null
  }
}

/**
 * Collects arg names with positions by accumulating patternString lengths.
 * Top-level args get accurate positions. Args nested inside complex styles
 * (select/plural/choice) are collected with position null,null (name only) since
 * QaPluralCheckHelper handles per-variant splitting for plural messages.
 */
private fun collectArgsWithPositions(
  node: MessagePatternUtil.MessageNode,
  offset: Int,
  args: MutableList<ArgInfo>,
) {
  var pos = offset
  for (content in node.contents) {
    val len = content.patternString.length
    if (content is MessagePatternUtil.ArgNode) {
      content.name?.let { name ->
        args.add(ArgInfo(name, pos, pos + len))
      }
      content.complexStyle?.variants?.forEach { variant ->
        variant.message?.let { collectNamesOnly(it, args) }
      }
    }
    pos += len
  }
}

/** Recursively collects arg names from nested messages without position tracking. */
private fun collectNamesOnly(
  node: MessagePatternUtil.MessageNode,
  args: MutableList<ArgInfo>,
) {
  for (content in node.contents) {
    if (content is MessagePatternUtil.ArgNode) {
      content.name?.let { name ->
        args.add(ArgInfo(name))
      }
      content.complexStyle?.variants?.forEach { variant ->
        variant.message?.let { collectNamesOnly(it, args) }
      }
    }
  }
}
