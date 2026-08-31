package io.tolgee.security.oauth2

import io.tolgee.api.v2.controllers.oauth2.OAuth2FlowController
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * The consent API is guarded by what it does *not* declare: absence of [AllowApiAccess] is what keeps API credentials
 * out, and absence of the two bypasses is what keeps an unverified-email or forced-SSO user out. Nothing else in the
 * codebase would fail if one of them were added back, so it is asserted here directly.
 */
class OAuth2EndpointGuardsTest {
  @Test
  fun `no consent endpoint opts out of authentication policy`() {
    val optedOut =
      OAuth2FlowController::class.java.declaredMethods
        .filter { method ->
          FORBIDDEN_ANNOTATIONS.any { method.isAnnotationPresent(it) }
        }.map { it.name }

    optedOut.assert.isEmpty()
  }

  companion object {
    private val FORBIDDEN_ANNOTATIONS =
      listOf(
        AllowApiAccess::class.java,
        BypassEmailVerification::class.java,
        BypassForcedSsoAuthentication::class.java,
      )
  }
}
