package io.tolgee.security.oauth2.cimd

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class CimdClientPolicyTest {
  @Test
  fun `a well-formed https URL is a candidate`() {
    policy().isCandidate("https://app.example.com/.well-known/client").assert.isTrue()
  }

  @Test
  fun `an http URL is not a candidate in production, but is when SSRF protection is disabled for dev`() {
    // Mirrors the fetcher, which rejects http unless the dev flag is set; otherwise isStillAuthorized would claim a
    // client find() could never resolve.
    policy().isCandidate("http://app.example.com/client").assert.isFalse()
    policy(disableSsrf = true).isCandidate("http://app.example.com/client").assert.isTrue()
  }

  @Test
  fun `a non-URL id is not a candidate`() {
    val policy = policy()

    policy.isCandidate("tolgee-cli").assert.isFalse()
    policy.isCandidate("tolgee-browser-extension").assert.isFalse()
  }

  @Test
  fun `a scheme prefix without a host is not a candidate`() {
    val policy = policy()

    // The old startsWith check let these through; a real parse rejects them before a doomed fetch.
    policy.isCandidate("https://").assert.isFalse()
    policy.isCandidate("https:///path").assert.isFalse()
    policy.isCandidate("not a url").assert.isFalse()
  }

  @Test
  fun `a non-http scheme is not a candidate`() {
    val policy = policy()

    policy.isCandidate("ftp://app.example.com/client").assert.isFalse()
    policy.isCandidate("file:///etc/passwd").assert.isFalse()
  }

  @Test
  fun `an empty allowed-hosts list permits any host`() {
    val policy = policy(allowedHosts = listOf())

    policy.isCandidate("https://anything.example/client").assert.isTrue()
  }

  @Test
  fun `a non-empty allowed-hosts list permits only the listed hosts`() {
    val policy = policy(allowedHosts = listOf("app.example.com"))

    policy.isCandidate("https://app.example.com/client").assert.isTrue()
    policy.isCandidate("https://evil.example/client").assert.isFalse()
  }

  private fun policy(
    allowedHosts: List<String> = listOf(),
    disableSsrf: Boolean = false,
  ) = CimdClientPolicy(
    OAuth2ServerProperties().apply { cimdAllowedHosts = allowedHosts },
    InternalProperties().apply { disableUrlSsrfProtection = disableSsrf },
  )
}
