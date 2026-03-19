package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HtmlSyntaxCheckTest {
  private val check = HtmlSyntaxCheck()

  private fun params(text: String) =
    QaCheckParams(
      baseText = null,
      text = text,
      baseLanguageTag = null,
      languageTag = "en",
    )

  @Test
  fun `returns empty when text is blank`() {
    assertThat(check.check(params("  "))).isEmpty()
  }

  @Test
  fun `returns empty when no HTML tags present`() {
    assertThat(check.check(params("Hello world"))).isEmpty()
  }

  @Test
  fun `returns empty when all tags properly paired`() {
    assertThat(check.check(params("<b>bold</b>"))).isEmpty()
  }

  @Test
  fun `returns empty for nested properly paired tags`() {
    assertThat(check.check(params("<div><b>text</b></div>"))).isEmpty()
  }

  @Test
  fun `returns empty for self-closing tags`() {
    assertThat(check.check(params("<br/>"))).isEmpty()
    assertThat(check.check(params("<br />"))).isEmpty()
  }

  @Test
  fun `returns empty for void elements without slash`() {
    assertThat(check.check(params("<br>"))).isEmpty()
    assertThat(check.check(params("<hr>"))).isEmpty()
    assertThat(check.check(params("<img>"))).isEmpty()
    assertThat(check.check(params("""<img src="photo.jpg">"""))).isEmpty()
  }

  @Test
  fun `detects unclosed tag`() {
    val results = check.check(params("<b>text"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_HTML_UNCLOSED_TAG)
    assertThat(results[0].replacement).isNull()
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(3)
    assertThat(results[0].params?.get("tag")).isEqualTo("<b>")
  }

  @Test
  fun `detects unopened closing tag`() {
    val results = check.check(params("text</b>"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_HTML_UNOPENED_TAG)
    assertThat(results[0].replacement).isEqualTo("")
    assertThat(results[0].positionStart).isEqualTo(4)
    assertThat(results[0].positionEnd).isEqualTo(8)
    assertThat(results[0].params?.get("tag")).isEqualTo("</b>")
  }

  @Test
  fun `detects multiple unclosed tags`() {
    val results = check.check(params("<b><i>text"))
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_HTML_UNCLOSED_TAG }
  }

  @Test
  fun `detects multiple unopened tags`() {
    val results = check.check(params("text</i></b>"))
    assertThat(results).hasSize(2)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_HTML_UNOPENED_TAG }
  }

  @Test
  fun `handles misnested but paired tags leniently`() {
    // <b><i>text</b></i> — tags are misnested but each has a matching pair
    val results = check.check(params("<b><i>text</b></i>"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles mixed valid and invalid`() {
    val results = check.check(params("<b>bold</b><i>unclosed"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_HTML_UNCLOSED_TAG)
    assertThat(results[0].params?.get("tag")).isEqualTo("<i>")
  }

  @Test
  fun `does not require base text`() {
    val results = check.check(params("<b>text"))
    assertThat(results).hasSize(1)
  }

  @Test
  fun `handles custom tag names`() {
    val results = check.check(params("<myComponent>text"))
    assertThat(results).hasSize(1)
    assertThat(results[0].params?.get("tag")).isEqualTo("<myComponent>")
  }

  @Test
  fun `void element with closing tag - closing is orphaned`() {
    // <br> is void, so </br> has no matching open tag
    val results = check.check(params("<br></br>"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_HTML_UNOPENED_TAG)
    assertThat(results[0].params?.get("tag")).isEqualTo("</br>")
  }

  @Test
  fun `all types are HTML_SYNTAX`() {
    val results = check.check(params("<b>text</i>"))
    assertThat(results).allMatch { it.type == QaCheckType.HTML_SYNTAX }
  }

  @Test
  fun `position of unclosed tag points to opening tag`() {
    val results = check.check(params("Hello <b>world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].positionStart).isEqualTo(6)
    assertThat(results[0].positionEnd).isEqualTo(9)
  }

  @Test
  fun `position of unopened tag points to closing tag`() {
    val results = check.check(params("Hello world</b>"))
    assertThat(results).hasSize(1)
    assertThat(results[0].positionStart).isEqualTo(11)
    assertThat(results[0].positionEnd).isEqualTo(15)
  }
}
