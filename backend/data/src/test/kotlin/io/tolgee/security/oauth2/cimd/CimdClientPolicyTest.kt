package io.tolgee.security.oauth2.cimd

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CimdClientPolicyTest {
  @Test
  fun `a well-formed https URL is a candidate`() {
    policy().isCandidate("https://app.example.com/.well-known/client").assert.isTrue()
  }

  @Test
  fun `an http URL is not a candidate in production, but is when SSRF protection is disabled for dev`() {
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

  @Test
  fun `the allowed-hosts comparison is case-insensitive, as host names are`() {
    val policy = policy(allowedHosts = listOf("Claude.ai"))

    policy.isCandidate("https://claude.ai/client").assert.isTrue()
    policy.isCandidate("https://CLAUDE.AI/client").assert.isTrue()
  }

  @Test
  fun `a client_id served from this instance's own origin is not a candidate`() {
    val policy = policy()

    policy.isCandidate("https://tolgee.example.com/client").assert.isFalse()
    policy.isCandidate("https://TOLGEE.EXAMPLE.COM:443/anything").assert.isFalse()
    policy.isCandidate("https://other.example.com/client").assert.isTrue()
  }

  @Test
  fun `a client_id carrying a fragment is not a candidate`() {
    policy().isCandidate("https://publisher.example/c.json#x").assert.isFalse()
  }

  @Test
  fun `a client_id carrying a query string is not a candidate`() {
    policy().isCandidate("https://publisher.example/c.json?n=1").assert.isFalse()
    policy().isCandidate("https://publisher.example/c.json").assert.isTrue()
  }

  @Test
  fun `a client_id whose scheme is not spelled in lower case is not a candidate`() {
    listOf("HTTPS://publisher.example/c", "Https://publisher.example/c", "hTTps://publisher.example/c").forEach {
      policy().isCandidate(it).assert.isFalse()
    }
  }

  @Test
  fun `every client_id the gate accepts is one the SQL prefix selects`() {
    val candidates =
      listOf(
        "https://publisher.example/client",
        "HTTPS://publisher.example/client",
        "Https://publisher.example/client",
        "http://127.0.0.1:8080/client",
        "HTTP://127.0.0.1:8080/client",
        "hxxps://publisher.example/client",
        "//publisher.example/client",
        "publisher.example/client",
      )

    candidates.filter { CimdUrls.isAcceptableClientId(it, allowHttp = true) }.forEach {
      it.startsWith("http").assert.isTrue()
    }
  }

  @Test
  fun `neither of this instance's own origins may present a document`() {
    val policy = policy()

    policy.isCandidate("https://tolgee.example.com/client").assert.isFalse()
    policy.isCandidate("https://app.tolgee.example.com/client").assert.isFalse()
    policy.isCandidate("https://APP.TOLGEE.EXAMPLE.COM:443/client").assert.isFalse()
    policy.isCandidate("https://other.example.com/client").assert.isTrue()
  }

  @Test
  fun `nothing is a candidate once CIMD is turned off`() {
    val policy = policy(cimdEnabled = false)

    policy.isCandidate("https://claude.ai/client").assert.isFalse()
    policy.isCandidate("https://other.example.com/client").assert.isFalse()
  }

  private fun policy(
    allowedHosts: List<String> = listOf(),
    disableSsrf: Boolean = false,
    cimdEnabled: Boolean = true,
    frontendUrl: String? = "https://app.tolgee.example.com",
  ) = CimdClientPolicy(
    OAuth2ServerProperties().apply {
      cimdAllowedHosts = allowedHosts
      this.cimdEnabled = cimdEnabled
    },
    InternalProperties().apply { disableUrlSsrfProtection = disableSsrf },
    mock { on { issuerUrl } doReturn "https://tolgee.example.com" },
    mock { on { stableUrl } doReturn frontendUrl },
  )
}
