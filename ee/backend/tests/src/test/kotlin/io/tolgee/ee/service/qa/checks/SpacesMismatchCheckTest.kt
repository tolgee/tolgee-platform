package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
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
    check.check(params("Hello")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when spaces match`() {
    check.check(params("Hello world", "Hi world")).assertNoIssues()
  }

  @Test
  fun `detects extra leading spaces`() {
    check.check(params("  Hello", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_LEADING_ADDED)
      position(0, 2)
      replacement("")
    }
  }

  @Test
  fun `detects missing leading spaces`() {
    check.check(params("Hello", "  Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_LEADING_REMOVED)
      replacement("  ")
    }
  }

  @Test
  fun `detects extra trailing spaces`() {
    check.check(params("Hello  ", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_TRAILING_ADDED)
      position(5, 7)
      replacement("")
    }
  }

  @Test
  fun `detects missing trailing spaces`() {
    check.check(params("Hello", "Hello  ")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_TRAILING_REMOVED)
      replacement("  ")
    }
  }

  @Test
  fun `detects doubled spaces`() {
    check.check(params("Hello  world", "Hello world")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_DOUBLED)
      position(6, 7)
      replacement("")
    }
  }

  @Test
  fun `detects multiple doubled spaces`() {
    check.check(params("A  B  C", "A B C")).assertIssues {
      issue { message(QaIssueMessage.QA_SPACES_DOUBLED) }
      issue { message(QaIssueMessage.QA_SPACES_DOUBLED) }
    }
  }

  @Test
  fun `detects doubled non-breaking spaces`() {
    val results = check.check(params("Hello\u00A0\u00A0world", "Hello world"))
    results
      .filter { it.message == QaIssueMessage.QA_SPACES_DOUBLED }
      .assertSingleIssue {}
  }

  @Test
  fun `detects mixed regular and non-breaking doubled spaces`() {
    val results = check.check(params("Hello \u00A0world", "Hello world"))
    results
      .filter { it.message == QaIssueMessage.QA_SPACES_DOUBLED }
      .assertSingleIssue {}
  }

  @Test
  fun `detects leading nbsp structure mismatch`() {
    check.check(params(" Hello", "\u00A0Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_LEADING_ADDED)
      position(0, 1)
      replacement("\u00A0")
    }
  }

  @Test
  fun `detects trailing nbsp structure mismatch`() {
    check.check(params("Hello ", "Hello\u00A0")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_TRAILING_ADDED)
      position(5, 6)
      replacement("\u00A0")
    }
  }

  @Test
  fun `detects extra leading nbsp`() {
    check.check(params("\u00A0Hello", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_LEADING_ADDED)
      position(0, 1)
      replacement("")
    }
  }

  @Test
  fun `detects missing leading nbsp`() {
    check.check(params("Hello", "\u00A0Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_SPACES_LEADING_REMOVED)
      position(0, 0)
      replacement("\u00A0")
    }
  }

  @Test
  fun `minimal edit preserves common prefix and suffix`() {
    val results = check.check(params("\u00A0  Hello", "\u00A0 \u00A0Hello"))
    results
      .filter {
        it.message == QaIssueMessage.QA_SPACES_LEADING_ADDED ||
          it.message == QaIssueMessage.QA_SPACES_LEADING_REMOVED
      }.assertSingleIssue {
        position(2, 3)
        replacement("\u00A0")
      }
  }

  @Test
  fun `no issue when mid-text nbsp differs but edges match`() {
    check.check(params("Hello\u00A0world", "Hello world")).assertNoIssues()
  }

  @Test
  fun `all types are SPACES_MISMATCH`() {
    check.check(params("  Hello  world  ", "Hello world")).assertAllHaveType(QaCheckType.SPACES_MISMATCH)
  }
}
