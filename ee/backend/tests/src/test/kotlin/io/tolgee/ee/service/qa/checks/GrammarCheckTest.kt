package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.LanguageToolCategory
import io.tolgee.ee.service.qa.LanguageToolMatch
import io.tolgee.ee.service.qa.LanguageToolReplacement
import io.tolgee.ee.service.qa.LanguageToolRule
import io.tolgee.ee.service.qa.LanguageToolService
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.checks.language.GrammarCheck
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class GrammarCheckTest {
  private val languageToolService = mock(LanguageToolService::class.java)
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

  private fun grammarMatch(
    message: String,
    offset: Int,
    length: Int,
    replacement: String? = null,
  ) = LanguageToolMatch(
    message = message,
    offset = offset,
    length = length,
    replacements = if (replacement != null) listOf(LanguageToolReplacement(replacement)) else emptyList(),
    rule =
      LanguageToolRule(
        id = "HE_VERB_AGR",
        category = LanguageToolCategory(id = "GRAMMAR"),
      ),
  )

  @Test
  fun `returns empty for blank text`() {
    check.check(params("   ")).assertNoIssues()
  }

  @Test
  fun `detects grammar error`() {
    `when`(languageToolService.check(eq("He go to school."), any())).thenReturn(
      listOf(grammarMatch("Did you mean 'goes'?", offset = 3, length = 2, replacement = "goes")),
    )

    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    results.assertAllHaveType(QaCheckType.GRAMMAR)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_GRAMMAR_ERROR }
  }

  @Test
  fun `includes message in params`() {
    `when`(languageToolService.check(eq("He go to school."), any())).thenReturn(
      listOf(grammarMatch("Did you mean 'goes'?", offset = 3, length = 2, replacement = "goes")),
    )

    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    val first = results.first()
    assertThat(first.params).containsKey("message")
    assertThat(first.params?.get("message")).isEqualTo("Did you mean 'goes'?")
  }

  @Test
  fun `provides suggestion as replacement`() {
    `when`(languageToolService.check(eq("He go to school."), any())).thenReturn(
      listOf(grammarMatch("Did you mean 'goes'?", offset = 3, length = 2, replacement = "goes")),
    )

    val results = check.check(params("He go to school."))
    assertThat(results).isNotEmpty
    assertThat(results.first().replacement).isEqualTo("goes")
  }

  @Test
  fun `returns empty when language tool returns no matches`() {
    `when`(languageToolService.check(any(), any())).thenReturn(emptyList())

    check.check(params("This is a correct sentence.")).assertNoIssues()
  }

  @Test
  fun `filters out spelling rules`() {
    val spellingMatch =
      LanguageToolMatch(
        message = "Possible spelling mistake",
        offset = 0,
        length = 4,
        rule =
          LanguageToolRule(
            id = "MORFOLOGIK_RULE_EN_US",
            category = LanguageToolCategory(id = "TYPOS"),
          ),
      )
    `when`(languageToolService.check(any(), any())).thenReturn(listOf(spellingMatch))

    check.check(params("Helo world")).assertNoIssues()
  }

  @Test
  fun `filters out sentence rules (CASING category)`() {
    val casingMatch =
      LanguageToolMatch(
        message = "This sentence does not start with an uppercase letter.",
        offset = 0,
        length = 5,
        rule =
          LanguageToolRule(
            id = "UPPERCASE_SENTENCE_START",
            category = LanguageToolCategory(id = "CASING"),
          ),
      )
    `when`(languageToolService.check(any(), any())).thenReturn(listOf(casingMatch))

    check.check(params("click here")).assertNoIssues()
  }
}
