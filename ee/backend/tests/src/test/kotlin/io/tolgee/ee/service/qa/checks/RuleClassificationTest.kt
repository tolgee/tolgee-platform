package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.LanguageToolCategory
import io.tolgee.ee.service.qa.LanguageToolMatch
import io.tolgee.ee.service.qa.LanguageToolRule
import io.tolgee.ee.service.qa.checks.language.isSentenceRule
import io.tolgee.ee.service.qa.checks.language.isSpellingRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for LanguageTool rule classification predicates:
 * [isSpellingRule] and [isSentenceRule].
 */
class RuleClassificationTest {
  @Test
  fun `isSentenceRule returns true for CASING category`() {
    val match = languageToolMatch(categoryId = "CASING")
    assertThat(isSentenceRule(match)).isTrue()
  }

  @Test
  fun `isSentenceRule returns false for GRAMMAR category`() {
    val match = languageToolMatch(categoryId = "GRAMMAR")
    assertThat(isSentenceRule(match)).isFalse()
  }

  @Test
  fun `isSentenceRule returns false for TYPOS category`() {
    val match = languageToolMatch(categoryId = "TYPOS")
    assertThat(isSentenceRule(match)).isFalse()
  }

  @Test
  fun `isSentenceRule returns false for PUNCTUATION category`() {
    val match = languageToolMatch(categoryId = "PUNCTUATION")
    assertThat(isSentenceRule(match)).isFalse()
  }

  @Test
  fun `isSpellingRule returns true for TYPOS category`() {
    val match = languageToolMatch(categoryId = "TYPOS")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns true for SPELLER_RULE id`() {
    val match = languageToolMatch(categoryId = "GRAMMAR", ruleId = "EN_SPELLER_RULE")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns true for MORFOLOGIK_RULE id`() {
    val match = languageToolMatch(categoryId = "GRAMMAR", ruleId = "MORFOLOGIK_RULE_EN_US")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns true for HUNSPELL_RULE id`() {
    val match = languageToolMatch(categoryId = "GRAMMAR", ruleId = "HUNSPELL_RULE")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns false for GRAMMAR category with non-spelling rule id`() {
    val match = languageToolMatch(categoryId = "GRAMMAR", ruleId = "HE_VERB_AGR")
    assertThat(isSpellingRule(match)).isFalse()
  }

  @Test
  fun `isSpellingRule returns false for CASING category`() {
    val match = languageToolMatch(categoryId = "CASING")
    assertThat(isSpellingRule(match)).isFalse()
  }

  private fun languageToolMatch(
    categoryId: String,
    ruleId: String = "TEST_RULE",
  ): LanguageToolMatch =
    LanguageToolMatch(
      message = "Test message",
      offset = 0,
      length = 4,
      rule =
        LanguageToolRule(
          id = ruleId,
          category = LanguageToolCategory(id = categoryId),
        ),
    )
}
