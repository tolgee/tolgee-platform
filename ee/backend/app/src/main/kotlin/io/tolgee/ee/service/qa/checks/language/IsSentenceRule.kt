package io.tolgee.ee.service.qa.checks.language

import io.tolgee.ee.service.qa.LanguageToolMatch

/**
 * Identifies LanguageTool rules that assume the text is a complete, standalone
 * sentence — e.g., "sentence should start with an uppercase letter" or "sentence
 * should end with a punctuation mark".
 *
 * These rules produce false positives on short, non-sentence translations like
 * button labels ("save", "click here", "add new item").
 */
private val PARAGRAPH_END_RULE_IDS = setOf("PUNCTUATION_PARAGRAPH_END", "PUNCTUATION_PARAGRAPH_END2")

fun isSentenceRule(match: LanguageToolMatch): Boolean {
  if (match.rule.category.id == "CASING") return true
  if (match.rule.id in PARAGRAPH_END_RULE_IDS) return true
  return false
}
