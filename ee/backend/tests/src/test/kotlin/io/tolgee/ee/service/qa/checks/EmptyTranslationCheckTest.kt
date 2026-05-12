package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class EmptyTranslationCheckTest {
  private val check = EmptyTranslationCheck()

  @Test
  fun `detects empty non-plural translation`() {
    check.check(qaCheckParams("")).assertSingleIssue {
      message(QaIssueMessage.QA_EMPTY_TRANSLATION)
      noPosition()
    }
  }

  @Test
  fun `detects blank non-plural translation`() {
    check.check(qaCheckParams("   ")).assertSingleIssue {
      message(QaIssueMessage.QA_EMPTY_TRANSLATION)
      noPosition()
    }
  }

  @Test
  fun `no issues for non-empty non-plural translation`() {
    check.check(qaCheckParams("Hello world")).assertNoIssues()
  }

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
  fun `no issue when at least one plural variant is filled`() {
    check
      .check(
        qaCheckParams(
          text = "{count, plural, one {} other {items}}",
          isPlural = true,
          textVariants = mapOf("one" to "", "other" to "items"),
        ),
      ).assertNoIssues()
  }

  @Test
  fun `fires when all plural variants are blank`() {
    check
      .check(
        qaCheckParams(
          text = "{count, plural, one {} other {}}",
          isPlural = true,
          textVariants = mapOf("one" to "", "other" to ""),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_EMPTY_TRANSLATION)
        noPosition()
      }
  }

  @Test
  fun `fires when textVariants is empty for plural`() {
    check
      .check(
        qaCheckParams(
          text = "{count, plural, }",
          isPlural = true,
          textVariants = emptyMap(),
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_EMPTY_TRANSLATION)
        noPosition()
      }
  }

  @Test
  fun `fully empty plural text fires generic empty translation`() {
    // text is blank, textVariants is null (getPluralForms("") returns null)
    check
      .check(
        qaCheckParams(
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
  fun `non-blank plural text with null textVariants returns no issues`() {
    // isPlural is true but textVariants is null (e.g., unparseable ICU string)
    check
      .check(
        qaCheckParams(
          text = "some non-blank text",
          isPlural = true,
          textVariants = null,
        ),
      ).assertNoIssues()
  }

  @Test
  fun `non-plural key with textVariants ignores variants`() {
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
