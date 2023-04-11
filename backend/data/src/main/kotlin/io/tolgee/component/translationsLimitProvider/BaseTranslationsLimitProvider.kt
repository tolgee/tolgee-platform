package io.tolgee.component.translationsLimitProvider

import io.tolgee.model.Organization
import org.springframework.stereotype.Component

@Component
class BaseTranslationsLimitProvider : TranslationsLimitProvider {
  override fun getLimit(organization: Organization?): Long = -1
  override fun getPlanTranslations(organization: Organization?): Long = -1
}
