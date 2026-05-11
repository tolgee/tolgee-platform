package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class EmptyTranslationCheckTest {
  private val check = EmptyTranslationCheck()

  private fun params(
    text: String,
    languageTag: String = "en",
    isPlural: Boolean = false,
    textVariants: Map<String, String>? = null,
    activeVariant: String? = null,
  ) = QaCheckParams(
    baseText = null,
    text = text,
    baseLanguageTag = null,
    languageTag = languageTag,
    isPlural = isPlural,
    textVariants = textVariants,
    activeVariant = activeVariant,
  )

  @Test
  fun `detects empty non-plural translation`() {
    check.check(params("")).assertSingleIssue {
      message(QaIssueMessage.QA_EMPTY_TRANSLATION)
      noPosition()
    }
  }

  @Test
  fun `detects blank non-plural translation`() {
    check.check(params("   ")).assertSingleIssue {
      message(QaIssueMessage.QA_EMPTY_TRANSLATION)
      noPosition()
    }
  }

  @Test
  fun `no issues for non-empty non-plural translation`() {
    check.check(params("Hello world")).assertNoIssues()
  }

  @Test
  fun `no issues when all required plural variants are filled`() {
    // English requires: one, other
    check
      .check(
        params(
          text = "{count, plural, one {item} other {items}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("one" to "item", "other" to "items"),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `detects blank required plural variant`() {
    // English requires: one, other
    check
      .check(
        params(
          text = "{count, plural, one {} other {items}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("one" to "", "other" to "items"),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
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
        params(
          text = "{count, plural, other {polozek}}",
          languageTag = "cs",
          isPlural = true,
          textVariants = mapOf("other" to "polozek"),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
          param("variant", "one")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
          param("variant", "few")
          pluralVariant("few")
        }
        issue {
          message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
          param("variant", "many")
          pluralVariant("many")
        }
      }
  }

  @Test
  fun `no issue for blank non-required variant`() {
    // English requires: one, other — "zero" is not required
    check
      .check(
        params(
          text = "{count, plural, =0 {} one {item} other {items}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("=0" to "", "one" to "item", "other" to "items"),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `activeVariant filters to only that variant - blank`() {
    // English requires: one, other — both blank, but only checking "one"
    check
      .check(
        params(
          text = "{count, plural, one {} other {}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("one" to "", "other" to ""),
          activeVariant = "one",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
        param("variant", "one")
        pluralVariant("one")
      }
  }

  @Test
  fun `activeVariant filters to only that variant - filled`() {
    // "one" is filled, "other" is blank — but we're only checking "one"
    check
      .check(
        params(
          text = "{count, plural, one {item} other {}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("one" to "item", "other" to ""),
          activeVariant = "one",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `activeVariant set to variant missing from textVariants`() {
    // "one" is required for English but missing from textVariants
    check
      .check(
        params(
          text = "{count, plural, other {items}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("other" to "items"),
          activeVariant = "one",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
        param("variant", "one")
        pluralVariant("one")
      }
  }

  @Test
  fun `fully empty plural text fires generic empty translation only`() {
    // text is blank, textVariants is null (getPluralForms("") returns null)
    check
      .check(
        params(
          text = "",
          languageTag = "cs",
          isPlural = true,
          textVariants = null,
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_EMPTY_TRANSLATION)
        noPosition()
      }
  }

  @Test
  fun `empty textVariants map reports all required forms as missing`() {
    // English requires: one, other
    check
      .check(
        params(
          text = "{count, plural, }",
          languageTag = "en",
          isPlural = true,
          textVariants = emptyMap(),
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
          param("variant", "one")
          pluralVariant("one")
        }
        issue {
          message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
          param("variant", "other")
          pluralVariant("other")
        }
      }
  }

  @Test
  fun `non-blank plural text with null textVariants returns no issues`() {
    // isPlural is true but textVariants is null (e.g., unparseable ICU string)
    check
      .check(
        params(
          text = "some non-blank text",
          languageTag = "en",
          isPlural = true,
          textVariants = null,
        ),
      ).assertNoIssues()
  }

  @Test
  fun `activeVariant set to non-required form returns no issues`() {
    // "zero" is not required for English — editing it should not flag anything
    check
      .check(
        params(
          text = "{count, plural, =0 {} one {item} other {items}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("=0" to "", "one" to "item", "other" to "items"),
          activeVariant = "zero",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `whitespace-only variant text is detected as blank`() {
    check
      .check(
        params(
          text = "{count, plural, one {   } other {items}}",
          languageTag = "en",
          isPlural = true,
          textVariants = mapOf("one" to "   ", "other" to "items"),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_EMPTY_PLURAL_VARIANT)
        param("variant", "one")
        pluralVariant("one")
      }
  }

  @Test
  fun `non-plural key with textVariants ignores variants`() {
    // isPlural is false — textVariants should be irrelevant
    check
      .check(
        params(
          text = "Hello world",
          languageTag = "en",
          isPlural = false,
          textVariants = mapOf("one" to "", "other" to ""),
        ),
      ).assertNoIssues()
  }
}
