package io.tolgee.testing.annotations

import io.tolgee.model.enums.Scope
import org.junit.jupiter.api.Test

@Test
annotation class ProjectApiKeyAuthTestMethod(
  val apiKeyPresentType: ApiKeyPresentMode = ApiKeyPresentMode.HEADER,
  val scopes: Array<Scope> = [Scope.TRANSLATIONS_EDIT, Scope.KEYS_EDIT, Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW],
)
