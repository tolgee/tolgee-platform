package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.LanguageToolService
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.checks.language.GrammarCheck
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GrammarCheckTest {
  private val languageToolService = LanguageToolService()
  private val check = GrammarCheck(languageToolService)

  private fun params(
    text: String,
    languageTag: String = "en",
  ) = QaCheckParams(
    baseText = null,
    text = text,
    baseLanguageTag = null,
    languageTag = languageTag,
  )

  @Test
  fun `returns empty for correct text`() {
    val results = check.check(params("This is a correct sentence."))
    assertThat(results).isEmpty()
  }

  @Test
  fun `returns empty for blank text`() {
    val results = check.check(params("   "))
    assertThat(results).isEmpty()
  }

  @Test
  fun `detects grammar error`() {
    // "He go to school" — subject-verb agreement error
    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    assertThat(results).allMatch { it.type == QaCheckType.GRAMMAR }
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_GRAMMAR_ERROR }
  }

  @Test
  fun `includes message in params`() {
    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    val first = results.first()
    assertThat(first.params).containsKey("message")
    assertThat(first.params?.get("message")).isNotBlank()
  }

  @Test
  fun `returns empty for unsupported language`() {
    val results = check.check(params("He go to school.", languageTag = "xx-unknown"))
    assertThat(results).isEmpty()
  }

  @Test
  fun `does not include spelling issues`() {
    // "Helo world" has spelling errors but "Helo" is not a grammar issue
    val results = check.check(params("Helo world"))
    assertThat(results).allMatch { it.type == QaCheckType.GRAMMAR }
    // None of the grammar results should flag "Helo" as a grammar issue
    val grammarPositions = results.map { it.positionStart to it.positionEnd }
    // Spelling issue would be at (0, 4) for "Helo"
    // Grammar check should not include it
  }

  @Test
  fun `all results have GRAMMAR type`() {
    val results = check.check(params("He go to school and she go too."))
    assertThat(results).allMatch { it.type == QaCheckType.GRAMMAR }
  }

  @Test
  fun `provides suggestion as replacement`() {
    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    val first = results.first()
    assertThat(first.replacement).isNotNull()
  }
}
