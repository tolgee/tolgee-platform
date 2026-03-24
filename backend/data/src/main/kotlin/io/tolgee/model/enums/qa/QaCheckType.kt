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
  BRACKETS_UNBALANCED(QaCheckSeverity.WARNING),
  SPECIAL_CHARACTER_MISMATCH(QaCheckSeverity.OFF),
  DIFFERENT_URLS(QaCheckSeverity.WARNING),

  INCONSISTENT_PLACEHOLDERS(QaCheckSeverity.WARNING),

  INCONSISTENT_HTML(QaCheckSeverity.WARNING),
  HTML_SYNTAX(QaCheckSeverity.WARNING),
  ICU_SYNTAX(QaCheckSeverity.WARNING),
  REPEATED_WORDS(QaCheckSeverity.WARNING),
  SPELLING(QaCheckSeverity.WARNING),
  GRAMMAR(QaCheckSeverity.WARNING),
  KEY_LENGTH_LIMIT(QaCheckSeverity.WARNING),
  ;

  companion object {
    val CATEGORIES: Map<QaCheckCategory, List<QaCheckType>> =
      linkedMapOf(
        QaCheckCategory.COMPARISON to
          listOf(
            EMPTY_TRANSLATION,
            SPACES_MISMATCH,
            UNMATCHED_NEWLINES,
            CHARACTER_CASE_MISMATCH,
            MISSING_NUMBERS,
            SPELLING,
            GRAMMAR,
            REPEATED_WORDS,
            PUNCTUATION_MISMATCH,
            BRACKETS_MISMATCH,
            BRACKETS_UNBALANCED,
            SPECIAL_CHARACTER_MISMATCH,
            DIFFERENT_URLS,
            KEY_LENGTH_LIMIT,
          ),
        QaCheckCategory.SYNTAX to
          listOf(
            INCONSISTENT_PLACEHOLDERS,
            INCONSISTENT_HTML,
            HTML_SYNTAX,
            ICU_SYNTAX,
          ),
      )
  }
}
