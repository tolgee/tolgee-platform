package io.tolgee.unit.apps

import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.apps.AppIconResolver
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
  fun `an absolute image URL on the app's own origin is kept`() {
    resolve("https://app.example.com/logo.png").assert.isEqualTo("https://app.example.com/logo.png")
  }

  @Test
  fun `a blank icon resolves to nothing`() {
    resolve(null).assert.isNull()
    resolve("  ").assert.isNull()
  }

  @Test
  fun `a valid icon has no errors`() {
    errorsOf("🧩").assert.isEmpty()
    errorsOf("/assets/logo.svg").assert.isEmpty()
  }

  @Test
  fun `a slash-free value carrying a URI scheme is refused`() {
    errorOf("javascript:alert(1)").assert.contains("emoji, a native icon name, or an image URL")
  }

  @Test
  fun `a slash-free value carrying markup or whitespace is refused`() {
    errorOf("<img src=x onerror=alert(1)>").assert.contains("emoji, a native icon name, or an image URL")
    errorOf("two words").assert.contains("emoji, a native icon name, or an image URL")
  }

  @Test
  fun `a malformed image URL is refused`() {
    errorOf("/logo icon.svg").assert.contains("is not a valid URL")
  }

  @Test
  fun `resolve throws with all errors when the icon is invalid`() {
    val exception =
      assertThrows<BadRequestException> { AppIconResolver("https://cdn.example.com/logo.png", BASE_URL).resolve() }
    exception.params!!.assert.contains("must be on the app's own origin")
  }

  @Test
  fun `a non-http URL is refused`() {
    errorOf("javascript://alert(1)").assert.contains("absolute http(s) URL")
    errorOf("file:///etc/passwd").assert.contains("absolute http(s) URL")
    errorOf("data:image/png;base64,AAAA").assert.contains("absolute http(s) URL")
  }

  @Test
  fun `an image URL on another origin is refused`() {
    errorOf("https://cdn.example.com/logo.png").assert.contains("must be on the app's own origin")
  }

  @Test
  fun `an over-long icon is refused`() {
    errorOf("https://app.example.com/" + "a".repeat(500)).assert.contains("exceeds")
  }

  private fun resolve(icon: String?): String? = AppIconResolver(icon, BASE_URL).resolve()

  private fun errorsOf(icon: String?): List<String> = AppIconResolver(icon, BASE_URL).collectErrors()

  private fun errorOf(icon: String): String {
    val errors = errorsOf(icon)
    errors.assert.hasSize(1)
    return errors.single()
  }

  companion object {
    private const val BASE_URL = "https://app.example.com"
  }
}
