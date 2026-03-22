package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
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
    val results = check.check(params("Visit https://example.com"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("Visit https://example.com", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    val results = check.check(params("  ", "Visit https://example.com"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when no URLs in either text`() {
    val results = check.check(params("Hallo Welt", "Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when URLs match`() {
    val results =
      check.check(
        params(
          "Besuchen Sie https://example.com für mehr",
          "Visit https://example.com for more",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when multiple URLs match`() {
    val results =
      check.check(
        params(
          "Links: https://a.com und https://b.com",
          "Links: https://a.com and https://b.com",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects missing URL`() {
    val results =
      check.check(
        params(
          "Besuchen Sie die Webseite",
          "Visit https://example.com for more",
        ),
      )
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_URL_MISSING)
    assertThat(results[0].params).containsEntry("url", "https://example.com")
    assertThat(results[0].replacement).isNull()
    assertThat(results[0].positionStart).isNull()
    assertThat(results[0].positionEnd).isNull()
  }

  @Test
  fun `detects extra URL`() {
    val results =
      check.check(
        params(
          "Besuchen Sie https://extra.com bitte",
          "Visit the website",
        ),
      )
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_URL_EXTRA)
    assertThat(results[0].params).containsEntry("url", "https://extra.com")
    assertThat(results[0].replacement).isEqualTo("")
    assertThat(results[0].positionStart).isEqualTo(13)
    assertThat(results[0].positionEnd).isEqualTo(30)
  }

  @Test
  fun `detects replaced URL`() {
    val results =
      check.check(
        params(
          "Besuchen Sie https://wrong.com für mehr",
          "Visit https://example.com for more",
        ),
      )
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_URL_REPLACE)
    assertThat(results[0].params).containsEntry("url", "https://wrong.com")
    assertThat(results[0].params).containsEntry("expected", "https://example.com")
    assertThat(results[0].replacement).isEqualTo("https://example.com")
    assertThat(results[0].positionStart).isEqualTo(13)
    assertThat(results[0].positionEnd).isEqualTo(30)
  }

  @Test
  fun `handles URL with query params`() {
    val results =
      check.check(
        params(
          "Seite https://example.com/search?q=test&lang=de anzeigen",
          "Page https://example.com/search?q=test&lang=de view",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles URL with fragment`() {
    val results =
      check.check(
        params(
          "Siehe https://example.com/page#section",
          "See https://example.com/page#section",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles www URL`() {
    val results =
      check.check(
        params(
          "Besuchen Sie die Webseite",
          "Visit www.example.com for more",
        ),
      )
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_URL_MISSING)
    assertThat(results[0].params).containsEntry("url", "www.example.com")
  }

  @Test
  fun `handles URL inside parentheses`() {
    val results =
      check.check(
        params(
          "Info (https://example.com) hier",
          "Info (https://example.com) here",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `strips trailing sentence punctuation from URL`() {
    val results =
      check.check(
        params(
          "Besuchen Sie https://example.com.",
          "Visit https://example.com.",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles duplicate URLs correctly`() {
    val results =
      check.check(
        params(
          "Link: https://example.com",
          "Link: https://example.com and https://example.com",
        ),
      )
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_URL_MISSING)
    assertThat(results[0].params).containsEntry("url", "https://example.com")
  }

  @Test
  fun `detects multiple missing URLs`() {
    val results =
      check.check(
        params(
          "Keine Links hier",
          "Visit https://a.com and https://b.com",
        ),
      )
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_URL_MISSING }
    val urls = results.mapNotNull { it.params?.get("url") }.toSet()
    assertThat(urls).containsExactlyInAnyOrder("https://a.com", "https://b.com")
  }

  @Test
  fun `handles mixed missing extra and replace`() {
    val results =
      check.check(
        params(
          "Link: https://wrong.com und https://extra.com",
          "Link: https://correct.com",
        ),
      )
    // 1 missing (correct.com) + 2 extra (wrong.com, extra.com)
    // Pair: correct.com missing + wrong.com extra = replace
    // Remaining: extra.com = extra
    assertThat(results).hasSize(2)
    val replace = results.first { it.message == QaIssueMessage.QA_URL_REPLACE }
    assertThat(replace.params).containsEntry("expected", "https://correct.com")
    val extra = results.first { it.message == QaIssueMessage.QA_URL_EXTRA }
    assertThat(extra.params).containsEntry("url", "https://extra.com")
  }

  @Test
  fun `all results have type DIFFERENT_URLS`() {
    val results =
      check.check(
        params(
          "Link: https://wrong.com und https://extra.com",
          "Link: https://correct.com",
        ),
      )
    assertThat(results).allMatch { it.type == QaCheckType.DIFFERENT_URLS }
  }

  @Test
  fun `handles ftp URLs`() {
    val results =
      check.check(
        params(
          "Download von ftp://files.example.com/data",
          "Download from ftp://files.example.com/data",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles case insensitive protocols`() {
    val results =
      check.check(
        params(
          "Link: HTTP://EXAMPLE.COM",
          "Link: HTTP://EXAMPLE.COM",
        ),
      )
    assertThat(results).isEmpty()
  }
}
