package io.tolgee.unit.util

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.testing.assert
import io.tolgee.util.SsrfSafeDnsResolver
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.UnknownHostException

class SsrfSafeDnsResolverTest {
  private val internalProperties = InternalProperties().apply { disableUrlSsrfProtection = false }
  private val resolver = SsrfSafeDnsResolver(UrlSecurity(internalProperties), internalProperties)

  private val blockedHosts =
    listOf(
      "127.0.0.1", // loopback
      "169.254.169.254", // link-local (cloud metadata)
      "10.0.0.1", // private
      "100.64.0.1", // RFC 6598 shared address space (Kubernetes / Tailscale)
    )

  @Test
  fun `refuses blocked addresses when local addresses are not allowed`() {
    val dnsResolver = resolver.create(allowLocalAddresses = false)
    blockedHosts.forEach { host ->
      assertThrows<UnknownHostException>("expected $host to be refused") { dnsResolver.resolve(host) }
    }
  }

  @Test
  fun `allows blocked addresses when local addresses are allowed`() {
    val dnsResolver = resolver.create(allowLocalAddresses = true)
    blockedHosts.forEach { host ->
      dnsResolver
        .resolve(host)
        .map { it.hostAddress }
        .assert
        .contains(host)
    }
  }

  @Test
  fun `allows blocked addresses when SSRF protection is disabled for dev`() {
    val devProperties = InternalProperties().apply { disableUrlSsrfProtection = true }
    val devResolver = SsrfSafeDnsResolver(UrlSecurity(devProperties), devProperties).create(allowLocalAddresses = false)
    blockedHosts.forEach { host ->
      devResolver
        .resolve(host)
        .map { it.hostAddress }
        .assert
        .contains(host)
    }
  }

  @Test
  fun `allows a public address even when local addresses are not allowed`() {
    val dnsResolver = resolver.create(allowLocalAddresses = false)
    dnsResolver
      .resolve("93.184.216.34")
      .map { it.hostAddress }
      .assert
      .contains("93.184.216.34")
  }

  @Test
  fun `exempts a host the caller marks as skipped`() {
    val dnsResolver = resolver.create(allowLocalAddresses = false, skipHost = { it == "10.0.0.1" })
    dnsResolver
      .resolve("10.0.0.1")
      .map { it.hostAddress }
      .assert
      .contains("10.0.0.1")
    assertThrows<UnknownHostException> { dnsResolver.resolve("127.0.0.1") }
  }
}
