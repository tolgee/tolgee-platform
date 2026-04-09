package io.tolgee.ee.service.qa.checks.language

import io.tolgee.ee.service.qa.LanguageToolMatch

private val SPELLING_RULE_PATTERNS = listOf("SPELLER_RULE", "MORFOLOGIK_RULE", "HUNSPELL_RULE")

fun isSpellingRule(match: LanguageToolMatch): Boolean {
  val categoryId = match.rule.category.id
  if (categoryId == "TYPOS") return true
  val ruleId = match.rule.id
  return SPELLING_RULE_PATTERNS.any { ruleId.contains(it) }
}
