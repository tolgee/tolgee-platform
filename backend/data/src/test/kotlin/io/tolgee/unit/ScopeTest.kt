package io.tolgee.unit

import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class ScopeTest {
  @Test
  fun `GLOSSARY_EDIT expands to include GLOSSARY_VIEW`() {
    Scope
      .expand(Scope.GLOSSARY_EDIT)
      .toSet()
      .assert
      .containsExactlyInAnyOrder(Scope.GLOSSARY_EDIT, Scope.GLOSSARY_VIEW)
  }

  @Test
  fun `GLOSSARY_VIEW expands to only itself`() {
    Scope
      .expand(Scope.GLOSSARY_VIEW)
      .toSet()
      .assert
      .containsExactlyInAnyOrder(Scope.GLOSSARY_VIEW)
  }

  @Test
  fun `project ADMIN does not include the org-level glossary scopes`() {
    val admin = Scope.expand(Scope.ADMIN).toSet()
    admin.assert.doesNotContain(Scope.GLOSSARY_VIEW, Scope.GLOSSARY_EDIT)
  }

  @Test
  fun `glossary scopes do not expand into project scopes`() {
    val expanded = Scope.expand(Scope.GLOSSARY_EDIT).toSet()
    expanded.assert.doesNotContain(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW, Scope.ADMIN)
  }

  @Test
  fun `organizationLevelScopes contains exactly the glossary scopes`() {
    Scope.organizationLevelScopes.assert
      .containsExactlyInAnyOrder(Scope.GLOSSARY_VIEW, Scope.GLOSSARY_EDIT)
  }

  @Test
  fun `isOrganizationLevel classifies scopes correctly`() {
    Scope.isOrganizationLevel(Scope.GLOSSARY_EDIT).assert.isTrue()
    Scope.isOrganizationLevel(Scope.GLOSSARY_VIEW).assert.isTrue()
    Scope.isOrganizationLevel(Scope.TRANSLATIONS_VIEW).assert.isFalse()
    Scope.isOrganizationLevel(Scope.ADMIN).assert.isFalse()
  }

  @Test
  fun `glossary scopes are parseable from their string values`() {
    Scope.fromValue("glossary.view").assert.isEqualTo(Scope.GLOSSARY_VIEW)
    Scope.fromValue("glossary.edit").assert.isEqualTo(Scope.GLOSSARY_EDIT)
  }
}
