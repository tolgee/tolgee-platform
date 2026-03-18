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
  fun `detects doubled non-breaking spaces`() {
    val results = check.check(params("Hello\u00A0\u00A0world", "Hello world"))
    val doubled = results.filter { it.message == QaIssueMessage.QA_SPACES_DOUBLED }
    assertThat(doubled).hasSize(1)
  }

  @Test
  fun `detects mixed regular and non-breaking doubled spaces`() {
    val results = check.check(params("Hello \u00A0world", "Hello world"))
    val doubled = results.filter { it.message == QaIssueMessage.QA_SPACES_DOUBLED }
    assertThat(doubled).hasSize(1)
  }

  @Test
  fun `detects leading nbsp structure mismatch`() {
    // base has leading nbsp, translation has regular space — structural edit
    val results = check.check(params(" Hello", "\u00A0Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_LEADING_ADDED)
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(1)
    assertThat(results[0].replacement).isEqualTo("\u00A0")
  }

  @Test
  fun `detects trailing nbsp structure mismatch`() {
    // base has trailing nbsp, translation has regular space
    val results = check.check(params("Hello ", "Hello\u00A0"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_TRAILING_ADDED)
    assertThat(results[0].positionStart).isEqualTo(5)
    assertThat(results[0].positionEnd).isEqualTo(6)
    assertThat(results[0].replacement).isEqualTo("\u00A0")
  }

  @Test
  fun `detects extra leading nbsp`() {
    // base has no leading whitespace, translation has a leading nbsp
    val results = check.check(params("\u00A0Hello", "Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_LEADING_ADDED)
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(1)
    assertThat(results[0].replacement).isEqualTo("")
  }

  @Test
  fun `detects missing leading nbsp`() {
    // base has leading nbsp, translation has none
    val results = check.check(params("Hello", "\u00A0Hello"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_SPACES_LEADING_REMOVED)
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(0)
    assertThat(results[0].replacement).isEqualTo("\u00A0")
  }

  @Test
  fun `minimal edit preserves common prefix and suffix`() {
    // base: nbsp + space + nbsp, translation: nbsp + space + space
    // only the last char differs — minimal edit should target just that char
    val results = check.check(params("\u00A0  Hello", "\u00A0 \u00A0Hello"))
    val leading = results.filter {
      it.message == QaIssueMessage.QA_SPACES_LEADING_ADDED ||
        it.message == QaIssueMessage.QA_SPACES_LEADING_REMOVED
    }
    assertThat(leading).hasSize(1)
    assertThat(leading[0].positionStart).isEqualTo(2)
    assertThat(leading[0].positionEnd).isEqualTo(3)
    assertThat(leading[0].replacement).isEqualTo("\u00A0")
  }

  @Test
  fun `no issue when mid-text nbsp differs but edges match`() {
    // nbsp in the middle of text is no longer checked
    val results = check.check(params("Hello\u00A0world", "Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `all types are SPACES_MISMATCH`() {
    val results = check.check(params("  Hello  world  ", "Hello world"))
    assertThat(results).allMatch { it.type == QaCheckType.SPACES_MISMATCH }
  }
}
