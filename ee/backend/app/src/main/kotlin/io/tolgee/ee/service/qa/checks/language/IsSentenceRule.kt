package io.tolgee.ee.service.qa.checks.language

import io.tolgee.ee.service.qa.LanguageToolMatch

/**
 * Identifies LanguageTool rules that assume sentence context — e.g.,
 * "sentence should start with an uppercase letter" (UPPERCASE_SENTENCE_START).
 *
 * These rules produce false positives on short, non-sentence translations
 * like button labels ("save", "click here", "add new item").
 */
fun isSentenceRule(match: LanguageToolMatch): Boolean {
  val categoryId = match.rule.category.id
  return categoryId == "CASING"
}
