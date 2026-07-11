package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class KeyLengthLimitCheckTest {
  private val check = KeyLengthLimitCheck()

  private fun params(
    text: String,
    maxCharLimit: Int? = null,
    isPlural: Boolean = false,
  ) = QaCheckParams(
    baseText = null,
    text = text,
    baseLanguageTag = null,
    languageTag = "en",
    maxCharLimit = maxCharLimit,
    isPlural = isPlural,
  )

  @Test
  fun `returns empty when maxCharLimit is null`() {
    check.check(params("Hello world", maxCharLimit = null)).assertNoIssues()
  }

  @Test
  fun `returns empty when maxCharLimit is zero`() {
    check.check(params("Hello world", maxCharLimit = 0)).assertNoIssues()
  }

  @Test
  fun `returns empty when maxCharLimit is negative`() {
    check.check(params("Hello world", maxCharLimit = -1)).assertNoIssues()
  }

  @Test
  fun `returns empty when text is within limit`() {
    check.check(params("Hello", maxCharLimit = 10)).assertNoIssues()
  }

  @Test
  fun `returns empty when text is at exact limit`() {
    check.check(params("Hello", maxCharLimit = 5)).assertNoIssues()
  }

  @Test
  fun `detects text exceeding limit`() {
    check.check(params("Hello world", maxCharLimit = 5)).assertSingleIssue {
      message(QaIssueMessage.QA_KEY_LENGTH_LIMIT_EXCEEDED)
      noReplacement()
      noPosition()
      param("limit", "5")
      param("count", "11")
    }
  }

  @Test
  fun `returns empty for empty text`() {
    check.check(params("", maxCharLimit = 5)).assertNoIssues()
  }

  @Test
  fun `excludes ICU placeholders from count`() {
    // "{name}" is a placeholder, only visible text is "Hello " (6 chars)
    check.check(params("Hello {name}", maxCharLimit = 10)).assertNoIssues()
  }

  @Test
  fun `detects limit exceeded even with placeholders`() {
    // "Hello world " is 12 visible chars, "{name}" is a placeholder
    check.check(params("Hello world {name}", maxCharLimit = 5)).assertSingleIssue {
      message(QaIssueMessage.QA_KEY_LENGTH_LIMIT_EXCEEDED)
      param("limit", "5")
      param("count", "12")
    }
  }

  @Test
  fun `handles plural text - max variant exceeds limit`() {
    val pluralText = "{count, plural, one {# item in cart} other {# items in cart}}"
    // "# items in cart" = 15 visible chars (longest variant)
    check.check(params(pluralText, maxCharLimit = 10, isPlural = true)).assertSingleIssue {
      message(QaIssueMessage.QA_KEY_LENGTH_LIMIT_EXCEEDED)
      param("limit", "10")
    }
  }

  @Test
  fun `handles plural text - all variants within limit`() {
    val pluralText = "{count, plural, one {# item} other {# items}}"
    // "# items" = 7 visible chars (longest variant)
    check.check(params(pluralText, maxCharLimit = 10, isPlural = true)).assertNoIssues()
  }

  @Test
  fun `all results have type KEY_LENGTH_LIMIT`() {
    check.check(params("Hello world", maxCharLimit = 5)).assertAllHaveType(QaCheckType.KEY_LENGTH_LIMIT)
  }
}
