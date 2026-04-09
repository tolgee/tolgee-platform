package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
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
    check.check(params("hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("hello", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when case matches - both upper`() {
    check.check(params("Bonjour", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when case matches - both lower`() {
    check.check(params("bonjour", "hello")).assertNoIssues()
  }

  @Test
  fun `detects lowercase when base is capitalized`() {
    check.check(params("bonjour", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      replacement("B")
      position(0, 1)
    }
  }

  @Test
  fun `detects uppercase when base is lowercase`() {
    check.check(params("Bonjour", "hello")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_LOWERCASE)
      replacement("b")
      position(0, 1)
    }
  }

  @Test
  fun `skips leading non-letter characters`() {
    check.check(params("  123 bonjour", "  123 Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      position(6, 7)
    }
  }

  @Test
  fun `returns empty when no letters in text`() {
    check.check(params("123", "Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when no letters in base`() {
    check.check(params("Hello", "123")).assertNoIssues()
  }

  @Test
  fun `uses Turkish locale for uppercase conversion`() {
    check.check(params("istanbul", "Hello", languageTag = "tr")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_CAPITALIZE)
      replacement("\u0130") // Turkish capital I with dot
    }
  }

  @Test
  fun `uses Turkish locale for lowercase conversion`() {
    check.check(params("Istanbul", "hello", languageTag = "tr")).assertSingleIssue {
      message(QaIssueMessage.QA_CASE_LOWERCASE)
      replacement("\u0131") // Turkish lowercase dotless i
    }
  }

  @Test
  fun `all types are CHARACTER_CASE_MISMATCH`() {
    check.check(params("bonjour", "Hello")).assertAllHaveType(QaCheckType.CHARACTER_CASE_MISMATCH)
  }
}
