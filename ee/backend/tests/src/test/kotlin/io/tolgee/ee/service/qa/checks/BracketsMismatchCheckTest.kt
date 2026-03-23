package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class BracketsMismatchCheckTest {
  private val check = BracketsMismatchCheck()

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
    check.check(params("Hello (world)")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello (world)", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello (world)")).assertNoIssues()
  }

  @Test
  fun `returns empty when brackets match`() {
    check.check(params("Ahoj (svete)", "Hello (world)")).assertNoIssues()
  }

  @Test
  fun `returns empty when no brackets in either`() {
    check.check(params("Ahoj svete", "Hello world")).assertNoIssues()
  }

  @Test
  fun `detects missing bracket in translation`() {
    check.check(params("Ahoj svete", "Hello (world)")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_MISSING)
        noReplacement()
        param("bracket", "(")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_MISSING)
        noReplacement()
        param("bracket", ")")
      }
    }
  }

  @Test
  fun `detects extra bracket in translation`() {
    val results = check.check(params("Ahoj (svete)", "Hello world"))
    results.assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
        position(5, 6)
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
    }
  }

  @Test
  fun `detects different bracket counts`() {
    // Translation has 3 ( and 3 ), source has 1 ( and 1 ) -> 2 extra of each
    check.check(params("Ahoj (svete) (a) (b)", "Hello (world)")).assertIssues {
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", "(")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
      issue {
        message(QaIssueMessage.QA_BRACKETS_EXTRA)
        param("bracket", ")")
      }
    }
  }

  @Test
  fun `handles multiple bracket types`() {
    check.check(params("Ahoj [svete]", "Hello (world) [test]")).assertIssues {
      issue { param("bracket", "(") }
      issue { param("bracket", ")") }
    }
  }

  @Test
  fun `handles curly braces`() {
    check.check(params("Ahoj svete", "Hello {world}")).assertIssues {
      issue { param("bracket", "{") }
      issue { param("bracket", "}") }
    }
  }

  @Test
  fun `all results have BRACKETS_MISMATCH type`() {
    check.check(params("Ahoj svete", "Hello (world)")).assertAllHaveType(QaCheckType.BRACKETS_MISMATCH)
  }
}
