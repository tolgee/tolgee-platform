package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InconsistentHtmlCheckTest {
  private val check = InconsistentHtmlCheck()

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
    assertThat(check.check(params("<b>Hello</b>"))).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    assertThat(check.check(params("<b>Hallo</b>", "  "))).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    assertThat(check.check(params("  ", "<b>Hello</b>"))).isEmpty()
  }

  @Test
  fun `returns empty when tags match`() {
    assertThat(check.check(params("<b>Hallo</b>", "<b>Hello</b>"))).isEmpty()
  }

  @Test
  fun `returns empty when neither has tags`() {
    assertThat(check.check(params("Hallo", "Hello"))).isEmpty()
  }

  @Test
  fun `returns empty when attributes differ but tag names match`() {
    val results =
      check.check(
        params(
          """<a href="/de/hilfe">Hilfe</a>""",
          """<a href="/en/help">Help</a>""",
        ),
      )
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects missing tags`() {
    val results = check.check(params("Hallo", "<b>Hello</b>"))
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.type == QaCheckType.INCONSISTENT_HTML }
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_HTML_TAG_MISSING }
    assertThat(results).allMatch { it.replacement == null }
    assertThat(results).allMatch { it.positionStart == 0 && it.positionEnd == 0 }
    assertThat(results.map { it.params?.get("tag") }).containsExactlyInAnyOrder("<b>", "</b>")
  }

  @Test
  fun `detects extra tags`() {
    val results = check.check(params("<b>Hallo</b>", "Hello"))
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_HTML_TAG_EXTRA }
    assertThat(results).allMatch { it.replacement == "" }
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(3)
    assertThat(results[1].positionStart).isEqualTo(8)
    assertThat(results[1].positionEnd).isEqualTo(12)
  }

  @Test
  fun `detects mixed - different tags`() {
    val results = check.check(params("<i>Hallo</i>", "<b>Hello</b>"))
    assertThat(results).hasSize(4)
    val extra = results.filter { it.message == QaIssueMessage.QA_HTML_TAG_EXTRA }
    val missing = results.filter { it.message == QaIssueMessage.QA_HTML_TAG_MISSING }
    assertThat(extra).hasSize(2)
    assertThat(missing).hasSize(2)
    assertThat(extra.map { it.params?.get("tag") }).containsExactlyInAnyOrder("<i>", "</i>")
    assertThat(missing.map { it.params?.get("tag") }).containsExactlyInAnyOrder("<b>", "</b>")
  }

  @Test
  fun `handles self-closing tags`() {
    val results = check.check(params("Line1 Line2", "Line1<br/>Line2"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_HTML_TAG_MISSING)
    assertThat(results[0].params?.get("tag")).isEqualTo("<br/>")
  }

  @Test
  fun `handles duplicate tags`() {
    val results = check.check(params("<b>Hallo</b>", "<b><b>Hello</b></b>"))
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_HTML_TAG_MISSING }
  }

  @Test
  fun `handles void elements without slash`() {
    val results = check.check(params("text", "<br>text"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_HTML_TAG_MISSING)
    assertThat(results[0].params?.get("tag")).isEqualTo("<br>")
  }

  @Test
  fun `custom tag names`() {
    val results = check.check(params("<link>klicken</link>", "<link>click</link>"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `custom tag name mismatch`() {
    val results = check.check(params("<bold>Hallo</bold>", "<link>click</link>"))
    assertThat(results).hasSize(4)
  }

  @Test
  fun `all types are INCONSISTENT_HTML`() {
    val results = check.check(params("<i>Hallo</i>", "<b>Hello</b>"))
    assertThat(results).allMatch { it.type == QaCheckType.INCONSISTENT_HTML }
  }
}
