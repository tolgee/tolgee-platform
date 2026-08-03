package io.tolgee.unit

import io.tolgee.dtos.sso.SsoTenantConfig
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class SsoTenantConfigTest {
  @Test
  fun `detects google by authorization uri`() {
    config(authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth").isGoogle.assert.isTrue()
  }

  @Test
  fun `detects google by token uri`() {
    config(tokenUri = "https://oauth2.googleapis.com/token").isGoogle.assert.isTrue()
  }

  @Test
  fun `does not detect generic provider as google`() {
    config().isGoogle.assert.isFalse()
  }

  @Test
  fun `does not detect google host in path as google`() {
    config(authorizationUri = "https://evil.com/accounts.google.com").isGoogle.assert.isFalse()
  }

  @Test
  fun `does not detect google-like suffix without dot as google`() {
    config(authorizationUri = "https://notgoogleapis.com/auth").isGoogle.assert.isFalse()
  }

  @Test
  fun `handles unparseable uri`() {
    config(authorizationUri = "not a uri").isGoogle.assert.isFalse()
  }

  private fun config(
    authorizationUri: String = "https://idp.example.com/auth",
    tokenUri: String = "https://idp.example.com/token",
  ) = SsoTenantConfig(
    clientId = "clientId",
    clientSecret = "clientSecret",
    authorizationUri = authorizationUri,
    domain = "example.com",
    tokenUri = tokenUri,
    force = false,
    global = false,
  )
}
