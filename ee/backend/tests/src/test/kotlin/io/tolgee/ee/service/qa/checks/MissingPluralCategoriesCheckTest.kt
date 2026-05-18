package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class MissingPluralCategoriesCheckTest {
  private val check = MissingPluralCategoriesCheck()

  @Test
  fun `no issues when all required plural variants are filled`() {
    // English requires: one, other
    check
      .check(
        qaCheckParams(
          text = "{count, plural, one {item} other {items}}",
          isPlural = true,
          textVariants = mapOf("one" to "item", "other" to "items"),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `detects blank required plural variant`() {
    check
      .check(
        qaCheckParams(
          text = "{count, plural, one {} other {items}}",
          isPlural = true,
          textVariants = mapOf("one" to "", "other" to "items"),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
        param("variant", "one")
        pluralVariant("one")
        noPosition()
      }
  }

  @Test
  fun `detects missing required plural variant`() {
    // Czech requires: one, few, many, other
    check
      .check(
        qaCheckParams(
          text = "{count, plural, other {polozek}}",
          languageTag = "cs",
          isPlural = true,
          textVariants = mapOf("other" to "polozek"),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
          param("variant", "one")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
          param("variant", "few")
          pluralVariant("few")
        }
        issue {
          message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
          param("variant", "many")
          pluralVariant("many")
        }
      }
  }

  @Test
  fun `no issue for blank non-required variant`() {
    // English requires: one, other — "zero" / "=0" is not required
    check
      .check(
        qaCheckParams(
          text = "{count, plural, =0 {} one {item} other {items}}",
          isPlural = true,
          textVariants = mapOf("=0" to "", "one" to "item", "other" to "items"),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `whitespace-only variant text is detected as blank`() {
    check
      .check(
        qaCheckParams(
          text = "{count, plural, one {   } other {items}}",
          isPlural = true,
          textVariants = mapOf("one" to "   ", "other" to "items"),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
        param("variant", "one")
        pluralVariant("one")
      }
  }

  @Test
  fun `fully empty plural text reports all required forms as missing`() {
    check
      .check(
        qaCheckParams(
          text = "",
          isPlural = true,
          textVariants = null,
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
          param("variant", "one")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
          param("variant", "other")
          pluralVariant("other")
        }
      }
  }

  @Test
  fun `all required plural variants blank reports each one`() {
    // Parseable plural with every required variant blank — the realistic shape
    // for "user created a plural key but hasn't filled anything in".
    check
      .check(
        qaCheckParams(
          text = "{count, plural, one {} other {}}",
          isPlural = true,
          textVariants = mapOf("one" to "", "other" to ""),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
          param("variant", "one")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_MISSING_PLURAL_CATEGORY)
          param("variant", "other")
          pluralVariant("other")
        }
      }
  }

  @Test
  fun `non-plural key with textVariants returns no issues`() {
    check
      .check(
        qaCheckParams(
          text = "Hello world",
          isPlural = false,
          textVariants = mapOf("one" to "", "other" to ""),
        ),
      ).assertNoIssues()
  }
}
