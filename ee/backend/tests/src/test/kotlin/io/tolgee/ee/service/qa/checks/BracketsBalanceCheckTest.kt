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
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
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
      param("bracket", "(")
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
        param("bracket", "]")
        replacement("")
        position(12, 13)
      }
      issue {
        param("bracket", "(")
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
    check.check(params("Hello (world [test")).assertIssues {
      issue { message(QaIssueMessage.QA_BRACKETS_UNCLOSED) }
      issue { message(QaIssueMessage.QA_BRACKETS_UNCLOSED) }
    }
  }

  @Test
  fun `handles curly braces`() {
    check.check(params("Hello {world}")).assertNoIssues()
  }

  @Test
  fun `detects unclosed curly brace`() {
    check.check(params("Hello {world")).assertSingleIssue {
      replacement("}")
    }
  }

  @Test
  fun `all results have BRACKETS_UNBALANCED type`() {
    check.check(params("Hello (world] {test")).assertAllHaveType(QaCheckType.BRACKETS_UNBALANCED)
  }
}
