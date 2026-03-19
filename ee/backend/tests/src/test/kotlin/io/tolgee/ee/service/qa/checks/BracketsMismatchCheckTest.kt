package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BracketsMismatchCheckTest {
  private val check = BracketsMismatchCheck()

  private fun params(
    text: String,
    base: String? = null,
  ) = QaCheckParams(
    baseText = base,
    text = text,
    baseLanguageTag = "en",
    languageTag = "cs",
  )

  @Test
  fun `returns empty when base is null`() {
    val results = check.check(params("Hello (world)"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when base is blank`() {
    val results = check.check(params("Hello (world)", "  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when text is blank`() {
    val results = check.check(params("  ", "Hello (world)"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when brackets match`() {
    val results = check.check(params("Ahoj (svete)", "Hello (world)"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when no brackets in either`() {
    val results = check.check(params("Ahoj svete", "Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects missing bracket in translation`() {
    val results = check.check(params("Ahoj svete", "Hello (world)"))
    assertThat(results).hasSize(2) // missing ( and )
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_BRACKETS_MISSING }
    assertThat(results).allMatch { it.type == QaCheckType.BRACKETS_MISMATCH }
    assertThat(results).allMatch { it.replacement == null }
    assertThat(results.map { it.params?.get("bracket") }).containsExactlyInAnyOrder("(", ")")
  }

  @Test
  fun `detects extra bracket in translation`() {
    val results = check.check(params("Ahoj (svete)", "Hello world"))
    assertThat(results).hasSize(2) // extra ( and )
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_BRACKETS_EXTRA }
    // Extra brackets should have positions pointing to the bracket in the translation
    val openParen = results.find { it.params?.get("bracket") == "(" }!!
    assertThat(openParen.positionStart).isEqualTo(5) // position of ( in "Ahoj (svete)"
    assertThat(openParen.positionEnd).isEqualTo(6)
  }

  @Test
  fun `detects different bracket counts`() {
    val results = check.check(params("Ahoj (svete) (a) (b)", "Hello (world)"))
    // Translation has 3 ( and 3 ), source has 1 ( and 1 ) → 2 extra of each
    assertThat(results).hasSize(4)
  }

  @Test
  fun `handles multiple bracket types`() {
    val results = check.check(params("Ahoj [svete]", "Hello (world) [test]"))
    // Missing ( and ), matching [ and ]
    assertThat(results).hasSize(2)
    assertThat(results.map { it.params?.get("bracket") }).containsExactlyInAnyOrder("(", ")")
  }

  @Test
  fun `handles curly braces`() {
    val results = check.check(params("Ahoj svete", "Hello {world}"))
    assertThat(results).hasSize(2)
    assertThat(results.map { it.params?.get("bracket") }).containsExactlyInAnyOrder("{", "}")
  }

  @Test
  fun `all results have BRACKETS_MISMATCH type`() {
    val results = check.check(params("Ahoj svete", "Hello (world)"))
    assertThat(results).allMatch { it.type == QaCheckType.BRACKETS_MISMATCH }
  }
}
