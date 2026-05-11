package io.tolgee.ee.service.qa.checks.whitespace

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class TrimCheckTest {
  private val check = TrimCheck()

  private fun baseParams(text: String) =
    QaCheckParams(
      baseText = null,
      text = text,
      baseLanguageTag = null,
      languageTag = "en",
    )

  private fun nonBaseParams(text: String) =
    QaCheckParams(
      baseText = "base",
      text = text,
      baseLanguageTag = "en",
      languageTag = "de",
    )

  // --- Guards ---

  @Test
  fun `skips non-base translations`() {
    check.check(nonBaseParams("Hello  ")).assertNoIssues()
  }

  @Test
  fun `skips blank text`() {
    check.check(baseParams("   ")).assertNoIssues()
  }

  @Test
  fun `no issues for clean text`() {
    check.check(baseParams("Hello world")).assertNoIssues()
  }

  @Test
  fun `no issues for interior whitespace only`() {
    check.check(baseParams("Hello   world")).assertNoIssues()
  }

  // --- Trailing spaces ---

  @Test
  fun `detects trailing spaces`() {
    check.check(baseParams("Hello   ")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_SPACES)
      position(5, 8)
      replacement("")
    }
  }

  @Test
  fun `detects trailing tab`() {
    check.check(baseParams("Hello\t")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_SPACES)
      position(5, 6)
      replacement("")
    }
  }

  @Test
  fun `detects trailing nbsp`() {
    check.check(baseParams("Hello\u00A0")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_SPACES)
      position(5, 6)
      replacement("")
    }
  }

  // --- Trailing newlines ---

  @Test
  fun `detects trailing LF`() {
    check.check(baseParams("Hello\n")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_NEWLINES)
      position(5, 6)
      replacement("")
    }
  }

  @Test
  fun `detects trailing CRLF`() {
    check.check(baseParams("Hello\r\n")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_NEWLINES)
      position(5, 7)
      replacement("")
    }
  }

  @Test
  fun `detects trailing CR`() {
    check.check(baseParams("Hello\r")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_NEWLINES)
      position(5, 6)
      replacement("")
    }
  }

  @Test
  fun `detects multiple trailing newlines`() {
    check.check(baseParams("Hello\n\n\n")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_NEWLINES)
      position(5, 8)
      replacement("")
    }
  }

  // --- Mixed trailing ---

  @Test
  fun `detects trailing newlines then spaces`() {
    // "Hello\n  " → QA_TRAILING_SPACES at 6..8, QA_TRAILING_NEWLINES at 5..6
    check.check(baseParams("Hello\n  ")).assertIssues {
      issue {
        message(QaIssueMessage.QA_TRAILING_SPACES)
        position(6, 8)
        replacement("")
      }
      issue {
        message(QaIssueMessage.QA_TRAILING_NEWLINES)
        position(5, 6)
        replacement("")
      }
    }
  }

  @Test
  fun `trailing spaces before newline reports only newline`() {
    // "Hello   \n" → only QA_TRAILING_NEWLINES at 8..9
    check.check(baseParams("Hello   \n")).assertSingleIssue {
      message(QaIssueMessage.QA_TRAILING_NEWLINES)
      position(8, 9)
      replacement("")
    }
  }

  // --- Leading spaces ---

  @Test
  fun `detects leading spaces`() {
    check.check(baseParams("  Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_LEADING_SPACES)
      position(0, 2)
      replacement("")
    }
  }

  @Test
  fun `detects leading tab`() {
    check.check(baseParams("\tHello")).assertSingleIssue {
      message(QaIssueMessage.QA_LEADING_SPACES)
      position(0, 1)
      replacement("")
    }
  }

  @Test
  fun `detects leading nbsp`() {
    check.check(baseParams("\u00A0Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_LEADING_SPACES)
      position(0, 1)
      replacement("")
    }
  }

  // --- Leading newlines ---

  @Test
  fun `detects leading LF`() {
    check.check(baseParams("\nHello")).assertSingleIssue {
      message(QaIssueMessage.QA_LEADING_NEWLINES)
      position(0, 1)
      replacement("")
    }
  }

  @Test
  fun `detects leading CRLF`() {
    check.check(baseParams("\r\nHello")).assertSingleIssue {
      message(QaIssueMessage.QA_LEADING_NEWLINES)
      position(0, 2)
      replacement("")
    }
  }

  // --- Mixed leading ---

  @Test
  fun `detects leading newlines then spaces`() {
    // "\n  Hello" → QA_LEADING_NEWLINES at 0..1, QA_LEADING_SPACES at 1..3
    check.check(baseParams("\n  Hello")).assertIssues {
      issue {
        message(QaIssueMessage.QA_LEADING_NEWLINES)
        position(0, 1)
        replacement("")
      }
      issue {
        message(QaIssueMessage.QA_LEADING_SPACES)
        position(1, 3)
        replacement("")
      }
    }
  }

  @Test
  fun `leading spaces before newline reports only spaces`() {
    // "  \nHello" → QA_LEADING_SPACES at 0..2 (newline is after the spaces, not leading)
    check.check(baseParams("  \nHello")).assertSingleIssue {
      message(QaIssueMessage.QA_LEADING_SPACES)
      position(0, 2)
      replacement("")
    }
  }

  // --- Both leading and trailing ---

  @Test
  fun `detects both leading and trailing spaces`() {
    check.check(baseParams("  Hello  ")).assertIssues {
      issue {
        message(QaIssueMessage.QA_LEADING_SPACES)
        position(0, 2)
      }
      issue {
        message(QaIssueMessage.QA_TRAILING_SPACES)
        position(7, 9)
      }
    }
  }

  // --- Plurals ---

  @Test
  fun `detects issues in plural variants`() {
    val params =
      QaCheckParams(
        baseText = null,
        text = "{count, plural, one {Hello } other {World  }}",
        baseLanguageTag = null,
        languageTag = "en",
        isPlural = true,
        textVariants = mapOf("one" to "Hello ", "other" to "World  "),
        textVariantOffsets = mapOf("one" to 20, "other" to 35),
        baseTextVariants = null,
      )
    check.check(params).assertIssues {
      issue {
        message(QaIssueMessage.QA_TRAILING_SPACES)
        position(25, 26)
        pluralVariant("one")
      }
      issue {
        message(QaIssueMessage.QA_TRAILING_SPACES)
        position(40, 42)
        pluralVariant("other")
      }
    }
  }

  // --- Type assertion ---

  @Test
  fun `all results have type TRIM_CHECK`() {
    check.check(baseParams("\n  Hello  \n")).assertAllHaveType(QaCheckType.TRIM_CHECK)
  }
}
