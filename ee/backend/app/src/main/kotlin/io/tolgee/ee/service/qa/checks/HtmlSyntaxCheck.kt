package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class HtmlSyntaxCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.HTML_SYNTAX

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, _ ->
      checkVariant(text)
    }
  }

  private fun checkVariant(text: String): List<QaCheckResult> {
    if (text.isBlank()) return emptyList()

    val tags = HtmlTagParser.findTags(text)
    if (tags.isEmpty()) return emptyList()

    val results = mutableListOf<QaCheckResult>()
    // Map of tag name → stack of open tags (same approach as frontend getPlaceholders)
    val openTags = mutableMapOf<String, MutableList<HtmlTag>>()

    for (tag in tags) {
      when (tag.kind) {
        HtmlTagKind.SELF_CLOSING -> { /* always valid */ }

        HtmlTagKind.OPEN -> {
          if (tag.name in VOID_ELEMENTS) {
            // Void elements like <br> don't need closing tags
          } else {
            openTags.getOrPut(tag.name) { mutableListOf() }.add(tag)
          }
        }

        HtmlTagKind.CLOSE -> {
          val stack = openTags[tag.name]
          if (stack != null && stack.isNotEmpty()) {
            stack.removeAt(stack.lastIndex)
            if (stack.isEmpty()) {
              openTags.remove(tag.name)
            }
          } else {
            // Closing tag without matching opener
            results.add(
              QaCheckResult(
                type = QaCheckType.HTML_SYNTAX,
                message = QaIssueMessage.QA_HTML_UNOPENED_TAG,
                replacement = "",
                positionStart = tag.start,
                positionEnd = tag.end,
                params = mapOf("tag" to tag.raw),
              ),
            )
          }
        }
      }
    }

    // Remaining open tags are unclosed
    for ((_, stack) in openTags) {
      for (openTag in stack) {
        results.add(
          QaCheckResult(
            type = QaCheckType.HTML_SYNTAX,
            message = QaIssueMessage.QA_HTML_UNCLOSED_TAG,
            replacement = null,
            positionStart = openTag.start,
            positionEnd = openTag.end,
            params = mapOf("tag" to openTag.raw),
          ),
        )
      }
    }

    return results
  }

  companion object {
    val VOID_ELEMENTS =
      setOf(
        "area",
        "base",
        "br",
        "col",
        "embed",
        "hr",
        "img",
        "input",
        "link",
        "meta",
        "param",
        "source",
        "track",
        "wbr",
      )
  }
}
