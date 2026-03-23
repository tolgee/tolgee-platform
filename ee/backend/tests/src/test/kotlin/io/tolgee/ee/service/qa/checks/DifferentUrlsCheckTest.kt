package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertIssues
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.assertSingleIssue
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.junit.jupiter.api.Test

class DifferentUrlsCheckTest {
  private val check = DifferentUrlsCheck()

  private fun params(
    text: String,
    base: String? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "de",
  )

  @Test
  fun `returns empty when base is null`() {
    check.check(params("Visit https://example.com")).assertNoIssues()
  }

  @Test
  fun `returns empty when base is blank`() {
    check.check(params("Visit https://example.com", "  ")).assertNoIssues()
  }

  @Test
  fun `returns empty when text is blank`() {
    check.check(params("  ", "Visit https://example.com")).assertNoIssues()
  }

  @Test
  fun `returns empty when no URLs in either text`() {
    check.check(params("Hallo Welt", "Hello world")).assertNoIssues()
  }

  @Test
  fun `returns empty when URLs match`() {
    check
      .check(
        params(
          "Besuchen Sie https://example.com fur mehr",
          "Visit https://example.com for more",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `returns empty when multiple URLs match`() {
    check
      .check(
        params(
          "Links: https://a.com und https://b.com",
          "Links: https://a.com and https://b.com",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `detects missing URL`() {
    check
      .check(
        params(
          "Besuchen Sie die Webseite",
          "Visit https://example.com for more",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_URL_MISSING)
        param("url", "https://example.com")
        noReplacement()
        noPosition()
      }
  }

  @Test
  fun `detects extra URL`() {
    check
      .check(
        params(
          "Besuchen Sie https://extra.com bitte",
          "Visit the website",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_URL_EXTRA)
        param("url", "https://extra.com")
        replacement("")
        position(13, 30)
      }
  }

  @Test
  fun `detects replaced URL`() {
    check
      .check(
        params(
          "Besuchen Sie https://wrong.com fur mehr",
          "Visit https://example.com for more",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_URL_REPLACE)
        param("url", "https://wrong.com")
        param("expected", "https://example.com")
        replacement("https://example.com")
        position(13, 30)
      }
  }

  @Test
  fun `handles URL with query params`() {
    check
      .check(
        params(
          "Seite https://example.com/search?q=test&lang=de anzeigen",
          "Page https://example.com/search?q=test&lang=de view",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `handles URL with fragment`() {
    check
      .check(
        params(
          "Siehe https://example.com/page#section",
          "See https://example.com/page#section",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `handles www URL`() {
    check
      .check(
        params(
          "Besuchen Sie die Webseite",
          "Visit www.example.com for more",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_URL_MISSING)
        param("url", "www.example.com")
      }
  }

  @Test
  fun `handles URL inside parentheses`() {
    check
      .check(
        params(
          "Info (https://example.com) hier",
          "Info (https://example.com) here",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `strips trailing sentence punctuation from URL`() {
    check
      .check(
        params(
          "Besuchen Sie https://example.com.",
          "Visit https://example.com.",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `handles duplicate URLs correctly`() {
    check
      .check(
        params(
          "Link: https://example.com",
          "Link: https://example.com and https://example.com",
        ),
      ).assertSingleIssue {
        message(QaIssueMessage.QA_URL_MISSING)
        param("url", "https://example.com")
      }
  }

  @Test
  fun `detects multiple missing URLs`() {
    check
      .check(
        params(
          "Keine Links hier",
          "Visit https://a.com and https://b.com",
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_URL_MISSING)
          param("url", "https://a.com")
        }
        issue {
          message(QaIssueMessage.QA_URL_MISSING)
          param("url", "https://b.com")
        }
      }
  }

  @Test
  fun `handles mixed missing extra and replace`() {
    check
      .check(
        params(
          "Link: https://wrong.com und https://extra.com",
          "Link: https://correct.com",
        ),
      ).assertIssues {
        issue {
          message(QaIssueMessage.QA_URL_REPLACE)
          param("expected", "https://correct.com")
        }
        issue {
          message(QaIssueMessage.QA_URL_EXTRA)
          param("url", "https://extra.com")
        }
      }
  }

  @Test
  fun `all results have type DIFFERENT_URLS`() {
    check
      .check(
        params(
          "Link: https://wrong.com und https://extra.com",
          "Link: https://correct.com",
        ),
      ).assertAllHaveType(QaCheckType.DIFFERENT_URLS)
  }

  @Test
  fun `handles ftp URLs`() {
    check
      .check(
        params(
          "Download von ftp://files.example.com/data",
          "Download from ftp://files.example.com/data",
        ),
      ).assertNoIssues()
  }

  @Test
  fun `handles case insensitive protocols`() {
    check
      .check(
        params(
          "Link: HTTP://EXAMPLE.COM",
          "Link: HTTP://EXAMPLE.COM",
        ),
      ).assertNoIssues()
  }
}
