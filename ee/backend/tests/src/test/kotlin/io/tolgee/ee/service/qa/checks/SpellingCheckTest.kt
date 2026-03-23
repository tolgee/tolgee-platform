package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.LanguageToolService
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.checks.language.SpellingCheck
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpellingCheckTest {
  private val languageToolService = LanguageToolService()
  private val check = SpellingCheck(languageToolService)

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
  fun `detects misspelled word`() {
    val results = check.check(params("Ths is a tset."))
    assertThat(results).isNotEmpty
    results.assertAllHaveType(QaCheckType.SPELLING)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_SPELLING_ERROR }
  }

  @Test
  fun `returns correct positions`() {
    val results = check.check(params("Helo world"))
    assertThat(results).isNotEmpty
    val first = results.first()
    assertThat(first.positionStart).isEqualTo(0)
    assertThat(first.positionEnd).isEqualTo(4)
    assertThat(first.params).containsEntry("word", "Helo")
  }

  @Test
  fun `provides suggestion as replacement`() {
    val results = check.check(params("Helo world"))
    assertThat(results).isNotEmpty
    assertThat(results.first().replacement).isNotNull()
  }

  @Test
  fun `returns empty for unsupported language`() {
    check.check(params("Helo world", languageTag = "xx-unknown")).assertNoIssues()
  }

  @Test
  fun `works with regional language tags`() {
    val results = check.check(params("Helo world", languageTag = "en-US"))
    assertThat(results).isNotEmpty
  }

  @Test
  fun `all results have SPELLING type`() {
    check.check(params("Ths is a tset sentense.")).assertAllHaveType(QaCheckType.SPELLING)
  }

  @Test
  fun `does not include grammar issues`() {
    val results = check.check(params("He go to school."))
    val spelledWords = results.mapNotNull { it.params?.get("word") }
    assertThat(spelledWords).doesNotContain("go", "He", "to", "school")
  }
}
