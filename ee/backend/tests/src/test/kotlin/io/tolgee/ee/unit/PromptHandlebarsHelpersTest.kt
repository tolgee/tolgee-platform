package io.tolgee.ee.unit

import com.github.jknack.handlebars.Handlebars
import io.tolgee.ee.service.prompt.PromptHandlebarsHelpers
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PromptHandlebarsHelpersTest {
  private val handlebars = PromptHandlebarsHelpers.register(Handlebars())

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
  fun `leaves an already escaped value untouched`() {
    val escaped = PromptHandlebarsHelpers.escapeJson("say \"hi\"")
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
}
