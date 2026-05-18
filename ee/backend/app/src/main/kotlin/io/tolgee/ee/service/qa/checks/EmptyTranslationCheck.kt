package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class EmptyTranslationCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.EMPTY_TRANSLATION

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    if (params.text.isBlank()) {
      return emptyTranslationIssue
    }

    if (
      params.isPlural &&
      allVariantsBlank(params)
    ) {
      return emptyTranslationIssue
    }

    return emptyList()
  }

  private fun allVariantsBlank(params: QaCheckParams): Boolean {
    // No error when variants failed to parse — ICU syntax check should scream for that already.
    val variants = params.textVariants ?: return false
    return variants.values.all { it.isBlank() }
  }

  private val emptyTranslationIssue =
    listOf(
      QaCheckResult(
        type = QaCheckType.EMPTY_TRANSLATION,
        message = QaIssueMessage.QA_EMPTY_TRANSLATION,
      ),
    )
}
