package io.tolgee.model.enums.qa

import com.fasterxml.jackson.annotation.JsonValue
import java.util.Locale

enum class QaIssueMessage {
  QA_EMPTY_TRANSLATION,
  QA_EMPTY_PLURAL_VARIANT,
  QA_CHECK_FAILED,

  // Spaces mismatch
  QA_SPACES_LEADING_ADDED,
  QA_SPACES_LEADING_REMOVED,
  QA_SPACES_TRAILING_ADDED,
  QA_SPACES_TRAILING_REMOVED,
  QA_SPACES_DOUBLED,
  QA_SPACES_NON_BREAKING_ADDED,
  QA_SPACES_NON_BREAKING_REMOVED,

  // Punctuation mismatch
  QA_PUNCTUATION_ADD,
  QA_PUNCTUATION_REMOVE,
  QA_PUNCTUATION_REPLACE,

  // Character case
  QA_CASE_CAPITALIZE,
  QA_CASE_LOWERCASE,

  // Missing numbers
  QA_NUMBERS_MISSING,

  // Trailing/leading whitespace (base translation)
  QA_LEADING_SPACES,
  QA_TRAILING_SPACES,
  QA_LEADING_NEWLINES,
  QA_TRAILING_NEWLINES,

  // Unmatched newlines
  QA_NEWLINES_MISSING,
  QA_NEWLINES_EXTRA,
  QA_NEWLINES_TOO_MANY_SECTIONS,
  QA_NEWLINES_TOO_FEW_SECTIONS,

  // Brackets mismatch (comparison)
  QA_BRACKETS_MISSING,
  QA_BRACKETS_EXTRA,

  // Brackets unbalanced (self-check)
  QA_BRACKETS_UNCLOSED,
  QA_BRACKETS_UNMATCHED_CLOSE,

  // Special character mismatch
  QA_SPECIAL_CHAR_MISSING,
  QA_SPECIAL_CHAR_ADDED,

  // Different URLs
  QA_URL_MISSING,
  QA_URL_EXTRA,
  QA_URL_REPLACE,

  // Repeated words
  QA_REPEATED_WORD,

  // Inconsistent placeholders
  QA_PLACEHOLDERS_MISSING,
  QA_PLACEHOLDERS_EXTRA,

  // Inconsistent HTML (comparison)
  QA_HTML_TAG_MISSING,
  QA_HTML_TAG_EXTRA,

  // HTML syntax
  QA_HTML_UNCLOSED_TAG,
  QA_HTML_UNOPENED_TAG,

  // ICU syntax
  QA_ICU_SYNTAX_ERROR,

  // Spelling
  QA_SPELLING_ERROR,

  // Grammar
  QA_GRAMMAR_ERROR,

  // Key length limit
  QA_KEY_LENGTH_LIMIT_EXCEEDED,
  ;

  @JsonValue
  fun code(): String = name.lowercase(Locale.getDefault())
}
