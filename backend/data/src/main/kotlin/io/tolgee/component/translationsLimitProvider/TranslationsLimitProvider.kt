package io.tolgee.component.translationsLimitProvider

import io.tolgee.model.Organization

interface TranslationsLimitProvider {
  /**
   * Returns number of translation slots limit for organization
   * (This is for plans where useSlots = true)
   */
  fun getTranslationSlotsLimit(organization: Organization?): Long

  /**
   * Returns number of translations
   * (This is for  plan types)
   */
  fun getTranslationLimit(organization: Organization?): Long

  fun getPlanTranslations(organization: Organization?): Long

  fun getPlanTranslationSlots(organization: Organization?): Long
}
