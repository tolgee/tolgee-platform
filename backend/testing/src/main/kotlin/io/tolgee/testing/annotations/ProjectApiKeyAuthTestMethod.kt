package io.tolgee.testing.annotations

import io.tolgee.model.enums.ApiScope

annotation class ProjectApiKeyAuthTestMethod(
  val scopes: Array<ApiScope> = [ApiScope.TRANSLATIONS_EDIT, ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW]
)
