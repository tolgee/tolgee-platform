package io.tolgee.ee.service.qa.checks.language

import org.languagetool.rules.RuleMatch

/**
 * Identifies LanguageTool rules that assume sentence context — e.g.,
 * "sentence should start with an uppercase letter" (UPPERCASE_SENTENCE_START).
 *
 * These rules produce false positives on short, non-sentence translations
 * like button labels ("save", "click here", "add new item").
 */
fun isSentenceRule(match: RuleMatch): Boolean {
  val categoryId =
    match.rule.category.id
      .toString()
  return categoryId == "CASING"
}
