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
    icuPlaceholders: Boolean = true,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
    icuPlaceholders = icuPlaceholders,
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
      param("characters", "\$")
      param("count", "1")
      noReplacement()
      noPosition()
    }
  }

  @Test
  fun `detects multiple missing special characters`() {
    // Multiple missing characters are aggregated into a single issue
    check.check(params("10 domov 2024", "\$10 @home \u00A92024")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
      param("count", "3")
      noReplacement()
      noPosition()
    }
  }

  @Test
  fun `detects missing when count differs`() {
    check.check(params("\$10 sleva", "\$10 - \$5 sleva")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
      param("characters", "\$")
      param("count", "1")
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
        param("characters", "\$")
        param("count", "1")
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
  fun `detects copyright trademark and degree characters`() {
    // A spread of less common special chars, to exercise the full check (not just $).
    check.check(params("text", "Copyright © 2024, registered ®, 100°F, brand ™")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
      param("count", "4")
    }
  }

  @Test
  fun `flags missing hash when ICU is disabled`() {
    // Issue tag dropped from translation.
    check.check(params("Cislo objednavky 123", "Order #123", icuPlaceholders = false)).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
      param("characters", "#")
      param("count", "1")
    }
  }

  @Test
  fun `skips missing hash when ICU is enabled`() {
    // With ICU enabled, # is owned by the Inconsistent placeholders check to avoid
    // double-reporting. Even a literal "#123" outside any plural is intentionally
    // skipped — the simplification we accept in exchange for a single owner of #.
    check.check(params("Cislo objednavky 123", "Order #123", icuPlaceholders = true)).assertNoIssues()
  }

  @Test
  fun `skips extra hash when ICU is enabled`() {
    check.check(params("Order #1 confirmed", "Order 1 confirmed", icuPlaceholders = true)).assertNoIssues()
  }

  @Test
  fun `with ICU enabled still flags other chars when hash also differs`() {
    // Translation dropped both `#` (issue tag) and `@` (handle). With ICU we ignore the
    // missing `#` but the `@` should still be reported.
    check
      .check(
        params(
          text = "Objednavka 5 od support",
          base = "Order #5 from @support",
          icuPlaceholders = true,
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
        param("characters", "@")
        param("count", "1")
      }
  }

  @Test
  fun `does not flag non-special characters`() {
    check.check(params("Hello world", "Hello world!?.:;")).assertNoIssues()
  }

  @Test
  fun `does not flag extra special char inside a URL`() {
    // The `&` lives inside the URL query string, not in the translated content.
    check.check(params("Visit https://x.com?a=1&b=2", "Visit page")).assertNoIssues()
  }

  @Test
  fun `does not flag extra special char inside an HTML tag`() {
    check.check(params("""Click <a href="mailto:x@y.com">here</a>""", "Click here")).assertNoIssues()
  }

  @Test
  fun `flags genuine extra special char outside blocked ranges`() {
    check.check(params("Price \$5 https://x.com", "Price 5")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_ADDED)
      param("character", "\$")
    }
  }

  @Test
  fun `still reports missing special char when a URL is present`() {
    // The missing-char issue carries no position, so range filtering never drops it.
    check.check(params("Cena: 10 https://x.com", "Price: \$10 https://x.com")).assertSingleIssue {
      message(QaIssueMessage.QA_SPECIAL_CHAR_MISSING)
      param("characters", "\$")
      param("count", "1")
    }
  }
}
