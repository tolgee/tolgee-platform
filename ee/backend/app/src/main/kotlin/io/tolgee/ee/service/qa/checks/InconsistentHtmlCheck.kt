package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class InconsistentHtmlCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.INCONSISTENT_HTML

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
    if (base.isBlank() || text.isBlank()) return emptyList()

    val baseTags = HtmlTagParser.findTags(base)
    val textTags = HtmlTagParser.findTags(text)

    if (baseTags.isEmpty() && textTags.isEmpty()) return emptyList()

    val results = mutableListOf<QaCheckResult>()

    // Find extra tags: in translation but not matched by base
    val remainingBase = baseTags.map { comparisonKey(it) }.toMutableList()
    for (tag in textTags) {
      val key = comparisonKey(tag)
      if (!remainingBase.remove(key)) {
        results.add(
          QaCheckResult(
            type = QaCheckType.INCONSISTENT_HTML,
            message = QaIssueMessage.QA_HTML_TAG_EXTRA,
            replacement = "",
            positionStart = tag.start,
            positionEnd = tag.end,
            params = mapOf("tag" to tag.raw),
          ),
        )
      }
    }

    // Find missing tags: in base but not matched by translation
    val remainingText = textTags.map { comparisonKey(it) }.toMutableList()
    for (tag in baseTags) {
      val key = comparisonKey(tag)
      if (!remainingText.remove(key)) {
        results.add(
          QaCheckResult(
            type = QaCheckType.INCONSISTENT_HTML,
            message = QaIssueMessage.QA_HTML_TAG_MISSING,
            replacement = null,
            positionStart = 0,
            positionEnd = 0,
            params = mapOf("tag" to tag.raw),
          ),
        )
      }
    }

    return results
  }

  companion object {
    private fun comparisonKey(tag: HtmlTag): String = "${tag.kind}:${tag.name}"
  }
}
