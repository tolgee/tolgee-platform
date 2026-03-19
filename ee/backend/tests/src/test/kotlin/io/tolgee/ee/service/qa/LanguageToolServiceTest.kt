package io.tolgee.ee.service.qa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LanguageToolServiceTest {
  private val service = LanguageToolService()

  @Test
  fun `returns results for supported language`() {
    val results = service.check("Ths is a tset.", "en")
    assertThat(results).isNotEmpty
  }

  @Test
  fun `returns empty for blank text`() {
    val results = service.check("   ", "en")
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for unsupported language`() {
    val results = service.check("some text", "xx-unsupported")
    assertThat(results).isEmpty()
  }

  @Test
  fun `resolves base language from regional tag`() {
    val results = service.check("Ths is a tset.", "en-US")
    assertThat(results).isNotEmpty
  }

  @Test
  fun `isLanguageSupported returns true for supported language`() {
    assertThat(service.isLanguageSupported("en")).isTrue()
    assertThat(service.isLanguageSupported("de")).isTrue()
  }

  @Test
  fun `isLanguageSupported returns false for unsupported language`() {
    assertThat(service.isLanguageSupported("xx-unsupported")).isFalse()
  }

  @Test
  fun `returns correct positions for misspelled word`() {
    // "Helo world" — "Helo" at position 0-4
    val results = service.check("Helo world", "en")
    val spellingResult = results.find { it.fromPos == 0 }
    assertThat(spellingResult).isNotNull
    assertThat(spellingResult!!.toPos).isEqualTo(4)
  }

  @Test
  fun `returns no issues for correct text`() {
    val results = service.check("This is a correct sentence.", "en")
    assertThat(results).isEmpty()
  }
}
