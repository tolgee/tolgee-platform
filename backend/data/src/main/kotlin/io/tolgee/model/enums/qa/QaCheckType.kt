package io.tolgee.model.enums.qa

enum class QaCheckType(
  val defaultSeverity: QaCheckSeverity,
) {
  EMPTY_TRANSLATION(QaCheckSeverity.OFF),
  SPACES_MISMATCH(QaCheckSeverity.WARNING),
  UNMATCHED_NEWLINES(QaCheckSeverity.WARNING),
  CHARACTER_CASE_MISMATCH(QaCheckSeverity.WARNING),
  MISSING_NUMBERS(QaCheckSeverity.WARNING),
  PUNCTUATION_MISMATCH(QaCheckSeverity.WARNING),
  BRACKETS_MISMATCH(QaCheckSeverity.WARNING),
  SPECIAL_CHARACTER_MISMATCH(QaCheckSeverity.OFF),
  DIFFERENT_URLS(QaCheckSeverity.WARNING),
  INCONSISTENT_PLACEHOLDERS(QaCheckSeverity.WARNING),
  INCONSISTENT_HTML(QaCheckSeverity.WARNING),
  ICU_SYNTAX(QaCheckSeverity.WARNING),
  REPEATED_WORDS(QaCheckSeverity.WARNING),
  SPELLING(QaCheckSeverity.WARNING),
  UNRESOLVED_COMMENTS(QaCheckSeverity.WARNING),
}
