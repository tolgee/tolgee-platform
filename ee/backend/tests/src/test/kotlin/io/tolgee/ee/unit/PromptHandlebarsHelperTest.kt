package io.tolgee.ee.unit

import com.github.jknack.handlebars.Handlebars
import io.tolgee.ee.service.prompt.PromptHandlebarsHelper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PromptHandlebarsHelperTest {
  private val handlebars = PromptHandlebarsHelper.register(Handlebars())

  private fun render(
    template: String,
    model: Map<String, Any?>,
  ): String = handlebars.compileInline(template).apply(model)

  @Test
  fun `escapes quotes, backslashes, newlines and tabs`() {
    render("{{escapeJson v}}", mapOf("v" to "a\"b\\c\nd\te"))
      .assert
      .isEqualTo("""a\"b\\c\nd\te""")
  }

  @Test
  fun `renders empty string for missing value`() {
    render("{{escapeJson missing}}", emptyMap())
      .assert
      .isEqualTo("")
  }

  @Test
  fun `renders empty string for null value`() {
    render("{{escapeJson v}}", mapOf("v" to null))
      .assert
      .isEqualTo("")
  }

  @Test
  fun `unwraps SafeString input`() {
    render("{{escapeJson v}}", mapOf("v" to Handlebars.SafeString("say \"hi\"")))
      .assert
      .isEqualTo("""say \"hi\"""")
  }

  @Test
  fun `does not html-escape output`() {
    render("{{escapeJson v}}", mapOf("v" to "<a & \"b\">"))
      .assert
      .isEqualTo("""<a & \"b\">""")
  }

  @Test
  fun `stringifies non-string values`() {
    render("{{escapeJson v}}", mapOf("v" to 42))
      .assert
      .isEqualTo("42")
  }

  @Test
  fun `stringifies boolean values`() {
    render("{{escapeJson v}}", mapOf("v" to true))
      .assert
      .isEqualTo("true")
  }

  @Test
  fun `leaves an already escaped value untouched`() {
    val escaped = PromptHandlebarsHelper.escapeJson("say \"hi\"")
    render("{{escapeJson v}}", mapOf("v" to escaped))
      .assert
      .isEqualTo("""say \"hi\"""")
  }

  @Test
  fun `is idempotent when applied twice`() {
    render("{{escapeJson (escapeJson v)}}", mapOf("v" to "say \"hi\""))
      .assert
      .isEqualTo("""say \"hi\"""")
  }

  @Test
  fun `renders empty for the whole context`() {
    render("{{escapeJson}}", mapOf("v" to "value"))
      .assert
      .isEqualTo("")
  }

  @Test
  fun `renders empty for an object value`() {
    render("{{escapeJson v}}", mapOf("v" to mapOf("nested" to "value")))
      .assert
      .isEqualTo("")
  }

  @Test
  fun `toRenderable keeps an escaped value as the same instance`() {
    val escaped = PromptHandlebarsHelper.escapeJson("say \"hi\"")
    PromptHandlebarsHelper
      .toRenderable(escaped)
      .assert
      .isSameAs(escaped)
  }

  @Test
  fun `toRenderable marks a plain string as safe so it is not html-escaped`() {
    PromptHandlebarsHelper
      .toRenderable("a & \"b\"")
      .assert
      .isInstanceOf(Handlebars.SafeString::class.java)
  }
}
