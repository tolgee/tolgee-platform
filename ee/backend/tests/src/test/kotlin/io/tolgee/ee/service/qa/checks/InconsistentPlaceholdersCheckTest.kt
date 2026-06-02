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
  fun `collapses one missing and one extra into a single replace issue`() {
    check.check(params("Ahoj {nom}", "Hello {name}")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_REPLACE)
      param("placeholder", "nom")
      param("expected", "name")
      position(5, 10)
      replacement("{name}")
    }
  }

  @Test
  fun `replace issue preserves the full source of a typed placeholder`() {
    check.check(params("Ahoj {pocet, number}", "Hello {count, number}")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_REPLACE)
      param("placeholder", "pocet")
      param("expected", "count")
      replacement("{count, number}")
    }
  }

  @Test
  fun `pairs into replace when only one of several placeholders is translated`() {
    check.check(params("Ahoj {x} a {b}", "Hello {a} and {b}")).assertSingleIssue {
      message(QaIssueMessage.QA_PLACEHOLDERS_REPLACE)
      param("placeholder", "x")
      param("expected", "a")
      replacement("{a}")
    }
  }

  @Test
  fun `falls back to separate issues when multiple placeholders are translated`() {
    check.check(params("Ahoj {x} a {y}", "Hello {a} and {b}")).assertIssues {
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
        param("placeholder", "a")
        noPosition()
        noReplacement()
      }
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
        param("placeholder", "b")
        noPosition()
        noReplacement()
      }
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
        param("placeholder", "x")
        replacement("")
      }
      issue {
        message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
        param("placeholder", "y")
        replacement("")
      }
    }
  }

  @Test
  fun `pairs into replace within a single plural variant only`() {
    check
      .check(
        params(
          text = "{count, plural, one {# polozka od {autor}} other {# polozek od {author}}}",
          base = "{count, plural, one {# item by {author}} other {# items by {author}}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka od {autor}", "other" to "# polozek od {author}"),
          baseTextVariants = mapOf("one" to "# item by {author}", "other" to "# items by {author}"),
          textVariantOffsets = mapOf("one" to 21, "other" to 49),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_PLACEHOLDERS_REPLACE)
        param("placeholder", "autor")
        param("expected", "author")
        replacement("{author}")
        pluralVariant("one")
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
  fun `detects missing hash placeholder in plural variants`() {
    // Base has # in both variants, translation has none in either.
    check
      .check(
        params(
          text = "{count, plural, one {polozka} other {polozek}}",
          base = "{count, plural, one {# polozka} other {# polozek}}",
          isPlural = true,
          textVariants = mapOf("one" to "polozka", "other" to "polozek"),
          baseTextVariants = mapOf("one" to "# polozka", "other" to "# polozek"),
          textVariantOffsets = mapOf("one" to 21, "other" to 37),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
          param("placeholder", "#")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
          param("placeholder", "#")
          pluralVariant("other")
        }
      }
  }

  @Test
  fun `detects extra hash placeholder in plural variants`() {
    check
      .check(
        params(
          text = "{count, plural, one {# polozka} other {# polozek}}",
          base = "{count, plural, one {polozka} other {polozek}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka", "other" to "# polozek"),
          baseTextVariants = mapOf("one" to "polozka", "other" to "polozek"),
          textVariantOffsets = mapOf("one" to 21, "other" to 39),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
          param("placeholder", "#")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
          param("placeholder", "#")
          pluralVariant("other")
        }
      }
  }

  @Test
  fun `does not flag hash placeholder when both base and translation have it`() {
    check
      .check(
        params(
          text = "{count, plural, one {# polozka} other {# polozek}}",
          base = "{count, plural, one {# item} other {# items}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka", "other" to "# polozek"),
          baseTextVariants = mapOf("one" to "# item", "other" to "# items"),
          textVariantOffsets = mapOf("one" to 21, "other" to 39),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `counts hash occurrences within a single plural variant`() {
    // Base has # twice in 'one' (e.g. "# orders, # total"), translation has it once —
    // one # is missing. Top-level # gets a position just like a regular {arg}, so each
    // occurrence is tracked individually rather than collapsed by name.
    check
      .check(
        params(
          text = "{count, plural, one {# polozek celkem} other {# polozek}}",
          base = "{count, plural, one {# polozek, # celkem} other {# polozek}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozek celkem", "other" to "# polozek"),
          baseTextVariants = mapOf("one" to "# polozek, # celkem", "other" to "# polozek"),
          textVariantOffsets = mapOf("one" to 21, "other" to 46),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
        param("placeholder", "#")
        pluralVariant("one")
      }
  }

  @Test
  fun `reports position of extra hash placeholder`() {
    // Base has no # — translation added one in 'one'. The extra-issue path should
    // include the # position (offset back into the full ICU text) and a "" replacement
    // so the editor can offer to delete it.
    check
      .check(
        params(
          text = "{count, plural, one {# polozka} other {polozek}}",
          base = "{count, plural, one {polozka} other {polozek}}",
          isPlural = true,
          textVariants = mapOf("one" to "# polozka", "other" to "polozek"),
          baseTextVariants = mapOf("one" to "polozka", "other" to "polozek"),
          textVariantOffsets = mapOf("one" to 21, "other" to 39),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_PLACEHOLDERS_EXTRA)
        param("placeholder", "#")
        pluralVariant("one")
        position(21, 22)
        replacement("")
      }
  }

  @Test
  fun `detects missing hash placeholder in non-plural ICU text`() {
    // Inline ICU plural inside a non-plural string. `#` inside should still be tracked.
    check
      .check(
        params(
          text = "You have {count, plural, one {an item} other {many items}}",
          base = "You have {count, plural, one {# item} other {# items}}",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
        param("placeholder", "#")
      }
  }

  @Test
  fun `escaped hash in plural variant is not a placeholder`() {
    // Asymmetric escaping: base uses real `#` (a placeholder), translation uses `'#'`
    // (literal hash, NOT a placeholder). Should report missing `#`.
    check
      .check(
        params(
          text = "{count, plural, one {'#' polozka} other {'#' polozek}}",
          base = "{count, plural, one {# item} other {# items}}",
          isPlural = true,
          textVariants = mapOf("one" to "'#' polozka", "other" to "'#' polozek"),
          baseTextVariants = mapOf("one" to "# item", "other" to "# items"),
          textVariantOffsets = mapOf("one" to 21, "other" to 41),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
          param("placeholder", "#")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_PLACEHOLDERS_MISSING)
          param("placeholder", "#")
          pluralVariant("other")
        }
      }
  }

  @Test
  fun `symmetric escaped hash in plural variant produces no issue`() {
    // Both sides use `'#'` (escaped literal hash, not a placeholder) — no mismatch.
    check
      .check(
        params(
          text = "{count, plural, one {'#' polozka} other {'#' polozek}}",
          base = "{count, plural, one {'#' item} other {'#' items}}",
          isPlural = true,
          textVariants = mapOf("one" to "'#' polozka", "other" to "'#' polozek"),
          baseTextVariants = mapOf("one" to "'#' item", "other" to "'#' items"),
          textVariantOffsets = mapOf("one" to 21, "other" to 41),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `hash inside select nested in plural is treated as literal`() {
    // Per ICU MessageFormat, `#` only refers to the plural argument when it appears
    // directly inside a plural variant body. Once nested inside another complex arg
    // (here a select), `#` is just literal text — so it should NOT be tracked as a
    // placeholder, and a missing `#` inside the nested select is not flagged.
    check
      .check(
        params(
          text =
            "{count, plural, one {{g, select, m {polozka} other {polozka}}} " +
              "other {{g, select, m {polozek} other {polozek}}}}",
          base =
            "{count, plural, one {{g, select, m {# item} other {# item}}} " +
              "other {{g, select, m {# items} other {# items}}}}",
          isPlural = true,
          textVariants =
            mapOf(
              "one" to "{g, select, m {polozka} other {polozka}}",
              "other" to "{g, select, m {polozek} other {polozek}}",
            ),
          baseTextVariants =
            mapOf(
              "one" to "{g, select, m {# item} other {# item}}",
              "other" to "{g, select, m {# items} other {# items}}",
            ),
          textVariantOffsets = mapOf("one" to 21, "other" to 71),
        ),
      ).assertNoIssues()
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
