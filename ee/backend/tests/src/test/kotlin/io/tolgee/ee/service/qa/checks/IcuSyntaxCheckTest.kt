package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class IcuSyntaxCheckTest {
  private val check = IcuSyntaxCheck()

  private fun params(text: String) =
    QaCheckParams(
      baseText = null,
      text = text,
      baseLanguageTag = null,
      languageTag = "en",
    )

  @Test
  fun `returns empty for valid simple text`() {
    check.check(params("Hello world")).assertNoIssues()
  }

  @Test
  fun `returns empty for valid text with placeholder`() {
    check.check(params("Hello {name}")).assertNoIssues()
  }

  @Test
  fun `returns empty for valid typed placeholder`() {
    check.check(params("You have {count, number} messages")).assertNoIssues()
  }

  @Test
  fun `returns empty for valid plural`() {
    check.check(params("{count, plural, one {# item} other {# items}}")).assertNoIssues()
  }

  @Test
  fun `returns empty for valid select`() {
    check.check(params("{gender, select, male {He} female {She} other {They}}")).assertNoIssues()
  }

  @Test
  fun `returns empty for empty text`() {
    check.check(params("")).assertNoIssues()
  }

  @Test
  fun `returns empty for escaped braces`() {
    check.check(params("This is '{'not a placeholder'}'")).assertNoIssues()
  }

  @Test
  fun `returns empty for plain text without ICU`() {
    check.check(params("Just a regular sentence.")).assertNoIssues()
  }

  @Test
  fun `detects unmatched opening brace`() {
    check.check(params("Hello {world")).assertSingleIssue {
      message(QaIssueMessage.QA_ICU_SYNTAX_ERROR)
      type(QaCheckType.ICU_SYNTAX)
    }
  }

  @Test
  fun `treats unmatched closing brace as valid`() {
    check.check(params("Hello world}")).assertNoIssues()
  }

  @Test
  fun `detects select missing other keyword`() {
    check.check(params("{gender, select, male {He} female {She}}")).assertSingleIssue {
      message(QaIssueMessage.QA_ICU_SYNTAX_ERROR)
    }
  }

  @Test
  fun `detects invalid plural syntax`() {
    check.check(params("{count, plural, }")).assertSingleIssue {
      message(QaIssueMessage.QA_ICU_SYNTAX_ERROR)
    }
  }

  @Test
  fun `returns correct position span`() {
    val text = "Hello {world"
    check.check(params(text)).assertSingleIssue {
      position(0, text.length)
    }
  }

  @Test
  fun `returns null replacement`() {
    check.check(params("Hello {world")).assertSingleIssue {
      noReplacement()
    }
  }
}
