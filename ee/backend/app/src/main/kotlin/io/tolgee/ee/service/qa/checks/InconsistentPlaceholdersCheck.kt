package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
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
}
