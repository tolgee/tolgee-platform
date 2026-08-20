package io.tolgee.unit.apps

import io.tolgee.service.apps.AppIconResolver
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class AppIconResolverTest {
  @Test
  fun `an emoji passes through unchanged`() {
    resolve("🧩").assert.isEqualTo("🧩")
  }

  @Test
  fun `a native icon name passes through unchanged`() {
    resolve("Key01").assert.isEqualTo("Key01")
  }

  @Test
  fun `a relative image URL resolves against the base URL`() {
    resolve("/assets/logo.svg").assert.isEqualTo("https://app.example.com/assets/logo.svg")
  }

  @Test
  fun `an absolute http image URL is kept`() {
    resolve("https://cdn.example.com/logo.png").assert.isEqualTo("https://cdn.example.com/logo.png")
  }

  @Test
  fun `a blank icon resolves to nothing`() {
    resolve(null).assert.isNull()
    resolve("  ").assert.isNull()
  }

  @Test
  fun `a slash-free value carrying a URI scheme is refused`() {
    errorOf("javascript:alert(1)").assert.contains("emoji, a native icon name, or an image URL")
  }

  @Test
  fun `a non-http URL is refused`() {
    errorOf("javascript://alert(1)").assert.contains("absolute http(s) URL")
    errorOf("file:///etc/passwd").assert.contains("absolute http(s) URL")
    errorOf("data:image/png;base64,AAAA").assert.contains("absolute http(s) URL")
  }

  @Test
  fun `an over-long icon is refused`() {
    errorOf("https://cdn.example.com/" + "a".repeat(500)).assert.contains("exceeds")
  }

  @Test
  fun `validate collects the same error resolve would hit`() {
    val errors = mutableListOf<String>()
    AppIconResolver().validate("file:///etc/passwd", BASE_URL, errors)
    errors.assert.hasSize(1)
  }

  private fun resolve(icon: String?): String? = AppIconResolver().resolve(icon, BASE_URL)

  private fun errorOf(icon: String): String {
    val errors = mutableListOf<String>()
    AppIconResolver().validate(icon, BASE_URL, errors)
    errors.assert.hasSize(1)
    return errors.single()
  }

  companion object {
    private const val BASE_URL = "https://app.example.com"
  }
}
