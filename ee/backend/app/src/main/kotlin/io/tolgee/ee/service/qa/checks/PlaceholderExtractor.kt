package io.tolgee.ee.service.qa.checks

import io.tolgee.formats.MessagePatternUtil

data class ArgInfo(
  val name: String,
  val positionStart: Int? = null,
  val positionEnd: Int? = null,
  val sourceText: String? = null,
)

/**
 * Virtual placeholder name used for the ICU plural `#` token (REPLACE_NUMBER).
 * Safe as a name because real ICU argument names are restricted to `[\p{L}\p{N}_]+`
 * — so a user-defined argument named `#` is impossible.
 */
const val REPLACE_NUMBER_PLACEHOLDER_NAME = "#"

/**
 * Synthetic plural wrapper that makes the ICU parser treat the variant body as it
 * would inside a real plural — unquoted `#` chars become REPLACE_NUMBER tokens.
 */
private const val PLURAL_WRAPPER_PREFIX = "{n,plural,other{"
private const val PLURAL_WRAPPER_SUFFIX = "}}"

fun extractArgs(
  text: String,
  isPluralVariant: Boolean = false,
): List<ArgInfo>? {
  return try {
    val rootMessage = buildMessageNode(text, isPluralVariant) ?: return emptyList()
    val args = mutableListOf<ArgInfo>()
    collectArgsWithPositions(rootMessage, 0, args)
    args
  } catch (_: Exception) {
    // Broad catch is intentional — parsing failures are surfaced by IcuSyntaxCheck,
    // so this extractor can silently return null for malformed text.
    null
  }
}

private fun buildMessageNode(
  text: String,
  isPluralVariant: Boolean,
): MessagePatternUtil.MessageNode? {
  if (!isPluralVariant) {
    return MessagePatternUtil.buildMessageNode(text)
  }

  // Wrap as plural, then unwrap the original message node - to correctly parse plural variants
  val wrappedNode = MessagePatternUtil.buildMessageNode(PLURAL_WRAPPER_PREFIX + text + PLURAL_WRAPPER_SUFFIX)
  val pluralArg = wrappedNode.contents.firstOrNull() as? MessagePatternUtil.ArgNode ?: return null
  return pluralArg.complexStyle
    ?.variants
    ?.firstOrNull()
    ?.message
}

/**
 * Collects arg names with positions by accumulating patternString lengths starting
 * from [offset]. Top-level args (and top-level REPLACE_NUMBER `#` tokens) get accurate
 * positions. Args nested inside complex styles (select/plural/choice) are collected
 * with null positions via [collectNamesOnly] since QaPluralCheckHelper handles
 * per-variant splitting for plural messages.
 */
private fun collectArgsWithPositions(
  node: MessagePatternUtil.MessageNode,
  offset: Int,
  args: MutableList<ArgInfo>,
) {
  var pos = offset
  for (content in node.contents) {
    val len = content.patternString.length
    when {
      content is MessagePatternUtil.ArgNode -> {
        content.name?.let { name ->
          args.add(ArgInfo(name, pos, pos + len, content.patternString))
        }
        content.complexStyle?.variants?.forEach { variant ->
          variant.message?.let { collectNamesOnly(it, args) }
        }
      }

      content.type == MessagePatternUtil.MessageContentsNode.Type.REPLACE_NUMBER -> {
        args.add(ArgInfo(REPLACE_NUMBER_PLACEHOLDER_NAME, pos, pos + len, content.patternString))
      }
    }
    pos += len
  }
}

/**
 * Recursively collects arg names from nested messages without position tracking.
 */
private fun collectNamesOnly(
  node: MessagePatternUtil.MessageNode,
  args: MutableList<ArgInfo>,
) {
  for (content in node.contents) {
    when {
      content is MessagePatternUtil.ArgNode -> {
        content.name?.let { name ->
          args.add(ArgInfo(name, sourceText = content.patternString))
        }
        content.complexStyle?.variants?.forEach { variant ->
          variant.message?.let { collectNamesOnly(it, args) }
        }
      }

      content.type == MessagePatternUtil.MessageContentsNode.Type.REPLACE_NUMBER -> {
        args.add(ArgInfo(REPLACE_NUMBER_PLACEHOLDER_NAME, sourceText = content.patternString))
      }
    }
  }
}
