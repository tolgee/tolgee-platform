package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IcuSyntaxCheckTest {
  private val check = IcuSyntaxCheck()

  private fun params(text: String) =
    QaCheckParams(
      baseText = null,
      text = text,
      baseLanguageTag = null,
      languageTag = "en",
    )

  @Test
  fun `returns empty for valid simple text`() {
    val results = check.check(params("Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for valid text with placeholder`() {
    val results = check.check(params("Hello {name}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for valid typed placeholder`() {
    val results = check.check(params("You have {count, number} messages"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for valid plural`() {
    val results = check.check(params("{count, plural, one {# item} other {# items}}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for valid select`() {
    val results = check.check(params("{gender, select, male {He} female {She} other {They}}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for empty text`() {
    val results = check.check(params(""))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for escaped braces`() {
    val results = check.check(params("This is '{'not a placeholder'}'"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for plain text without ICU`() {
    val results = check.check(params("Just a regular sentence."))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects unmatched opening brace`() {
    val results = check.check(params("Hello {world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_ICU_SYNTAX_ERROR)
    assertThat(results[0].type).isEqualTo(QaCheckType.ICU_SYNTAX)
  }

  @Test
  fun `treats unmatched closing brace as valid`() {
    // ICU4J treats unmatched closing braces as literal text, not a syntax error
    val results = check.check(params("Hello world}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects select missing other keyword`() {
    val results = check.check(params("{gender, select, male {He} female {She}}"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_ICU_SYNTAX_ERROR)
  }

  @Test
  fun `detects invalid plural syntax`() {
    val results = check.check(params("{count, plural, }"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_ICU_SYNTAX_ERROR)
  }

  @Test
  fun `returns correct position span`() {
    val text = "Hello {world"
    val results = check.check(params(text))
    assertThat(results).hasSize(1)
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(text.length)
  }

  @Test
  fun `returns null replacement`() {
    val results = check.check(params("Hello {world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].replacement).isNull()
  }
}
