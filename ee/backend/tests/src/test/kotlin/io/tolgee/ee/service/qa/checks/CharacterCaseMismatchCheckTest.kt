package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CharacterCaseMismatchCheckTest {
  private val check = CharacterCaseMismatchCheck()

  private fun params(
    text: String,
    base: String? = null,
    baseLanguageTag: String? = null,
    languageTag: String = "en",
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = baseLanguageTag,
    languageTag = languageTag,
  )

  @Test
  fun `returns empty when base is null`() {
    val results = check.check(params("hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("hello", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    val results = check.check(params("  ", "Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when case matches - both upper`() {
    val results = check.check(params("Bonjour", "Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when case matches - both lower`() {
    val results = check.check(params("bonjour", "hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects lowercase when base is capitalized`() {
    val results = check.check(params("bonjour", "Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_CASE_CAPITALIZE)
    assertThat(results[0].replacement).isEqualTo("B")
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(1)
  }

  @Test
  fun `detects uppercase when base is lowercase`() {
    val results = check.check(params("Bonjour", "hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_CASE_LOWERCASE)
    assertThat(results[0].replacement).isEqualTo("b")
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(1)
  }

  @Test
  fun `skips leading non-letter characters`() {
    val results = check.check(params("  123 bonjour", "  123 Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_CASE_CAPITALIZE)
    assertThat(results[0].positionStart).isEqualTo(6)
    assertThat(results[0].positionEnd).isEqualTo(7)
  }

  @Test
  fun `returns empty when no letters in text`() {
    val results = check.check(params("123", "Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when no letters in base`() {
    val results = check.check(params("Hello", "123"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `uses Turkish locale for uppercase conversion`() {
    val results = check.check(params("istanbul", "Hello", languageTag = "tr"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_CASE_CAPITALIZE)
    assertThat(results[0].replacement).isEqualTo("\u0130") // İ (Turkish capital I with dot)
  }

  @Test
  fun `uses Turkish locale for lowercase conversion`() {
    val results = check.check(params("Istanbul", "hello", languageTag = "tr"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_CASE_LOWERCASE)
    assertThat(results[0].replacement).isEqualTo("\u0131") // ı (Turkish lowercase dotless i)
  }

  @Test
  fun `all types are CHARACTER_CASE_MISMATCH`() {
    val results = check.check(params("bonjour", "Hello"))
    assertThat(results).allMatch { it.type == QaCheckType.CHARACTER_CASE_MISMATCH }
  }
}
