package io.tolgee.component.translationsLimitProvider

import io.tolgee.model.Organization

interface TranslationsLimitProvider {
  fun getLimit(organization: Organization?): Long

  fun getPlanTranslations(organization: Organization?): Long
}
