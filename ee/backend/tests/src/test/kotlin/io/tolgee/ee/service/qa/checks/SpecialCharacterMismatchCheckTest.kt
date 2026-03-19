package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpecialCharacterMismatchCheckTest {
  private val check = SpecialCharacterMismatchCheck()

  private fun params(
    text: String,
    base: String? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
  )

  @Test
  fun `returns empty when base is null`() {
    val results = check.check(params("Price: \$10"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("Price: \$10", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    val results = check.check(params("  ", "Price: \$10"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when no special chars in either`() {
    val results = check.check(params("Bonjour monde", "Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when special chars match exactly`() {
    val results = check.check(params("Cena: \$10 @domov", "Price: \$10 @home"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects missing special character`() {
    val results = check.check(params("Cena: 10", "Price: \$10"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
    assertThat(results[0].params).containsEntry("character", "\$")
    assertThat(results[0].replacement).isNull()
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(0)
  }

  @Test
  fun `detects multiple missing special characters`() {
    val results = check.check(params("10 domov 2024", "\$10 @home ©2024"))
    assertThat(results).hasSize(3)
    val chars = results.map { it.params?.get("character") }.toSet()
    assertThat(chars).containsExactlyInAnyOrder("\$", "@", "©")
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_SPECIAL_CHAR_MISSING }
  }

  @Test
  fun `detects missing when count differs`() {
    val results = check.check(params("\$10 sleva", "\$10 - \$5 sleva"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
    assertThat(results[0].params).containsEntry("character", "\$")
  }

  @Test
  fun `detects added special character`() {
    val results = check.check(params("Bonjour ©monde", "Hello world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPECIAL_CHAR_ADDED)
    assertThat(results[0].params).containsEntry("character", "©")
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `detects added character with correct position`() {
    val results = check.check(params("Hel\$lo", "Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].positionStart).isEqualTo(3)
    assertThat(results[0].positionEnd).isEqualTo(4)
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `detects both missing and added`() {
    val results = check.check(params("€10 prix", "\$10 price"))
    assertThat(results).hasSize(2)
    val missing = results.filter { it.message == QaIssueMessage.QA_SPECIAL_CHAR_MISSING }
    val added = results.filter { it.message == QaIssueMessage.QA_SPECIAL_CHAR_ADDED }
    assertThat(missing).hasSize(1)
    assertThat(missing[0].params).containsEntry("character", "\$")
    assertThat(added).hasSize(1)
    assertThat(added[0].params).containsEntry("character", "€")
  }

  @Test
  fun `all types are SPECIAL_CHARACTER_MISMATCH`() {
    val results = check.check(params("€10 prix", "\$10 price"))
    assertThat(results).allMatch { it.type == QaCheckType.SPECIAL_CHARACTER_MISMATCH }
  }

  @Test
  fun `detects all supported special characters`() {
    for (char in SpecialCharacterMismatchCheck.SPECIAL_CHARS) {
      val results = check.check(params("text", "text$char"))
      assertThat(results)
        .withFailMessage("Should detect missing '$char'")
        .hasSize(1)
      assertThat(results[0].params).containsEntry("character", char.toString())
    }
  }

  @Test
  fun `does not flag non-special characters`() {
    val results = check.check(params("Hello world", "Hello world!?.:;"))
    assertThat(results).isEmpty()
  }
}
