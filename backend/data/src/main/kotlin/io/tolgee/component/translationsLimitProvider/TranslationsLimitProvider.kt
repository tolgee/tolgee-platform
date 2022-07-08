package io.tolgee.component.translationsLimitProvider

import io.tolgee.model.Organization

interface TranslationsLimitProvider {
  fun get(organization: Organization?): Long
}
