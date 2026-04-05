package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.formats.getPluralFormsForLocale
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class EmptyTranslationCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.EMPTY_TRANSLATION

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    if (params.text.isBlank()) {
      return listOf(
        QaCheckResult(
          type = QaCheckType.EMPTY_TRANSLATION,
          message = QaIssueMessage.QA_EMPTY_TRANSLATION,
        ),
      )
    }

    if (params.isPlural && params.textVariants != null) {
      return checkPluralVariants(params)
    }

    return emptyList()
  }

  private fun checkPluralVariants(params: QaCheckParams): List<QaCheckResult> {
    val requiredForms = getPluralFormsForLocale(params.languageTag)

    val formsToCheck =
      if (params.activeVariant != null) {
        requiredForms.filter { it == params.activeVariant }
      } else {
        requiredForms
      }

    return formsToCheck.mapNotNull { form ->
      val variantText = params.textVariants?.get(form)
      if (variantText.isNullOrBlank()) {
        QaCheckResult(
          type = QaCheckType.EMPTY_TRANSLATION,
          message = QaIssueMessage.QA_EMPTY_PLURAL_VARIANT,
          params = mapOf("variant" to form),
          pluralVariant = form,
        )
      } else {
        null
      }
    }
  }
}
