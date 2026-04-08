package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class BracketsBalanceCheckTest {
  private val check = BracketsBalanceCheck()

  private fun params(
    text: String,
    base: String? = null,
    icuPlaceholders: Boolean = true,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
    icuPlaceholders = icuPlaceholders,
  )

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ")).assertNoIssues()
  }

  @Test
  fun `runs even when base is null`() {
    check.check(params("Hello (world")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_UNCLOSED)
    }
  }

  @Test
  fun `returns empty when brackets are balanced`() {
    check.check(params("Hello (world)")).assertNoIssues()
  }

  @Test
  fun `returns empty when no brackets`() {
    check.check(params("Hello world")).assertNoIssues()
  }

  @Test
  fun `detects unclosed opening bracket`() {
    check.check(params("Hello (world")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_UNCLOSED)
      param("brackets", "(")
      param("count", "1")
      replacement(")")
      position(12, 12)
    }
  }

  @Test
  fun `detects unmatched closing bracket`() {
    check.check(params("Hello world)")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_UNMATCHED_CLOSE)
      param("bracket", ")")
      replacement("")
      position(11, 12)
    }
  }

  @Test
  fun `detects mismatched bracket types`() {
    check.check(params("Hello (world]")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_UNMATCHED_CLOSE)
        param("bracket", "]")
        replacement("")
        position(12, 13)
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_UNCLOSED)
        param("brackets", "(")
        param("count", "1")
        replacement(")")
      }
    }
  }

  @Test
  fun `handles nested brackets`() {
    check.check(params("Hello ((world))")).assertNoIssues()
  }

  @Test
  fun `handles multiple bracket types`() {
    check.check(params("Hello (world) [test]")).assertNoIssues()
  }

  @Test
  fun `handles complex nesting`() {
    check.check(params("Hello ([world])")).assertNoIssues()
  }

  @Test
  fun `detects multiple unclosed brackets`() {
    // Multiple unclosed brackets are aggregated into a single issue
    check.check(params("Hello (world [test")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_UNCLOSED)
      param("brackets", "(, [")
      param("count", "2")
      replacement("])")
      position(18, 18)
    }
  }

  @Test
  fun `handles curly braces when ICU is disabled`() {
    check.check(params("Hello {world}", icuPlaceholders = false)).assertNoIssues()
  }

  @Test
  fun `detects unclosed curly brace when ICU is disabled`() {
    check.check(params("Hello {world", icuPlaceholders = false)).assertSingleIssue {
      replacement("}")
    }
  }

  @Test
  fun `all results have BRACKETS_UNBALANCED type`() {
    check
      .check(
        params("Hello (world] [test", icuPlaceholders = false),
      ).assertAllHaveType(QaCheckType.BRACKETS_UNBALANCED)
  }

  // ICU placeholder tests

  @Test
  fun `ignores curly braces when ICU is enabled`() {
    check.check(params("Hello {world}")).assertNoIssues()
  }

  @Test
  fun `ignores unclosed curly brace when ICU is enabled`() {
    check.check(params("Hello {world")).assertNoIssues()
  }

  @Test
  fun `still detects unclosed round bracket alongside ICU text`() {
    check.check(params("{count} items (extra")).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_UNCLOSED)
      param("brackets", "(")
      param("count", "1")
    }
  }

  @Test
  fun `checks curly braces when ICU is disabled`() {
    check.check(params("Hello {world", icuPlaceholders = false)).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_UNCLOSED)
      param("brackets", "{")
      param("count", "1")
    }
  }

  @Test
  fun `ignores extra closing curly brace when ICU is enabled`() {
    check.check(params("Hello world}")).assertNoIssues()
  }

  @Test
  fun `detects extra closing curly brace when ICU is disabled`() {
    check.check(params("Hello world}", icuPlaceholders = false)).assertSingleIssue {
      message(QaIssueMessage.QA_BRACKETS_UNMATCHED_CLOSE)
      param("bracket", "}")
    }
  }
}
