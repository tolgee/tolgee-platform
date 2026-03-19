package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BracketsBalanceCheckTest {
  private val check = BracketsBalanceCheck()

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
  fun `returns empty when text is blank`() {
    val results = check.check(params("  "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `runs even when base is null`() {
    val results = check.check(params("Hello (world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_BRACKETS_UNCLOSED)
  }

  @Test
  fun `returns empty when brackets are balanced`() {
    val results = check.check(params("Hello (world)"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty when no brackets`() {
    val results = check.check(params("Hello world"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects unclosed opening bracket`() {
    val results = check.check(params("Hello (world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_BRACKETS_UNCLOSED)
    assertThat(results[0].params).containsEntry("bracket", "(")
    // Suggests adding closing bracket at end of text
    assertThat(results[0].replacement).isEqualTo(")")
    assertThat(results[0].positionStart).isEqualTo(12) // end of "Hello (world"
    assertThat(results[0].positionEnd).isEqualTo(12)
  }

  @Test
  fun `detects unmatched closing bracket`() {
    val results = check.check(params("Hello world)"))
    assertThat(results).hasSize(1)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_BRACKETS_UNMATCHED_CLOSE)
    assertThat(results[0].params).containsEntry("bracket", ")")
    // Suggests removing the unmatched closing bracket
    assertThat(results[0].replacement).isEqualTo("")
    assertThat(results[0].positionStart).isEqualTo(11)
    assertThat(results[0].positionEnd).isEqualTo(12)
  }

  @Test
  fun `detects mismatched bracket types`() {
    val results = check.check(params("Hello (world]"))
    assertThat(results).hasSize(2) // ] is unmatched, ( is unclosed
    val closingIssue = results.find { it.params?.get("bracket") == "]" }!!
    assertThat(closingIssue.replacement).isEqualTo("")
    assertThat(closingIssue.positionStart).isEqualTo(12)

    val openingIssue = results.find { it.params?.get("bracket") == "(" }!!
    assertThat(openingIssue.replacement).isEqualTo(")")
  }

  @Test
  fun `handles nested brackets`() {
    val results = check.check(params("Hello ((world))"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles multiple bracket types`() {
    val results = check.check(params("Hello (world) [test]"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `handles complex nesting`() {
    val results = check.check(params("Hello ([world])"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects multiple unclosed brackets`() {
    val results = check.check(params("Hello (world [test"))
    assertThat(results).hasSize(2) // ( and [ both unclosed
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_BRACKETS_UNCLOSED }
  }

  @Test
  fun `handles curly braces`() {
    val results = check.check(params("Hello {world}"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects unclosed curly brace`() {
    val results = check.check(params("Hello {world"))
    assertThat(results).hasSize(1)
    assertThat(results[0].replacement).isEqualTo("}")
  }

  @Test
  fun `all results have BRACKETS_UNBALANCED type`() {
    val results = check.check(params("Hello (world] {test"))
    assertThat(results).allMatch { it.type == QaCheckType.BRACKETS_UNBALANCED }
  }
}
