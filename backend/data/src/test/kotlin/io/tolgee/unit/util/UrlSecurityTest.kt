package io.tolgee.unit.util

import io.tolgee.exceptions.BadRequestException
import io.tolgee.util.UrlSecurity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class UrlSecurityTest {
  @Test
  fun `allows valid external HTTPS URLs`() {
    assertDoesNotThrow { UrlSecurity.validateUrl("https://example.com/webhook") }
    assertDoesNotThrow { UrlSecurity.validateUrl("https://api.openai.com/v1/chat/completions") }
  }

  @Test
  fun `allows valid external HTTP URLs`() {
    assertDoesNotThrow { UrlSecurity.validateUrl("http://example.com/webhook") }
  }

  @Test
  fun `blocks non-HTTP schemes`() {
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("ftp://example.com/file") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("file:///etc/passwd") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("jar:file:///tmp/test.jar!/data") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("gopher://localhost") }
  }

  @Test
  fun `blocks loopback addresses`() {
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://127.0.0.1/admin") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://127.0.0.2/admin") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("https://localhost/admin") }
  }

  @Test
  fun `blocks private network addresses`() {
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://10.0.0.1/internal") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://172.16.0.1/internal") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://192.168.1.1/internal") }
  }

  @Test
  fun `blocks link-local addresses`() {
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://169.254.169.254/latest/meta-data/") }
  }

  @Test
  fun `blocks wildcard addresses`() {
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://0.0.0.0/") }
  }

  @Test
  fun `blocks malformed URLs`() {
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("not-a-url") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("") }
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("://missing-scheme") }
  }

  @Test
  fun `blocks URLs without host`() {
    assertThrows<BadRequestException> { UrlSecurity.validateUrl("http://") }
  }
}
