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

  @Test
  fun `resolves base language to its default variant`() {
    assertThat(service.resolveLanguage("en")?.shortCodeWithCountryAndVariant).isEqualTo("en-US")
    assertThat(service.resolveLanguage("de")?.shortCodeWithCountryAndVariant).isEqualTo("de-DE")
  }

  @Test
  fun `preserves regional variant as-is`() {
    assertThat(service.resolveLanguage("en-US")?.shortCodeWithCountryAndVariant).isEqualTo("en-US")
    assertThat(service.resolveLanguage("en-GB")?.shortCodeWithCountryAndVariant).isEqualTo("en-GB")
    assertThat(service.resolveLanguage("pt-BR")?.shortCodeWithCountryAndVariant).isEqualTo("pt-BR")
    assertThat(service.resolveLanguage("pt-PT")?.shortCodeWithCountryAndVariant).isEqualTo("pt-PT")
  }

  @Test
  fun `resolves single-variant language to itself`() {
    assertThat(service.resolveLanguage("fr")?.shortCodeWithCountryAndVariant).isEqualTo("fr")
  }

  @Test
  fun `resolves underscore-separated tag via base code fallback`() {
    // LanguageTool only recognizes hyphens, so underscored tags fail exact match
    // and fall back to base code (the part before "_") → defaultLanguageVariant.
    assertThat(service.resolveLanguage("pt_BR")?.shortCodeWithCountryAndVariant).isEqualTo("pt-PT")
    assertThat(service.resolveLanguage("de_AT")?.shortCodeWithCountryAndVariant).isEqualTo("de-DE")
  }

  @Test
  fun `resolves unknown regional variant via base code fallback`() {
    assertThat(service.resolveLanguage("en-XX")?.shortCodeWithCountryAndVariant).isEqualTo("en-US")
  }

  @Test
  fun `returns null for unsupported language tags`() {
    assertThat(service.resolveLanguage("")).isNull()
    assertThat(service.resolveLanguage("xx")).isNull()
    assertThat(service.resolveLanguage("xx-YY")).isNull()
    assertThat(service.resolveLanguage("xyz-abc-def")).isNull()
  }

  @Test
  fun `handles case sensitivity in language tags`() {
    assertThat(service.resolveLanguage("en")?.shortCodeWithCountryAndVariant).isEqualTo("en-US")
    assertThat(service.resolveLanguage("EN")?.shortCodeWithCountryAndVariant).isEqualTo("en-US")
  }

  @Test
  fun `resolves valid multi-segment BCP-47 tag to itself`() {
    assertThat(service.resolveLanguage("ca-ES-valencia")?.shortCodeWithCountryAndVariant).isEqualTo("ca-ES-valencia")
  }
}
