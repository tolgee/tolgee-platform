package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpacesMismatchCheckTest {
  private val check = SpacesMismatchCheck()

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
    val results = check.check(params("Hello"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("Hello", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when spaces match`() {
    val results = check.check(params("Hello world", "Hi world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects extra leading spaces`() {
    val results = check.check(params("  Hello", "Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_LEADING_ADDED)
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(2)
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `detects missing leading spaces`() {
    val results = check.check(params("Hello", "  Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_LEADING_REMOVED)
    assertThat(results[0].replacement).isEqualTo("  ")
  }

  @Test
  fun `detects extra trailing spaces`() {
    val results = check.check(params("Hello  ", "Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_TRAILING_ADDED)
    assertThat(results[0].positionStart).isEqualTo(5)
    assertThat(results[0].positionEnd).isEqualTo(7)
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `detects missing trailing spaces`() {
    val results = check.check(params("Hello", "Hello  "))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_TRAILING_REMOVED)
    assertThat(results[0].replacement).isEqualTo("  ")
  }

  @Test
  fun `detects doubled spaces`() {
    val results = check.check(params("Hello  world", "Hello world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_DOUBLED)
    assertThat(results[0].positionStart).isEqualTo(6)
    assertThat(results[0].positionEnd).isEqualTo(7)
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `detects multiple doubled spaces`() {
    val results = check.check(params("A  B  C", "A B C"))
    val doubled = results.filter { it.message == QaIssueMessage.QA_SPACES_DOUBLED }
    assertThat(doubled).hasSize(2)
  }

  @Test
  fun `detects non-breaking space added`() {
    val results = check.check(params("Hello\u00A0world", "Hello world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_NON_BREAKING_ADDED)
    assertThat(results[0].positionStart).isEqualTo(5)
    assertThat(results[0].positionEnd).isEqualTo(6)
    assertThat(results[0].replacement).isEqualTo(" ")
  }

  @Test
  fun `detects non-breaking space removed`() {
    val results = check.check(params("Hello world", "Hello\u00A0world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_NON_BREAKING_REMOVED)
    assertThat(results[0].replacement).isNull()
  }

  @Test
  fun `all types are SPACES_MISMATCH`() {
    val results = check.check(params("  Hello  world  ", "Hello world"))
    assertThat(results).allMatch { it.type == QaCheckType.SPACES_MISMATCH }
  }
}
