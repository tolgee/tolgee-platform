package io.tolgee.model.enums.qa

import com.fasterxml.jackson.annotation.JsonValue
import java.util.Locale

enum class QaIssueMessage {
  QA_EMPTY_TRANSLATION,
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
  ;

  @JsonValue
  fun code(): String = name.lowercase(Locale.getDefault())
}
