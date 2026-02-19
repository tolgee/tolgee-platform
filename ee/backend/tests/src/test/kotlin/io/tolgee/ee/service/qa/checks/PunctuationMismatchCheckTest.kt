package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PunctuationMismatchCheckTest {
  private val check = PunctuationMismatchCheck()

  private fun params(
    text: String,
    base: String? = null,
    baseLanguageTag: String? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = baseLanguageTag,
    languageTag = "en",
  )

  @Test
  fun `returns empty when base is null`() {
    val results = check.check(params("Hello."))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("Hello.", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    val results = check.check(params("  ", "Hello."))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when punctuation matches`() {
    val results = check.check(params("Bonjour.", "Hello."))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when neither has punctuation`() {
    val results = check.check(params("Bonjour", "Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects missing punctuation - add`() {
    val results = check.check(params("Bonjour", "Hello."))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_PUNCTUATION_ADD)
    assertThat(results[0].params).containsEntry("punctuation", ".")
    assertThat(results[0].replacement).isEqualTo("Bonjour.")
  }

  @Test
  fun `detects extra punctuation - remove`() {
    val results = check.check(params("Bonjour!", "Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_PUNCTUATION_REMOVE)
    assertThat(results[0].params).containsEntry("punctuation", "!")
    assertThat(results[0].replacement).isEqualTo("Bonjour")
  }

  @Test
  fun `detects different punctuation - replace`() {
    val results = check.check(params("Bonjour!", "Hello."))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_PUNCTUATION_REPLACE)
    assertThat(results[0].params).containsEntry("punctuation", "!")
    assertThat(results[0].params).containsEntry("expected", ".")
    assertThat(results[0].replacement).isEqualTo("Bonjour.")
  }

  @Test
  fun `ignores trailing whitespace`() {
    val results = check.check(params("Bonjour.  ", "Hello.  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles all punctuation types`() {
    for (punct in listOf('.', ',', '!', '?', ':', ';')) {
      val results = check.check(params("Bonjour", "Hello$punct"))
      assertThat(results).hasSize(1)
      assertThat(results[0].params).containsEntry("punctuation", punct.toString())
    }
  }

  @Test
  fun `all types are PUNCTUATION_MISMATCH`() {
    val results = check.check(params("Bonjour!", "Hello."))
    assertThat(results).allMatch { it.type == QaCheckType.PUNCTUATION_MISMATCH }
  }
}
