package io.tolgee.model.oauth2

import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class OAuth2GrantTest {
  @Test
  fun `an unbound grant reaches no project, rather than every project`() {
    OAuth2Grant().boundProjectIds().assert.isEmpty()
  }

  @Test
  fun `an empty selection reaches no project`() {
    val grant = OAuth2Grant().apply { projectSelection = "" }

    grant.boundProjectIds().assert.isEmpty()
  }

  @Test
  fun `binding an empty collection is a deny, not an all-projects grant`() {
    val grant = OAuth2Grant().apply { bindProjects(emptyList()) }

    grant.boundProjectIds().assert.isEmpty()
  }

  @Test
  fun `only the sentinel means every project`() {
    val grant = OAuth2Grant().apply { bindProjects(null) }

    grant.projectSelection.assert.isEqualTo(OAuth2Constants.ALL_PROJECTS)
    grant.boundProjectIds().assert.isNull()
  }

  @Test
  fun `a stored scope name that no longer resolves is dropped, narrowing the grant`() {
    val grant =
      OAuth2Grant().apply {
        issuedTokenScopes = "${Scope.TRANSLATIONS_VIEW.name} TRANSLATIONS_RETIRED_SCOPE"
      }

    grant.issuedTokenScopeSet().assert.containsExactly(Scope.TRANSLATIONS_VIEW)
  }
}
