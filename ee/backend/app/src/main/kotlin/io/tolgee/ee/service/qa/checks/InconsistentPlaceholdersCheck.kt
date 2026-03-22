package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.formats.MessagePatternUtil
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class InconsistentPlaceholdersCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.INCONSISTENT_PLACEHOLDERS

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText ->
      checkVariant(text, baseText)
    }
  }

  private fun checkVariant(
    text: String,
    baseText: String?,
  ): List<QaCheckResult> {
    val base = baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    if (text.isBlank()) return emptyList()

    val baseArgs = extractArgs(base) ?: return emptyList()
    val textArgs = extractArgs(text) ?: return emptyList()

    val baseNames = baseArgs.map { it.name }.toSet()
    val textNames = textArgs.map { it.name }.toSet()

    val results = mutableListOf<QaCheckResult>()

    val missingNames = baseNames - textNames
    for (name in missingNames) {
      results.add(
        QaCheckResult(
          type = QaCheckType.INCONSISTENT_PLACEHOLDERS,
          message = QaIssueMessage.QA_PLACEHOLDERS_MISSING,
          replacement = null,
          positionStart = null,
          positionEnd = null,
          params = mapOf("placeholder" to name),
        ),
      )
    }

    val extraNames = textNames - baseNames
    for (arg in textArgs.filter { it.name in extraNames }) {
      val hasPosition = arg.positionStart != null && arg.positionEnd != null
      results.add(
        QaCheckResult(
          type = QaCheckType.INCONSISTENT_PLACEHOLDERS,
          message = QaIssueMessage.QA_PLACEHOLDERS_EXTRA,
          replacement = if (hasPosition) "" else null,
          positionStart = arg.positionStart,
          positionEnd = arg.positionEnd,
          params = mapOf("placeholder" to arg.name),
        ),
      )
    }

    return results
  }

  data class ArgInfo(
    val name: String,
    val positionStart: Int? = null,
    val positionEnd: Int? = null,
  )

  companion object {
    fun extractArgs(text: String): List<ArgInfo>? {
      return try {
        val node = MessagePatternUtil.buildMessageNode(text)
        val args = mutableListOf<ArgInfo>()
        collectArgsWithPositions(node, 0, args)
        args
      } catch (e: Exception) {
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
  }
}
