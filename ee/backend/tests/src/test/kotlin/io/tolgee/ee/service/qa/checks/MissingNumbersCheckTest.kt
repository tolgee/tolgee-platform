package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MissingNumbersCheckTest {
  private val check = MissingNumbersCheck()

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
    val results = check.check(params("Hello 42"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("Hello 42", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    val results = check.check(params("  ", "Hello 42"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when all numbers present`() {
    val results = check.check(params("Bonjour 42 monde", "Hello 42 world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when no numbers in base`() {
    val results = check.check(params("Bonjour", "Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects missing number`() {
    val results = check.check(params("Bonjour monde", "Hello 42 world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_NUMBERS_MISSING)
    assertThat(results[0].params).containsEntry("number", "42")
    assertThat(results[0].replacement).isNull()
  }

  @Test
  fun `detects multiple missing numbers`() {
    val results = check.check(params("Bonjour", "Page 5 of 10"))
    assertThat(results).hasSize(2)
    val numbers = results.map { it.params?.get("number") }.toSet()
    assertThat(numbers).containsExactlyInAnyOrder("5", "10")
  }

  @Test
  fun `handles decimal numbers`() {
    val results = check.check(params("Prix: EUR", "Price: 9.99 EUR"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("number", "9.99")
  }

  @Test
  fun `handles comma-separated numbers`() {
    val results = check.check(params("Total: EUR", "Total: 1,000 EUR"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params).containsEntry("number", "1,000")
  }

  @Test
  fun `does not report numbers only in translation`() {
    val results = check.check(params("Bonjour 42", "Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `all types are MISSING_NUMBERS`() {
    val results = check.check(params("Bonjour", "Hello 42"))
    assertThat(results).allMatch { it.type == QaCheckType.MISSING_NUMBERS }
  }
}
