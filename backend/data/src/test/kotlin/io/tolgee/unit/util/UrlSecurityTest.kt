package io.tolgee.unit.util

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.testing.assert
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class UrlSecurityTest {
  private val urlSecurity = UrlSecurity(InternalProperties())

  @Test
  fun `allows valid external HTTPS URLs`() {
    assertDoesNotThrow { urlSecurity.validateUrl("https://example.com/webhook") }
    assertDoesNotThrow { urlSecurity.validateUrl("https://api.openai.com/v1/chat/completions") }
  }

  @Test
  fun `allows valid external HTTP URLs`() {
    assertDoesNotThrow { urlSecurity.validateUrl("http://example.com/webhook") }
  }

  @Test
  fun `blocks non-HTTP schemes`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("ftp://example.com/file") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("file:///etc/passwd") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("jar:file:///tmp/test.jar!/data") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("gopher://localhost") }
  }

  @Test
  fun `blocks loopback addresses`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://127.0.0.1/admin") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://127.0.0.2/admin") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("https://localhost/admin") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[::1]/admin") }
  }

  @Test
  fun `blocks private network addresses`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://10.0.0.1/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://172.16.0.1/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://192.168.1.1/internal") }
  }

  @Test
  fun `blocks link-local addresses`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://169.254.169.254/latest/meta-data/") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[fe80::1]/internal") }
  }

  @Test
  fun `blocks carrier-grade NAT and other reserved IPv4 ranges`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://100.64.0.1/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://100.127.255.254/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://192.0.0.1/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://198.18.0.1/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://192.88.99.1/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://240.0.0.1/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://255.255.255.255/internal") }
  }

  @Test
  fun `blocks an IPv4 address wearing IPv6 clothes`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[::127.0.0.1]/admin") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[::169.254.169.254]/latest/meta-data/") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[::ffff:10.0.0.1]/internal") }
    // NAT64: a gateway translates 64:ff9b::/96 back to the IPv4 address in the last 32 bits.
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[64:ff9b::127.0.0.1]/admin") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[64:ff9b::a00:1]/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[64:ff9b:1:7f00:0:100:1:1]/admin") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[64:ff9b:1:a00:0:100::]/internal") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[2002:7f00:1::]/admin") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[2002:a00:1::]/internal") }
    // RFC 2765 IPv4-translated: the ffff sits two bytes earlier than in the mapped form.
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[::ffff:0:127.0.0.1]/admin") }
  }

  @Test
  fun `still allows public addresses that merely look unusual`() {
    assertDoesNotThrow { urlSecurity.validateUrl("http://100.63.255.255/") }
    assertDoesNotThrow { urlSecurity.validateUrl("http://100.128.0.1/") }
    assertDoesNotThrow { urlSecurity.validateUrl("http://198.20.0.1/") }
    assertDoesNotThrow { urlSecurity.validateUrl("http://[64:ff9b::8.8.8.8]/") }
  }

  @Test
  fun `blocks wildcard addresses`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://0.0.0.0/") }
  }

  @Test
  fun `blocks multicast addresses`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://224.0.0.1/") }
  }

  @Test
  fun `blocks IPv6 unique-local addresses`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://[fd00::1]/") }
  }

  @Test
  fun `blocks malformed URLs`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("not-a-url") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("") }
    assertThrows<BadRequestException> { urlSecurity.validateUrl("://missing-scheme") }
  }

  @Test
  fun `blocks URLs without host`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://") }
  }

  @Test
  fun `allows local addresses when allowLocalAddresses is true`() {
    assertDoesNotThrow { urlSecurity.validateUrl("http://localhost/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://foo.localhost/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://127.0.0.1/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://192.168.1.10/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://[::1]/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://169.254.169.254/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://0.0.0.0/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://224.0.0.1/webhook", allowLocalAddresses = true) }
    assertDoesNotThrow { urlSecurity.validateUrl("http://[fd00::1]/webhook", allowLocalAddresses = true) }
  }

  @Test
  fun `still validates scheme and host when allowLocalAddresses is true`() {
    assertUrlNotValid { urlSecurity.validateUrl("ftp://example.com/file", allowLocalAddresses = true) }
    assertUrlNotValid { urlSecurity.validateUrl("file:///etc/passwd", allowLocalAddresses = true) }
    assertUrlNotValid { urlSecurity.validateUrl("not-a-url", allowLocalAddresses = true) }
    assertUrlNotValid { urlSecurity.validateUrl("http://", allowLocalAddresses = true) }
  }

  @Test
  fun `validateUrlAndResolve returns the resolved public addresses`() {
    val addresses = urlSecurity.validateUrlAndResolve("https://example.com/.well-known/client")

    addresses.assert.isNotEmpty()
  }

  @Test
  fun `validateUrlAndResolve resolves an IP literal to itself`() {
    val addresses = urlSecurity.validateUrlAndResolve("https://93.184.216.34/x")

    addresses.map { it.hostAddress }.assert.containsExactly("93.184.216.34")
  }

  @Test
  fun `validateUrlAndResolve blocks the same ranges validateUrl does`() {
    listOf(
      "http://127.0.0.1/admin",
      "https://localhost/admin",
      "http://10.0.0.1/internal",
      "http://169.254.169.254/latest/meta-data/",
      "http://[fd00::1]/",
      "ftp://example.com/x",
      "http://",
      "not-a-url",
    ).forEach { url ->
      assertUrlNotValid { urlSecurity.validateUrlAndResolve(url) }
    }
  }

  @Test
  fun `validateUrlAndResolve returns loopback addresses when local addresses are allowed`() {
    val addresses = urlSecurity.validateUrlAndResolve("http://127.0.0.1/x", allowLocalAddresses = true)

    addresses
      .single()
      .isLoopbackAddress.assert
      .isTrue()
  }

  private fun assertUrlNotValid(executable: () -> Unit) {
    val exception = assertThrows<BadRequestException>(executable)
    exception.code.assert.isEqualTo(Message.URL_NOT_VALID.code)
  }
}
