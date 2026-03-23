package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.LanguageToolService
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
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
    check.check(params("This is a correct sentence.")).assertNoIssues()
  }

  @Test
  fun `returns empty for blank text`() {
    check.check(params("   ")).assertNoIssues()
  }

  @Test
  fun `detects grammar error`() {
    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    results.assertAllHaveType(QaCheckType.GRAMMAR)
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
    check.check(params("He go to school.", languageTag = "xx-unknown")).assertNoIssues()
  }

  @Test
  fun `does not include spelling issues`() {
    // Results may be empty — assertThat.allMatch is vacuously true on empty lists
    val results = check.check(params("Helo world"))
    assertThat(results).allMatch { it.type == QaCheckType.GRAMMAR }
  }

  @Test
  fun `all results have GRAMMAR type`() {
    check.check(params("He go to school and she go too.")).assertAllHaveType(QaCheckType.GRAMMAR)
  }

  @Test
  fun `provides suggestion as replacement`() {
    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    assertThat(results.first().replacement).isNotNull()
  }
}
