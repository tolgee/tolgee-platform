package io.tolgee.annotations

import io.tolgee.model.enums.ApiScope
import org.testng.annotations.Test

@Test
annotation class ProjectApiKeyAuthTestMethod(
  val scopes: Array<ApiScope> = [ApiScope.TRANSLATIONS_EDIT, ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW]
)
