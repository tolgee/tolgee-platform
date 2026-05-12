package io.tolgee.model.enums.qa

enum class QaCheckType(
  val defaultSeverity: QaCheckSeverity,
) {
  // TEXT category — checks about the quality of the translated text.
  // SPELLING and GRAMMAR are intentionally kept as the last two entries of this group.
  EMPTY_TRANSLATION(QaCheckSeverity.WARNING),
  MISSING_PLURAL_CATEGORIES(QaCheckSeverity.WARNING),
  CHARACTER_CASE_MISMATCH(QaCheckSeverity.WARNING),
  REPEATED_WORDS(QaCheckSeverity.WARNING),
  PUNCTUATION_MISMATCH(QaCheckSeverity.WARNING),
  TRIM_CHECK(QaCheckSeverity.WARNING),
  SPACES_MISMATCH(QaCheckSeverity.WARNING),
  UNMATCHED_NEWLINES(QaCheckSeverity.WARNING),
  MISSING_NUMBERS(QaCheckSeverity.WARNING),
  SPECIAL_CHARACTER_MISMATCH(QaCheckSeverity.WARNING),
  BRACKETS_MISMATCH(QaCheckSeverity.WARNING),
  BRACKETS_UNBALANCED(QaCheckSeverity.WARNING),
  SPELLING(QaCheckSeverity.OFF),
  GRAMMAR(QaCheckSeverity.OFF),

  // TECHNICAL category — checks for technical correctness.
  KEY_LENGTH_LIMIT(QaCheckSeverity.WARNING),
  DIFFERENT_URLS(QaCheckSeverity.WARNING),
  INCONSISTENT_PLACEHOLDERS(QaCheckSeverity.WARNING),
  INCONSISTENT_HTML(QaCheckSeverity.WARNING),
  HTML_SYNTAX(QaCheckSeverity.WARNING),
  ICU_SYNTAX(QaCheckSeverity.WARNING),
  ;

  companion object {
    val CATEGORIES: Map<QaCheckCategory, List<QaCheckType>> =
      linkedMapOf(
        QaCheckCategory.TEXT to
          listOf(
            EMPTY_TRANSLATION,
            MISSING_PLURAL_CATEGORIES,
            CHARACTER_CASE_MISMATCH,
            REPEATED_WORDS,
            PUNCTUATION_MISMATCH,
            TRIM_CHECK,
            SPACES_MISMATCH,
            UNMATCHED_NEWLINES,
            MISSING_NUMBERS,
            SPECIAL_CHARACTER_MISMATCH,
            BRACKETS_MISMATCH,
            BRACKETS_UNBALANCED,
            SPELLING,
            GRAMMAR,
          ),
        QaCheckCategory.TECHNICAL to
          listOf(
            KEY_LENGTH_LIMIT,
            DIFFERENT_URLS,
            INCONSISTENT_PLACEHOLDERS,
            INCONSISTENT_HTML,
            HTML_SYNTAX,
            ICU_SYNTAX,
          ),
      )
  }
}
