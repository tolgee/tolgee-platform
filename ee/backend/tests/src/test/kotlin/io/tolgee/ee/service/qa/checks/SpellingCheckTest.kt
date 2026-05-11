package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.LanguageToolCategory
import io.tolgee.ee.service.qa.LanguageToolMatch
import io.tolgee.ee.service.qa.LanguageToolReplacement
import io.tolgee.ee.service.qa.LanguageToolRule
import io.tolgee.ee.service.qa.LanguageToolService
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaGlossaryTerm
import io.tolgee.ee.service.qa.assertAllHaveType
import io.tolgee.ee.service.qa.assertNoIssues
import io.tolgee.ee.service.qa.checks.language.SpellingCheck
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class SpellingCheckTest {
  private val languageToolService = mock(LanguageToolService::class.java)
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

  private fun spellingMatch(
    word: String,
    offset: Int,
    replacement: String? = null,
  ) = LanguageToolMatch(
    message = "Possible spelling mistake: \"$word\"",
    offset = offset,
    length = word.length,
    replacements = if (replacement != null) listOf(LanguageToolReplacement(replacement)) else emptyList(),
    rule =
      LanguageToolRule(
        id = "MORFOLOGIK_RULE_EN_US",
        category = LanguageToolCategory(id = "TYPOS"),
      ),
  )

  @Test
  fun `returns empty for blank text`() {
    check.check(params("   ")).assertNoIssues()
  }

  @Test
  fun `detects misspelled word`() {
    `when`(languageToolService.check(eq("Helo world"), any())).thenReturn(
      listOf(spellingMatch("Helo", offset = 0, replacement = "Hello")),
    )

    val results = check.check(params("Helo world"))
    assertThat(results).isNotEmpty
    results.assertAllHaveType(QaCheckType.SPELLING)
    assertThat(results).allMatch { it.message == QaIssueMessage.QA_SPELLING_ERROR }
  }

  @Test
  fun `returns correct positions`() {
    `when`(languageToolService.check(eq("Helo world"), any())).thenReturn(
      listOf(spellingMatch("Helo", offset = 0, replacement = "Hello")),
    )

    val results = check.check(params("Helo world"))
    assertThat(results).isNotEmpty
    val first = results.first()
    assertThat(first.positionStart).isEqualTo(0)
    assertThat(first.positionEnd).isEqualTo(4)
    assertThat(first.params).containsEntry("word", "Helo")
  }

  @Test
  fun `provides suggestion as replacement`() {
    `when`(languageToolService.check(eq("Helo world"), any())).thenReturn(
      listOf(spellingMatch("Helo", offset = 0, replacement = "Hello")),
    )

    val results = check.check(params("Helo world"))
    assertThat(results).isNotEmpty
    assertThat(results.first().replacement).isEqualTo("Hello")
  }

  @Test
  fun `returns empty when language tool returns no matches`() {
    `when`(languageToolService.check(any(), any())).thenReturn(emptyList())

    check.check(params("This is correct.")).assertNoIssues()
  }

  @Test
  fun `filters spelling issue overlapping glossary term`() {
    `when`(languageToolService.check(eq("Kubernetes is great"), any())).thenReturn(
      listOf(spellingMatch("Kubernetes", offset = 0, replacement = "Cube nets")),
    )

    val p =
      QaCheckParams(
        baseText = null,
        text = "Kubernetes is great",
        baseLanguageTag = null,
        languageTag = "en",
        glossaryTerms = listOf(QaGlossaryTerm(start = 0, end = 10, termText = "Kubernetes")),
      )

    check.check(p).assertNoIssues()
  }

  @Test
  fun `keeps spelling issue not overlapping glossary term`() {
    `when`(languageToolService.check(eq("Kubernetes is gret"), any())).thenReturn(
      listOf(
        spellingMatch("Kubernetes", offset = 0, replacement = "Cube nets"),
        spellingMatch("gret", offset = 14, replacement = "great"),
      ),
    )

    val p =
      QaCheckParams(
        baseText = null,
        text = "Kubernetes is gret",
        baseLanguageTag = null,
        languageTag = "en",
        glossaryTerms = listOf(QaGlossaryTerm(start = 0, end = 10, termText = "Kubernetes")),
      )

    val results = check.check(p)
    assertThat(results).hasSize(1)
    assertThat(results.first().positionStart).isEqualTo(14)
  }

  @Test
  fun `filters spelling issue with partial glossary overlap`() {
    `when`(languageToolService.check(eq("MyKubernetes cluster"), any())).thenReturn(
      listOf(spellingMatch("MyKubernetes", offset = 0, replacement = "My Kubernetes")),
    )

    val p =
      QaCheckParams(
        baseText = null,
        text = "MyKubernetes cluster",
        baseLanguageTag = null,
        languageTag = "en",
        glossaryTerms = listOf(QaGlossaryTerm(start = 2, end = 12, termText = "Kubernetes")),
      )

    check.check(p).assertNoIssues()
  }

  @Test
  fun `null glossary terms preserves existing behavior`() {
    `when`(languageToolService.check(eq("Helo world"), any())).thenReturn(
      listOf(spellingMatch("Helo", offset = 0, replacement = "Hello")),
    )

    val p =
      QaCheckParams(
        baseText = null,
        text = "Helo world",
        baseLanguageTag = null,
        languageTag = "en",
        glossaryTerms = null,
      )

    val results = check.check(p)
    assertThat(results).hasSize(1)
  }

  @Test
  fun `filters out non-spelling rules`() {
    val grammarMatch =
      LanguageToolMatch(
        message = "Grammar error",
        offset = 0,
        length = 2,
        rule =
          LanguageToolRule(
            id = "HE_VERB_AGR",
            category = LanguageToolCategory(id = "GRAMMAR"),
          ),
      )
    `when`(languageToolService.check(any(), any())).thenReturn(listOf(grammarMatch))

    check.check(params("He go to school.")).assertNoIssues()
  }
}
