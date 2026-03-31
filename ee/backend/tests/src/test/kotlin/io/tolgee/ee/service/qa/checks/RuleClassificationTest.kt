package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.checks.language.isSentenceRule
import io.tolgee.ee.service.qa.checks.language.isSpellingRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.languagetool.rules.Category
import org.languagetool.rules.CategoryId
import org.languagetool.rules.Rule
import org.languagetool.rules.RuleMatch
import org.mockito.Mockito

/**
 * Unit tests for LanguageTool rule classification predicates:
 * [isSpellingRule] and [isSentenceRule].
 */
class RuleClassificationTest {
  @Test
  fun `isSentenceRule returns true for CASING category`() {
    val match = ruleMatch(categoryId = "CASING")
    assertThat(isSentenceRule(match)).isTrue()
  }

  @Test
  fun `isSentenceRule returns false for GRAMMAR category`() {
    val match = ruleMatch(categoryId = "GRAMMAR")
    assertThat(isSentenceRule(match)).isFalse()
  }

  @Test
  fun `isSentenceRule returns false for TYPOS category`() {
    val match = ruleMatch(categoryId = "TYPOS")
    assertThat(isSentenceRule(match)).isFalse()
  }

  @Test
  fun `isSentenceRule returns false for PUNCTUATION category`() {
    val match = ruleMatch(categoryId = "PUNCTUATION")
    assertThat(isSentenceRule(match)).isFalse()
  }

  @Test
  fun `isSpellingRule returns true for TYPOS category`() {
    val match = ruleMatch(categoryId = "TYPOS")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns true for SPELLER_RULE id`() {
    val match = ruleMatch(categoryId = "GRAMMAR", ruleId = "EN_SPELLER_RULE")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns true for MORFOLOGIK_RULE id`() {
    val match = ruleMatch(categoryId = "GRAMMAR", ruleId = "MORFOLOGIK_RULE_EN_US")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns true for HUNSPELL_RULE id`() {
    val match = ruleMatch(categoryId = "GRAMMAR", ruleId = "HUNSPELL_RULE")
    assertThat(isSpellingRule(match)).isTrue()
  }

  @Test
  fun `isSpellingRule returns false for GRAMMAR category with non-spelling rule id`() {
    val match = ruleMatch(categoryId = "GRAMMAR", ruleId = "HE_VERB_AGR")
    assertThat(isSpellingRule(match)).isFalse()
  }

  @Test
  fun `isSpellingRule returns false for CASING category`() {
    val match = ruleMatch(categoryId = "CASING")
    assertThat(isSpellingRule(match)).isFalse()
  }

  private fun ruleMatch(
    categoryId: String,
    ruleId: String = "TEST_RULE",
  ): RuleMatch {
    val category = Category(CategoryId(categoryId), categoryId)
    val rule =
      Mockito.mock(Rule::class.java).apply {
        Mockito.`when`(this.category).thenReturn(category)
        Mockito.`when`(this.id).thenReturn(ruleId)
      }
    return RuleMatch(rule, null, 0, 4, "Test message")
  }
}
