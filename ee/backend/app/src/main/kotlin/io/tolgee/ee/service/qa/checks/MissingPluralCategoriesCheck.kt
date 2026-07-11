package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.formats.getPluralFormsForLocale
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class MissingPluralCategoriesCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.MISSING_PLURAL_CATEGORIES

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    if (!params.isPlural) {
      return emptyList()
    }

    val requiredForms = getPluralFormsForLocale(params.languageTag)
    return requiredForms.mapNotNull { form ->
      val variantText = params.textVariants?.get(form)
      if (variantText.isNullOrBlank()) {
        QaCheckResult(
          type = QaCheckType.MISSING_PLURAL_CATEGORIES,
          message = QaIssueMessage.QA_MISSING_PLURAL_CATEGORY,
          params = mapOf("variant" to form),
          pluralVariant = form,
        )
      } else {
        null
      }
    }
  }
}
