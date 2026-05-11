package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
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
    check.check(params("Hello.")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello.", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello.")).assertNoIssues()
  }

  @Test
  fun `returns empty when punctuation matches`() {
    check.check(params("Bonjour.", "Hello.")).assertNoIssues()
  }

  @Test
  fun `returns empty when neither has punctuation`() {
    check.check(params("Bonjour", "Hello")).assertNoIssues()
  }

  @Test
  fun `detects missing punctuation - add`() {
    check.check(params("Bonjour", "Hello.")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_ADD)
      param("punctuation", ".")
      replacement(".")
    }
  }

  @Test
  fun `detects extra punctuation - remove`() {
    check.check(params("Bonjour!", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REMOVE)
      param("punctuation", "!")
      replacement("")
    }
  }

  @Test
  fun `detects different punctuation - replace`() {
    check.check(params("Bonjour!", "Hello.")).assertSingleIssue {
      message(QaIssueMessage.QA_PUNCTUATION_REPLACE)
      param("punctuation", "!")
      param("expected", ".")
      replacement(".")
    }
  }

  @Test
  fun `ignores trailing whitespace`() {
    check.check(params("Bonjour.  ", "Hello.  ")).assertNoIssues()
  }

  @Test
  fun `handles all punctuation types`() {
    for (punct in listOf('.', ',', '!', '?', ':', ';')) {
      check.check(params("Bonjour", "Hello$punct")).assertSingleIssue {
        param("punctuation", punct.toString())
      }
    }
  }

  @Test
  fun `all types are PUNCTUATION_MISMATCH`() {
    check.check(params("Bonjour!", "Hello.")).assertAllHaveType(QaCheckType.PUNCTUATION_MISMATCH)
  }
}
