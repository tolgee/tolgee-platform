package io.tolgee.component.translationsLimitProvider

import io.tolgee.model.Organization
import org.springframework.stereotype.Component

@Component
class BaseTranslationsLimitProvider : TranslationsLimitProvider {
  override fun getTranslationSlotsLimit(organization: Organization?): Long = -1

  override fun getTranslationLimit(organization: Organization?): Long = -1

  override fun getPlanTranslations(organization: Organization?): Long = -1

  override fun getPlanTranslationSlots(organization: Organization?): Long = -1
}
