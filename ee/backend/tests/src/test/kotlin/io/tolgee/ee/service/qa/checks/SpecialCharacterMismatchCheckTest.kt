package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class SpecialCharacterMismatchCheckTest {
  private val check = SpecialCharacterMismatchCheck()

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
    check.check(params("Price: \$10")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Price: \$10", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Price: \$10")).assertNoIssues()
  }

  @Test
  fun `returns empty when no special chars in either`() {
    check.check(params("Bonjour monde", "Hello world")).assertNoIssues()
  }

  @Test
  fun `returns empty when special chars match exactly`() {
    check.check(params("Cena: \$10 @domov", "Price: \$10 @home")).assertNoIssues()
  }

  @Test
  fun `detects missing special character`() {
    check.check(params("Cena: 10", "Price: \$10")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
      param("character", "\$")
      noReplacement()
      noPosition()
    }
  }

  @Test
  fun `detects multiple missing special characters`() {
    check.check(params("10 domov 2024", "\$10 @home \u00A92024")).assertIssues {
      issue {
        message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
        param("character", "\$")
      }
      issue {
        message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
        param("character", "@")
      }
      issue {
        message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
        param("character", "\u00A9")
      }
    }
  }

  @Test
  fun `detects missing when count differs`() {
    check.check(params("\$10 sleva", "\$10 - \$5 sleva")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
      param("character", "\$")
    }
  }

  @Test
  fun `detects added special character`() {
    check.check(params("Bonjour \u00A9monde", "Hello world")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_ADDED)
      param("character", "\u00A9")
      replacement("")
    }
  }

  @Test
  fun `detects added character with correct position`() {
    check.check(params("Hel\$lo", "Hello")).assertSingleIssue {
      position(3, 4)
      replacement("")
    }
  }

  @Test
  fun `detects both missing and added`() {
    check.check(params("\u20AC10 prix", "\$10 price")).assertIssues {
      issue {
        message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
        param("character", "\$")
      }
      issue {
        message(QaIssueMessage.QA_SPECIAL_CHAR_ADDED)
        param("character", "\u20AC")
      }
    }
  }

  @Test
  fun `all types are SPECIAL_CHARACTER_MISMATCH`() {
    check.check(params("\u20AC10 prix", "\$10 price")).assertAllHaveType(QaCheckType.SPECIAL_CHARACTER_MISMATCH)
  }

  @Test
  fun `detects all supported special characters`() {
    for (char in SpecialCharacterMismatchCheck.SPECIAL_CHARS) {
      check.check(params("text", "text$char")).assertSingleIssue {
        param("character", char.toString())
      }
    }
  }

  @Test
  fun `does not flag non-special characters`() {
    check.check(params("Hello world", "Hello world!?.:;")).assertNoIssues()
  }
}
