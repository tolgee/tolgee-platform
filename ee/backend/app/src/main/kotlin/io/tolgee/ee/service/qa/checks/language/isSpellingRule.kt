package io.tolgee.ee.service.qa.checks.language

import org.languagetool.rules.RuleMatch

private val SPELLING_RULE_PATTERNS = listOf("SPELLER_RULE", "MORFOLOGIK_RULE", "HUNSPELL_RULE")

fun isSpellingRule(match: RuleMatch): Boolean {
  val categoryId =
    match.rule.category.id
      .toString()
  if (categoryId == "TYPOS") return true
  val ruleId = match.rule.id
  return SPELLING_RULE_PATTERNS.any { ruleId.contains(it) }
}
