package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
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
    check.check(params("Hello 42")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello 42", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello 42")).assertNoIssues()
  }

  @Test
  fun `returns empty when all numbers present`() {
    check.check(params("Bonjour 42 monde", "Hello 42 world")).assertNoIssues()
  }

  @Test
  fun `returns empty when no numbers in base`() {
    check.check(params("Bonjour", "Hello")).assertNoIssues()
  }

  @Test
  fun `detects missing number`() {
    check.check(params("Bonjour monde", "Hello 42 world")).assertSingleIssue {
      message(QaIssueMessage.QA_NUMBERS_MISSING)
      param("number", "42")
      noReplacement()
    }
  }

  @Test
  fun `detects multiple missing numbers`() {
    check.check(params("Bonjour", "Page 5 of 10")).assertIssues {
      issue { param("number", "5") }
      issue { param("number", "10") }
    }
  }

  @Test
  fun `handles decimal numbers`() {
    check.check(params("Prix: EUR", "Price: 9.99 EUR")).assertSingleIssue {
      param("number", "9.99")
    }
  }

  @Test
  fun `handles comma-separated numbers`() {
    check.check(params("Total: EUR", "Total: 1,000 EUR")).assertSingleIssue {
      param("number", "1,000")
    }
  }

  @Test
  fun `does not report numbers only in translation`() {
    check.check(params("Bonjour 42", "Hello world")).assertNoIssues()
  }

  @Test
  fun `all types are MISSING_NUMBERS`() {
    check.check(params("Bonjour", "Hello 42")).assertAllHaveType(QaCheckType.MISSING_NUMBERS)
  }
}
