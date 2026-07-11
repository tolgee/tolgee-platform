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

  private fun assertUrlNotValid(executable: () -> Unit) {
    val exception = assertThrows<BadRequestException>(executable)
    exception.code.assert.isEqualTo(Message.URL_NOT_VALID.code)
  }
}
