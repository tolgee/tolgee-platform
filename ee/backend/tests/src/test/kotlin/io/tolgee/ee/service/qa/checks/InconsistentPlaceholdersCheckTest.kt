package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class InconsistentPlaceholdersCheckTest {
  private val check = InconsistentPlaceholdersCheck()

  private fun params(
    text: String,
    base: String? = null,
    isPlural: Boolean = false,
    textVariants: Map<String, String>? = null,
    baseTextVariants: Map<String, String>? = null,
    textVariantOffsets: Map<String, Int>? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
    isPlural = isPlural,
    textVariants = textVariants,
    baseTextVariants = baseTextVariants,
    textVariantOffsets = textVariantOffsets,
  )

  @Test
  fun `returns empty when base is null`() {
    check.check(params("Hello {name}")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Hello {name}", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Hello {name}")).assertNoIssues()
  }

  @Test
  fun `returns empty when all placeholders match`() {
    check.check(params("Ahoj {name}", "Hello {name}")).assertNoIssues()
  }

  @Test
  fun `returns empty when no placeholders in either`() {
    check.check(params("Ahoj", "Hello")).assertNoIssues()
  }

  @Test
  fun `detects missing placeholder`() {
    check.check(params("Ahoj", "Hello {name}")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
      param("placeholder", "name")
      noReplacement()
    }
  }

  @Test
  fun `detects extra placeholder with position and replacement`() {
    check.check(params("Ahoj {extra}", "Hello")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
      param("placeholder", "extra")
      position(5, 12)
      replacement("")
    }
  }

  @Test
  fun `detects both missing and extra`() {
    check.check(params("Ahoj {nom}", "Hello {name}")).assertIssues {
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
        param("placeholder", "name")
        noPosition()
        noReplacement()
      }
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
        param("placeholder", "nom")
        position(5, 10)
        replacement("")
      }
    }
  }

  @Test
  fun `reports position for each occurrence of duplicate extra placeholder`() {
    check.check(params("Ahoj {foo} a {foo}", "Hello")).assertIssues {
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
        replacement("")
        position(5, 10)
      }
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
        replacement("")
        position(13, 18)
      }
    }
  }

  @Test
  fun `detects missing duplicate placeholder`() {
    check.check(params("Ahoj {name}", "Hello {name} and {name}")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
      param("placeholder", "name")
      noReplacement()
    }
  }

  @Test
  fun `detects extra duplicate placeholder`() {
    check.check(params("Ahoj {name} a {name}", "Hello {name}")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
      param("placeholder", "name")
      position(14, 20)
      replacement("")
    }
  }

  @Test
  fun `handles numbered placeholders`() {
    check.check(params("Ahoj {1}", "Hello {0} and {1}")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
      param("placeholder", "0")
    }
  }

  @Test
  fun `handles typed placeholders`() {
    check.check(params("Ahoj", "Hello {count, number}")).assertSingleIssue {
      param("placeholder", "count")
    }
  }

  @Test
  fun `handles styled placeholders`() {
    check.check(params("Cena", "Price {price, number, currency}")).assertSingleIssue {
      param("placeholder", "price")
    }
  }

  @Test
  fun `handles multiple matching placeholders`() {
    check
      .check(
        params(
          "Ahoj {name}, mate {count, number} zprav",
          "Hello {name}, you have {count, number} messages",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `handles nested select args`() {
    check
      .check(
        params(
          "{gender, select, male {On} female {Ona} other {Ono}}",
          "{gender, select, male {He has {count}} female {She has {count}} other {They have {count}}}",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
        param("placeholder", "count")
      }
  }

  @Test
  fun `returns empty when parse fails on base`() {
    check.check(params("Hello {name}", "Hello {unclosed")).assertNoIssues()
  }

  @Test
  fun `returns empty when parse fails on text`() {
    check.check(params("Hello {unclosed", "Hello {name}")).assertNoIssues()
  }

  @Test
  fun `handles escaped braces`() {
    check.check(params("Ahoj '{'not a placeholder'}'", "Hello '{'not a placeholder'}'")).assertNoIssues()
  }

  @Test
  fun `ignores hash in standalone text`() {
    check.check(params("# items by {author}", "# items by {author}")).assertNoIssues()
  }

  @Test
  fun `all types are INCONSISTENT_PLACEHOLDERS`() {
    check.check(params("Ahoj {nom}", "Hello {name}")).assertAllHaveType(QaCheckType.INCONSISTENT_PLACEHOLDERS)
  }

  @Test
  fun `works with plural variants`() {
    check
      .check(
        params(
          text = "{count, plural, one {# polozka} other {# polozek}}",
          base = "{count, plural, one {# item by {author}} other {# items by {author}}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka", "other" to "# polozek"),
          baseTextVariants = mapOf("one" to "# item by {author}", "other" to "# items by {author}"),
          textVariantOffsets = mapOf("one" to 24, "other" to 39),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
          param("placeholder", "author")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
          param("placeholder", "author")
          pluralVariant("other")
        }
      }
  }

  @Test
  fun `detects missing placeholder in one plural variant only`() {
    check
      .check(
        params(
          text = "{count, plural, one {# polozka od {author}} other {# polozek}}",
          base = "{count, plural, one {# item by {author}} other {# items by {author}}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka od {author}", "other" to "# polozek"),
          baseTextVariants = mapOf("one" to "# item by {author}", "other" to "# items by {author}"),
          textVariantOffsets = mapOf("one" to 24, "other" to 51),
        ),
      ).assertSingleIssue {
        pluralVariant("other")
        param("placeholder", "author")
      }
  }
}
