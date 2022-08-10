package io.tolgee.component.translationsLimitProvider

import io.tolgee.model.Organization
import org.springframework.stereotype.Component

@Component
class BaseTranslationsLimitProvider : TranslationsLimitProvider {
  override fun get(organization: Organization?): Long = -1
}
