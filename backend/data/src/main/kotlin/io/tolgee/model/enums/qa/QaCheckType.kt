package io.tolgee.model.enums.qa

enum class QaCheckType(
  val defaultSeverity: QaCheckSeverity,
  val isSlow: Boolean,
) {
  // TEXT category — checks about the quality of the translated text.
  // SPELLING and GRAMMAR are intentionally kept as the last two entries of this group.
  EMPTY_TRANSLATION(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  MISSING_PLURAL_CATEGORIES(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  CHARACTER_CASE_MISMATCH(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  REPEATED_WORDS(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  PUNCTUATION_MISMATCH(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  TRIM_CHECK(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  SPACES_MISMATCH(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  UNMATCHED_NEWLINES(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  MISSING_NUMBERS(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  SPECIAL_CHARACTER_MISMATCH(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  BRACKETS_MISMATCH(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  BRACKETS_UNBALANCED(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  SPELLING(defaultSeverity = QaCheckSeverity.OFF, isSlow = true),
  GRAMMAR(defaultSeverity = QaCheckSeverity.OFF, isSlow = true),

  // TECHNICAL category — checks for technical correctness.
  KEY_LENGTH_LIMIT(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  DIFFERENT_URLS(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  INCONSISTENT_PLACEHOLDERS(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  INCONSISTENT_HTML(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  HTML_SYNTAX(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
  ICU_SYNTAX(defaultSeverity = QaCheckSeverity.WARNING, isSlow = false),
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
