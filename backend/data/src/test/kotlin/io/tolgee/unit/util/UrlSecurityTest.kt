package io.tolgee.unit.util

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.exceptions.BadRequestException
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
  }

  @Test
  fun `blocks wildcard addresses`() {
    assertThrows<BadRequestException> { urlSecurity.validateUrl("http://0.0.0.0/") }
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
}
